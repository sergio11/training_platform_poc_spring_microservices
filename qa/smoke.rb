#!/usr/bin/env ruby
# Startup + connectivity smoke test for the kind/Podman deployment.
# Usage: ruby qa/smoke.rb   (or: rake k8s:smoke)
require_relative 'lib/harness'

at_exit { QA.stop_forwards }

DEPLOYMENTS = %w[
  training-rabbitmq
  training-keycloak
  training-catalog-service
  training-employee-service
  training-enrollment-service
  training-certification-service
  training-notification-service
  training-kong
  training-prometheus
  training-otel-collector
  training-grafana
  training-jaeger
  training-loki
].freeze

SERVICES = {
  'training-catalog-service' => 8081,
  'training-enrollment-service' => 8082,
  'training-certification-service' => 8083,
  'training-notification-service' => 8084,
  'training-employee-service' => 8085
}.freeze

def section(title)
  puts
  puts "== #{title} =="
end

failures = 0

puts 'Training Platform - Kubernetes smoke validation'
puts "Namespace: #{QA::NAMESPACE}"

section 'Workload readiness'
QA.wait_until(timeout: 600, interval: 5, label: 'all workloads Ready') do
  DEPLOYMENTS.all? { |name| QA.deployment_ready?(name) } && QA.statefulset_ready?('postgresql')
end

pods = QA.kubectl_json('get', 'pods', '--namespace', QA::NAMESPACE, '-o', 'json')
pods.fetch('items', []).each do |pod|
  statuses = pod.dig('status', 'containerStatuses').to_a
  restarts = statuses.sum { |cs| cs['restartCount'].to_i }
  state = statuses.map { |cs| cs.dig('state', 'waiting', 'reason') }.compact.first || 'Running'
  ready = statuses.all? { |cs| cs['ready'] } && !statuses.empty?
  printf("  %-52s ready=%-5s restarts=%-2d state=%s\n",
         pod.dig('metadata', 'name'), ready, restarts, state)
end

restarts = QA.pod_restart_total
failures += 1 unless QA.check('no container restarts after bring-up', restarts.zero?, "total restarts=#{restarts}")
bad = pods.fetch('items', []).select do |pod|
  pod.dig('status', 'containerStatuses').to_a.any? do |cs|
    %w[CrashLoopBackOff Error].include?(cs.dig('state', 'waiting', 'reason'))
  end
end
failures += 1 unless QA.check('no CrashLoopBackOff/Error pods', bad.empty?, bad.map { |p| p.dig('metadata', 'name') }.join(', '))

section 'Spring Boot Actuator health'
SERVICES.each do |service, port|
  out, err, status = QA.kubectl(
    'exec', "deployment/#{service}", '--namespace', QA::NAMESPACE, '--',
    'wget', '-qO-', "http://127.0.0.1:#{port}/actuator/health",
    allow_failure: true
  )
  ok = status.success? && out.include?('"status":"UP"')
  failures += 1 unless QA.check("#{service} health UP", ok, (err.to_s.strip.empty? ? out.to_s.strip[0, 160] : err.to_s.strip[0, 160]))
end

section 'Keycloak OIDC'
QA.port_forward('svc/keycloak', QA::KEYCLOAK_LOCAL_PORT, 8080)
code, well_known, = QA.http_json(:get, "#{QA::KEYCLOAK_URL}/realms/#{QA::REALM}/.well-known/openid-configuration")
issuer = well_known.is_a?(Hash) ? well_known['issuer'] : nil
failures += 1 unless QA.check('OIDC discovery reachable', code == 200)
failures += 1 unless QA.check("issuer is #{QA::ISSUER}", issuer == QA::ISSUER, issuer.to_s)

certs_code, certs, = QA.http_json(:get, "#{QA::KEYCLOAK_URL}/realms/#{QA::REALM}/protocol/openid-connect/certs")
expected_modulus = begin
  QA.rsa_modulus_b64url(QA.kong_rsa_public_key)
rescue QA::Failure
  nil
end
jwk_moduli = certs.is_a?(Hash) ? certs.fetch('keys', []).map { |k| k['n'] } : []
failures += 1 unless QA.check('realm signing key matches Kong JWT key',
                              certs_code == 200 && expected_modulus && jwk_moduli.include?(expected_modulus),
                              "JWKS keys=#{jwk_moduli.length}")

admin_token = nil
begin
  admin_token = QA.token('admin', 'admin')
  failures += 1 unless QA.check('admin token issued', !admin_token.to_s.empty?)
rescue QA::Failure => e
  failures += 1
  puts "  [FAIL] admin token issued - #{e.message}"
end

section 'Kong gateway'
QA.port_forward('svc/kong-proxy', QA::KONG_LOCAL_PORT, 8000)
code, = QA.http(:get, "#{QA::KONG_URL}/api/v1/courses")
failures += 1 unless QA.check('unauthenticated request rejected by Kong (401)', code == 401, "HTTP #{code}")

if admin_token
  code, body = QA.http_json(:get, "#{QA::KONG_URL}/api/v1/courses?size=1", token: admin_token)
  failures += 1 unless QA.check('authenticated request routed through Kong (200)', code == 200, "HTTP #{code} #{body.to_s[0, 160]}")
end

