#!/usr/bin/env ruby
# End-to-end business flow against the kind/Podman deployment.
# All calls go through Kong (http://127.0.0.1:18000), so the gateway JWT plugin is
# exercised as well. Tokens are issued by Keycloak via port-forward.
#
# Usage: ruby qa/run_all.rb   (or: rake k8s:test)
require_relative 'lib/harness'
require 'time'

at_exit { QA.stop_forwards }

STARTED_AT = Time.now.utc
STAMP = STARTED_AT.strftime('%Y%m%d%H%M%S')
COURSE_TITLE = "POC K8s Full Flow #{STAMP}"
EMPLOYEE_NUMBER = "EMP-K8S-#{STAMP}"

def section(title)
  puts
  puts "== #{title} =="
end

def api(method, path, token: nil, body: nil)
  QA.http_json(method, "#{QA::KONG_URL}#{path}", token: token, body: body)
end

def jwt_claim(token, name)
  payload = token.to_s.split('.')[1].to_s
  padded = payload + ('=' * ((4 - payload.length % 4) % 4))
  JSON.parse(Base64.urlsafe_decode64(padded))[name]
end

def k8s_logs_since_start(deployment)
  out, = QA.kubectl(
    'logs', "deployment/#{deployment}", '--namespace', QA::NAMESPACE,
    "--since-time=#{STARTED_AT.strftime('%Y-%m-%dT%H:%M:%SZ')}",
    '--tail=2000', allow_failure: true
  )
  out
end

failures = 0

puts 'Training Platform - Kubernetes full E2E validation'
puts "Namespace: #{QA::NAMESPACE}"
puts "Run stamp: #{STAMP}"

section 'Preconditions'
QA.port_forward('svc/keycloak', QA::KEYCLOAK_LOCAL_PORT, 8080)
QA.port_forward('svc/kong-proxy', QA::KONG_LOCAL_PORT, 8000)

%w[postgresql].each do |name|
  QA.assert("statefulset/#{name} Ready", QA.statefulset_ready?(name))
end
%w[
  training-rabbitmq training-keycloak training-catalog-service training-employee-service
  training-enrollment-service training-certification-service training-notification-service
  training-kong
].each do |name|
  QA.assert("deployment/#{name} Ready", QA.deployment_ready?(name))
end

admin_token = QA.token('admin', 'admin')
instructor_token = QA.token('instructor1', 'instructor1')
employee_token = QA.token('employee1', 'employee1')
QA.check('tokens issued for admin/instructor/employee', [admin_token, instructor_token, employee_token].all? { |t| !t.to_s.empty? })

section 'Gateway authorization'
code, = api(:get, '/api/v1/courses?size=1')
failures += 1 unless QA.check('unauthenticated request rejected by Kong (401)', code == 401, "HTTP #{code}")

section 'Business flow'
code, course, = api(:post, '/api/v1/courses', token: admin_token,
                    body: { title: COURSE_TITLE, description: 'K8s E2E', instructor: 'Instructor', capacity: 5 })
failures += 1 unless QA.check('admin creates course', code == 201 && course.is_a?(Hash) && course['id'], "HTTP #{code} #{course}")
course_id = course && course['id']

code, employee, = api(:post, '/api/v1/employees', token: admin_token,
                      body: { employeeNumber: EMPLOYEE_NUMBER, fullName: "Employee #{STAMP}",
                              email: "#{EMPLOYEE_NUMBER.downcase}@poc.test", department: 'IT', position: 'Dev' })
failures += 1 unless QA.check('admin creates employee', code == 201 && employee.is_a?(Hash) && employee['id'], "HTTP #{code} #{employee}")
employee_id = employee && employee['id']

if course_id && employee_id
  code, course_get, = api(:get, "/api/v1/courses/#{course_id}", token: admin_token)
  ok = code == 200 && course_get && course_get['title'] == COURSE_TITLE && course_get['status'] == 'ACTIVE'
  failures += 1 unless QA.check('course persisted and ACTIVE', ok, "HTTP #{code} #{course_get}")

  code, enrollment, = api(:post, '/api/v1/enrollments', token: admin_token,
                          body: { employeeId: employee_id, courseId: course_id })
  ok = code == 201 && enrollment.is_a?(Hash) && enrollment['status'] == 'PENDING'
  failures += 1 unless QA.check('enrollment created in PENDING', ok, "HTTP #{code} #{enrollment}")
  enrollment_id = enrollment && enrollment['id']

  if enrollment_id
    code, course_after_enroll, = api(:get, "/api/v1/courses/#{course_id}", token: admin_token)
    ok = code == 200 && course_after_enroll && course_after_enroll['enrolledCount'] == 1
    failures += 1 unless QA.check('enrolledCount incremented to 1', ok, "HTTP #{code} #{course_after_enroll}")

    code, approved, = api(:patch, "/api/v1/enrollments/#{enrollment_id}/approve", token: instructor_token)
    ok = code == 200 && approved.is_a?(Hash) && approved['status'] == 'APPROVED'
    failures += 1 unless QA.check('instructor approves enrollment', ok, "HTTP #{code} #{approved}")

    code, completed, = api(:patch, "/api/v1/enrollments/#{enrollment_id}/complete", token: instructor_token)
    ok = code == 200 && completed.is_a?(Hash) && completed['status'] == 'COMPLETED'
    failures += 1 unless QA.check('instructor completes enrollment', ok, "HTTP #{code} #{completed}")

    certificate = nil
    begin
      QA.wait_until(timeout: 60, interval: 2, label: 'certificate generation') do
        code, page, = api(:get, "/api/v1/certificates?employeeId=#{employee_id}", token: admin_token)
        cert = code == 200 && page.is_a?(Hash) ? page.fetch('content', []).find { |c| c['courseId'] == course_id } : nil
        certificate = cert if cert
      end
      ok = certificate && certificate['certificateNumber'].to_s.start_with?('CERT-') &&
           certificate['enrollmentId'] == enrollment_id
      failures += 1 unless QA.check("certificate generated (#{certificate && certificate['certificateNumber']})", ok, certificate.to_s)
    rescue QA::Failure => e
      failures += 1
      puts "  [FAIL] certificate generated - #{e.message}"
    end

    expected_types = %w[ENROLLMENT_CREATED ENROLLMENT_APPROVED COURSE_COMPLETED CERTIFICATE_GENERATED]
    notifications = []
    begin
      QA.wait_until(timeout: 60, interval: 2, label: 'notifications delivered') do
        code, page, = api(:get, "/api/v1/notifications?recipientId=#{employee_id}&size=50", token: admin_token)
        notifications = code == 200 && page.is_a?(Hash) ? page.fetch('content', []) : []
        (expected_types - notifications.map { |n| n['type'] }).empty?
      end
      missing = expected_types - notifications.map { |n| n['type'] }
      failures += 1 unless QA.check('notifications ENROLLMENT_CREATED/APPROVED/COURSE_COMPLETED/CERTIFICATE_GENERATED',
                                    missing.empty?, "missing=#{missing.join(', ')}")
      not_delivered = notifications.select { |n| expected_types.include?(n['type']) && n['status'] != 'DELIVERED' }
      failures += 1 unless QA.check('all flow notifications DELIVERED', not_delivered.empty?,
                                    not_delivered.map { |n| "#{n['type']}=#{n['status']}" }.join(', '))
    rescue QA::Failure => e
      failures += 1
      puts "  [FAIL] notifications delivered - #{e.message}"
    end

    code, dup_body, = api(:post, '/api/v1/enrollments', token: admin_token,
                         body: { employeeId: employee_id, courseId: course_id })
    failures += 1 unless QA.check('duplicate enrollment rejected (400/409)', [400, 409].include?(code), "HTTP #{code} #{dup_body}")

    code, course_final, = api(:get, "/api/v1/courses/#{course_id}", token: admin_token)
    ok = code == 200 && course_final && course_final['enrolledCount'] == 1
    failures += 1 unless QA.check('enrolledCount compensated back to 1', ok, "HTTP #{code} #{course_final}")

    other_employee_id = jwt_claim(employee_token, 'employeeId').to_i + 1_000_000
    code, forbidden_body, = api(:get, "/api/v1/notifications?recipientId=#{other_employee_id}", token: employee_token)
    ok = code == 403
    failures += 1 unless QA.check('employee cannot read another employee notifications (403)', ok, "HTTP #{code} #{forbidden_body}")
  end