section 'Persistence'
databases = %w[training_catalog training_enrollment training_certification training_notification training_employee keycloak]
databases.each do |database|
  out, err, status = QA.kubectl(
    'exec', 'postgresql-0', '--namespace', QA::NAMESPACE, '--',
    'psql', '-U', 'postgres', '-d', database, '-tAc', 'select 1',
    allow_failure: true
  )
  failures += 1 unless QA.check("database #{database} reachable", status.success? && out.strip == '1', err.to_s.strip[0, 120])
end

if %w[training_catalog training_enrollment training_certification training_notification training_employee].all? do |db|
     begin
       QA.sql(db, 'select count(*) from flyway_schema_history').to_i.positive?
     rescue QA::Failure
       false
     end
   end
  failures += 1 unless QA.check('Flyway history present in all service databases', true)
else
  failures += 1 unless QA.check('Flyway history present in all service databases', false)
end

section 'RabbitMQ topology'
begin
  queues = QA.rabbitmq_queues
  names = queues.map { |q| q['name'] }
  expected = %w[
    enrollment.approved enrollment.rejected enrollment.cancelled
    certification.course.completed
    notification.enrollment.created notification.course.completed
    notification.certificate.generated
    catalog.dlq certification.dlq notification.dlq
  ]
  missing = expected - names
  failures += 1 unless QA.check('expected queues declared', missing.empty?, "missing=#{missing.join(', ')}")

  dlq_backlog = queues.select { |q| q['name'].to_s.end_with?('.dlq') }
                      .sum { |q| q['messages_ready'].to_i + q['messages_unacknowledged'].to_i }
  failures += 1 unless QA.check('no messages in dead-letter queues', dlq_backlog.zero?, "messages=#{dlq_backlog}")
rescue QA::Failure => e
  failures += 1
  puts "  [FAIL] RabbitMQ topology - #{e.message}"
end

section 'Observability'
QA.port_forward('svc/prometheus', QA::PROMETHEUS_LOCAL_PORT, 9090)
code, = QA.http(:get, "http://127.0.0.1:#{QA::PROMETHEUS_LOCAL_PORT}/-/ready")
failures += 1 unless QA.check('Prometheus ready', code == 200, "HTTP #{code}")

targets_code, targets = QA.http_json(:get, "http://127.0.0.1:#{QA::PROMETHEUS_LOCAL_PORT}/api/v1/targets?state=active")
if targets_code == 200 && targets.is_a?(Hash)
  active = targets.dig('data', 'activeTargets').to_a
  up = active.count { |t| t['health'] == 'up' }
  failures += 1 unless QA.check('scrape targets are up', up >= 5, "#{up}/#{active.length} up")
end

section 'Metrics server / HPA'
begin
  top_ok = QA.wait_until(timeout: 120, interval: 5, label: 'kubectl top nodes') do
    _out, _err, top_status = QA.kubectl('top', 'nodes', allow_failure: true)
    top_status.success?
  end
  failures += 1 unless QA.check('kubectl top nodes returns metrics', top_ok)
rescue QA::Failure => e
  failures += 1
  puts "  [FAIL] kubectl top nodes returns metrics - #{e.message}"
end

begin
  metrics_ready = QA.wait_until(timeout: 120, interval: 5, label: 'HPA CPU metrics') do
    hpas = QA.kubectl_json('get', 'hpa', '--namespace', QA::NAMESPACE, '-o', 'json')
    hpas.fetch('items', []).any? && hpas.fetch('items', []).all? do |hpa|
      hpa.dig('status', 'currentMetrics').to_a.any? do |metric|
        metric.dig('resource', 'name') == 'cpu' && metric.dig('resource', 'current', 'averageUtilization')
      end
    end
  end
  failures += 1 unless QA.check('all HPAs report CPU utilization', metrics_ready)
rescue QA::Failure => e
  failures += 1
  puts "  [FAIL] all HPAs report CPU utilization - #{e.message}"
end

section 'Distributed tracing and logs'
QA.port_forward('svc/jaeger', QA::JAEGER_LOCAL_PORT, 16686)
QA.port_forward('svc/loki', QA::LOKI_LOCAL_PORT, 3100)

begin
  services_ok = QA.wait_until(timeout: 90, interval: 3, label: 'Jaeger services') do
    code, body = QA.http_json(:get, "#{QA::JAEGER_URL}/api/services")
    code == 200 && body.is_a?(Hash) && body.fetch('data', []).include?('training-catalog-service')
  end
  failures += 1 unless QA.check('Jaeger receives catalog-service traces', services_ok)
rescue QA::Failure => e
  failures += 1
  puts "  [FAIL] Jaeger receives catalog-service traces - #{e.message}"
end

begin
  logs_ok = QA.wait_until(timeout: 90, interval: 3, label: 'Loki logs') do
    query = URI.encode_www_form_component('{service_name="training-catalog-service"}')
    code, body = QA.http_json(:get, "#{QA::LOKI_URL}/loki/api/v1/query_range?query=#{query}&limit=5")
    code == 200 && body.is_a?(Hash) && body.dig('data', 'result').to_a.any?
  end
  failures += 1 unless QA.check('Loki receives catalog-service logs', logs_ok)
rescue QA::Failure => e
  failures += 1
  puts "  [FAIL] Loki receives catalog-service logs - #{e.message}"
end

puts
if failures.zero?
  puts 'SMOKE RESULT: PASS'
  exit 0
else
  puts "SMOKE RESULT: FAIL (#{failures} failure(s))"
  exit 1
end