end

section 'Persistence / outbox verification'
if course_id && employee_id
  enrollment_status = QA.sql('training_enrollment',
                             "select status from enrollments where employee_id = #{employee_id} and course_id = #{course_id}")
  failures += 1 unless QA.check('enrollment row is COMPLETED', enrollment_status == 'COMPLETED', enrollment_status)

  enrollment_outbox = QA.sql('training_enrollment',
                             "select count(*) from outbox_events where processed = false").to_i
  failures += 1 unless QA.check('enrollment outbox fully processed', enrollment_outbox.zero?, "pending=#{enrollment_outbox}")

  catalog_processed = QA.sql('training_catalog',
                             "select count(*) from processed_events where event_type = 'EnrollmentApprovedEvent'").to_i
  failures += 1 unless QA.check('catalog consumed EnrollmentApprovedEvent', catalog_processed >= 1, "rows=#{catalog_processed}")

  cert_rows = QA.sql('training_certification',
                     "select count(*) from certificates where employee_id = #{employee_id} and course_id = #{course_id}").to_i
  failures += 1 unless QA.check('certificate row persisted', cert_rows == 1, "rows=#{cert_rows}")

  cert_outbox = QA.sql('training_certification',
                       "select count(*) from outbox_events where processed = false").to_i
  failures += 1 unless QA.check('certification outbox fully processed', cert_outbox.zero?, "pending=#{cert_outbox}")

  notification_rows = QA.sql('training_notification',
                             "select count(*) from notifications where recipient_id = #{employee_id} and status = 'DELIVERED'").to_i
  failures += 1 unless QA.check('delivered notification rows persisted', notification_rows >= 5, "rows=#{notification_rows}")

  notification_types = QA.sql('training_notification',
                              "select string_agg(distinct type, ',' order by type) from notifications where recipient_id = #{employee_id}").split(',')
  missing_db_types = %w[EMPLOYEE_CREATED ENROLLMENT_CREATED ENROLLMENT_APPROVED COURSE_COMPLETED CERTIFICATE_GENERATED] - notification_types
  failures += 1 unless QA.check('notification types persisted in database', missing_db_types.empty?,
                                "missing=#{missing_db_types.join(', ')}")

  processed_events = QA.sql('training_notification',
                            "select count(*) from processed_events where event_type in ('ENROLLMENT_CREATED','ENROLLMENT_APPROVED','COURSE_COMPLETED','CERTIFICATE_GENERATED')").to_i
  failures += 1 unless QA.check('notification idempotency records present', processed_events >= 4, "rows=#{processed_events}")

  employee_outbox = QA.sql('training_employee',
                           "select count(*) from outbox_events where processed = false").to_i
  failures += 1 unless QA.check('employee outbox fully processed', employee_outbox.zero?, "pending=#{employee_outbox}")
end

section 'RabbitMQ drain'
begin
  consumed_queues = %w[
    enrollment.approved enrollment.rejected enrollment.cancelled
    certification.course.completed
    notification.enrollment.created notification.enrollment.approved
    notification.enrollment.rejected notification.enrollment.cancelled
    notification.course.completed notification.certificate.generated
    notification.employee.created notification.employee.updated notification.employee.deactivated
    notification.notification.sent
  ]
  dlqs = %w[catalog.dlq certification.dlq notification.dlq]

  QA.wait_until(timeout: 60, interval: 2, label: 'queues drained') do
    queues = QA.rabbitmq_queues
    relevant = queues.select { |q| (consumed_queues + dlqs).include?(q['name']) }
    relevant.all? { |q| q['messages_ready'].to_i.zero? && q['messages_unacknowledged'].to_i.zero? }
  end
  queues = QA.rabbitmq_queues
  backlog = queues.select { |q| (consumed_queues + dlqs).include?(q['name']) }
                  .sum { |q| q['messages_ready'].to_i + q['messages_unacknowledged'].to_i }
  failures += 1 unless QA.check('no backlog in flow queues', backlog.zero?, "messages=#{backlog}")
  failures += 1 unless QA.check('no dead-letter messages', queues.select { |q| q['name'].end_with?('.dlq') }
                                                          .sum { |q| q['messages_ready'].to_i + q['messages_unacknowledged'].to_i }.zero?)
rescue QA::Failure => e
  failures += 1
  puts "  [FAIL] RabbitMQ drain - #{e.message}"
end

section 'Distributed tracing and logs'
QA.port_forward('svc/jaeger', QA::JAEGER_LOCAL_PORT, 16686)
QA.port_forward('svc/loki', QA::LOKI_LOCAL_PORT, 3100)

# Deployment name in Kubernetes -> spring.application.name exported over OTLP.
{ 'training-catalog-service' => 'training-catalog-service',
  'training-enrollment-service' => 'enrollment-service' }.each do |deployment, service|
  begin
    found = QA.wait_until(timeout: 60, interval: 2, label: "Jaeger traces for #{service}") do
      code, body = QA.http_json(:get, "#{QA::JAEGER_URL}/api/traces?service=#{service}&limit=1")
      code == 200 && body.is_a?(Hash) && body.fetch('data', []).any?
    end
    failures += 1 unless QA.check("Jaeger trace present for #{deployment} (svc #{service})", found)
  rescue QA::Failure => e
    failures += 1
    puts "  [FAIL] Jaeger trace present for #{deployment} (svc #{service}) - #{e.message}"
  end

  begin
    found = QA.wait_until(timeout: 60, interval: 2, label: "Loki logs for #{service}") do
      query = URI.encode_www_form_component(%({service_name="#{service}"}))
      code, body = QA.http_json(:get, "#{QA::LOKI_URL}/loki/api/v1/query_range?query=#{query}&limit=5")
      code == 200 && body.is_a?(Hash) && body.dig('data', 'result').to_a.any?
    end
    failures += 1 unless QA.check("Loki logs present for #{deployment} (svc #{service})", found)
  rescue QA::Failure => e
    failures += 1
    puts "  [FAIL] Loki logs present for #{deployment} (svc #{service}) - #{e.message}"
  end
end

section 'Stability after flow'
pods = QA.kubectl_json('get', 'pods', '--namespace', QA::NAMESPACE, '-o', 'json')
restarts = pods.fetch('items', []).sum do |pod|
  pod.dig('status', 'containerStatuses').to_a.sum { |cs| cs['restartCount'].to_i }
end
failures += 1 unless QA.check('no container restarts during validation', restarts.zero?, "total=#{restarts}")

crashing = pods.fetch('items', []).select do |pod|
  pod.dig('status', 'containerStatuses').to_a.any? do |cs|
    %w[CrashLoopBackOff Error].include?(cs.dig('state', 'waiting', 'reason'))
  end
end
failures += 1 unless QA.check('no CrashLoopBackOff/Error pods', crashing.empty?,
                              crashing.map { |p| p.dig('metadata', 'name') }.join(', '))

section 'Application logs since start'
%w[
  training-rabbitmq training-keycloak training-catalog-service training-employee-service
  training-enrollment-service training-certification-service training-notification-service
  training-kong training-prometheus training-otel-collector training-grafana
  training-jaeger training-loki
].each do |deployment|
  logs = k8s_logs_since_start(deployment)
  errors = logs.lines.select { |line| line.match?(/\bERROR\b/) || line.include?('[error]') }
  ok = errors.empty?
  failures += 1 unless QA.check("#{deployment} emitted no ERROR logs", ok, "#{errors.length} line(s)")
  errors.first(3).each { |line| puts "    #{line.strip}" }
end

puts
if failures.zero?
  puts "E2E RESULT: PASS (course=#{course_id}, employee=#{employee_id})"
  exit 0
else
  puts "E2E RESULT: FAIL (#{failures} failure(s))"
  exit 1
end
