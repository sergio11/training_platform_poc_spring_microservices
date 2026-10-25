# Rakefile for Corporate Training Platform POC

CONTAINER_ENGINE = ENV['CONTAINER_ENGINE'] || 'podman'
PODMAN_DOCKER_HOST = "npipe:////./pipe/docker_engine"

def setup_podman_env
  ENV['DOCKER_HOST'] ||= PODMAN_DOCKER_HOST
  ENV['TESTCONTAINERS_RYUK_DISABLED'] = 'true'
end

# ──────────────────────────────────────────────────────────────────────────────
# Coverage report helpers
# ──────────────────────────────────────────────────────────────────────────────
JACOCO_CSV_PATHS = {
  'training-catalog-service' => 'services/training-catalog-service/build/reports/jacoco/test/jacocoTestReport.csv',
  'employee-service'         => 'services/employee-service/build/reports/jacoco/test/jacocoTestReport.csv',
  'enrollment-service'       => 'services/enrollment-service/build/reports/jacoco/test/jacocoTestReport.csv',
  'certification-service'    => 'services/certification-service/build/reports/jacoco/test/jacocoTestReport.csv',
  'notification-service'     => 'services/notification-service/build/reports/jacoco/test/jacocoTestReport.csv',
  'shared'                   => 'shared/build/reports/jacoco/test/jacocoTestReport.csv',
}.freeze

def parse_jacoco_csv(path)
  return nil unless File.exist?(path)

  totals = Hash.new(0)
  File.foreach(path).with_index do |line, idx|
    next if idx == 0  # skip header
    cols = line.chomp.split(',')
    next if cols.size < 13

    totals[:instr_missed]  += cols[3].to_i
    totals[:instr_covered] += cols[4].to_i
    totals[:branch_missed] += cols[5].to_i
    totals[:branch_covered]+= cols[6].to_i
    totals[:line_missed]   += cols[7].to_i
    totals[:line_covered]  += cols[8].to_i
    totals[:method_missed] += cols[11].to_i
    totals[:method_covered]+= cols[12].to_i
  end
  totals
end

def pct(covered, missed)
  total = covered + missed
  return 'N/A' if total == 0
  (covered.to_f / total * 100).round(1)
end

def color_pct(val)
  return val if val == 'N/A'
  if val >= 90
    "\e[32m#{val}%\e[0m"      # green
  elsif val >= 70
    "\e[33m#{val}%\e[0m"      # yellow
  else
    "\e[31m#{val}%\e[0m"      # red
  end
end

def print_coverage_table
  puts
  puts "\e[1m\e[36m╔══════════════════════════════════════════════════════════════════════════════╗\e[0m"
  puts "\e[1m\e[36m║                        📊  JACOCO COVERAGE REPORT                          ║\e[0m"
  puts "\e[1m\e[36m╚══════════════════════════════════════════════════════════════════════════════╝\e[0m"
  puts

  header = sprintf("  %-28s  %10s  %10s  %10s  %10s", "Service", "Instruction", "Branch", "Line", "Method")
  puts "\e[1m#{header}\e[0m"
  puts "  " + "─" * 74

  grand = Hash.new(0)
  any_report = false

  JACOCO_CSV_PATHS.each do |service, csv_path|
    stats = parse_jacoco_csv(csv_path)
    unless stats
      printf("  %-28s  %s\n", service, "\e[90m(no report found)\e[0m")
      next
    end
    any_report = true

    instr_pct  = pct(stats[:instr_covered],  stats[:instr_missed])
    branch_pct = pct(stats[:branch_covered], stats[:branch_missed])
    line_pct   = pct(stats[:line_covered],   stats[:line_missed])
    method_pct = pct(stats[:method_covered], stats[:method_missed])

    grand[:instr_missed]   += stats[:instr_missed]
    grand[:instr_covered]  += stats[:instr_covered]
    grand[:branch_missed]  += stats[:branch_missed]
    grand[:branch_covered] += stats[:branch_covered]
    grand[:line_missed]    += stats[:line_missed]
    grand[:line_covered]   += stats[:line_covered]
    grand[:method_missed]  += stats[:method_missed]
    grand[:method_covered] += stats[:method_covered]

    printf("  %-28s  %10s  %10s  %10s  %10s\n",
      service,
      color_pct(instr_pct).ljust(10 + 9),
      color_pct(branch_pct).ljust(10 + 9),
      color_pct(line_pct).ljust(10 + 9),
      color_pct(method_pct)
    )
  end

  if any_report
    puts "  " + "─" * 74
    g_instr  = pct(grand[:instr_covered],  grand[:instr_missed])
    g_branch = pct(grand[:branch_covered], grand[:branch_missed])
    g_line   = pct(grand[:line_covered],   grand[:line_missed])
    g_method = pct(grand[:method_covered], grand[:method_missed])

    printf("\e[1m  %-28s  %10s  %10s  %10s  %10s\e[0m\n",
      "TOTAL",
      color_pct(g_instr).ljust(10 + 9),
      color_pct(g_branch).ljust(10 + 9),
      color_pct(g_line).ljust(10 + 9),
      color_pct(g_method)
    )
  end

  puts
end

# ──────────────────────────────────────────────────────────────────────────────
# Tasks
# ──────────────────────────────────────────────────────────────────────────────
namespace :infra do
  desc 'Start all infrastructure services'
  task :start do
    sh "cd infra && #{CONTAINER_ENGINE}-compose up -d"
  end

  desc 'Stop all infrastructure services'
  task :stop do
    sh "cd infra && #{CONTAINER_ENGINE}-compose down"
  end
end

namespace :test do
  desc 'Run all tests (unit + integration + e2e) with coverage'
  task :run do
    setup_podman_env
    cmd = Rake::Win32.windows? ? 'gradlew.bat' : './gradlew'
    begin
      sh "#{cmd} clean test jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --no-build-cache --no-daemon 2>&1"
    ensure
      print_coverage_table
    end
  end
end

namespace :k8s do
  # ── Local QA cloud (kind + Podman provider) ────────────────────────────────
  KIND_CLUSTER = 'poc-qa'
  KIND_CONFIG  = 'infra/kind/kind-config.yaml'
  K8S_NS       = 'training-platform'

  # kind's podman provider requires a rootful connection from Windows
  # (rootless connections fail kind's systemd Delegate=yes check).
  # Scoped to k8s tasks only via CONTAINER_CONNECTION; the default connection is untouched.
  PODMAN_CONNECTION = ENV['PODMAN_CONNECTION'] || 'podman-machine-default-root'

  def use_qa_podman_connection(conn = PODMAN_CONNECTION)
    ENV['CONTAINER_CONNECTION'] = conn
  end

  APP_IMAGES = {
    'services/training-catalog-service' => 'training-catalog-service:1.0.0',
    'services/enrollment-service'       => 'training-enrollment-service:1.0.0',
    'services/certification-service'    => 'training-certification-service:1.0.0',
    'services/notification-service'     => 'training-notification-service:1.0.0',
    'services/employee-service'         => 'training-employee-service:1.0.0',
  }.freeze

  K8S_DEPLOYMENTS = %w[
    training-rabbitmq training-keycloak
    training-catalog-service training-employee-service training-enrollment-service
    training-certification-service training-notification-service
    training-kong training-prometheus training-otel-collector
    training-grafana training-jaeger training-loki
  ].freeze

  def k8s_null
    Rake::Win32.windows? ? 'NUL' : '/dev/null'
  end

  namespace :cluster do
    desc "Create kind cluster '#{KIND_CLUSTER}' (Podman provider). Deletes it first if it exists."
    task :create do
      use_qa_podman_connection
      existing = `kind get clusters 2>#{k8s_null}`.split
      if existing.include?(KIND_CLUSTER)
        puts "Cluster #{KIND_CLUSTER} already exists, deleting it first..."
        sh "kind delete cluster --name #{KIND_CLUSTER}"
      end
      sh "kind create cluster --name #{KIND_CLUSTER} --config #{KIND_CONFIG} --retain"
      sh 'kubectl cluster-info'
      sh 'kubectl wait --for=condition=Ready nodes --all --timeout=180s'
    end

    desc 'Delete kind cluster'
    task :delete do
      use_qa_podman_connection
      sh "kind delete cluster --name #{KIND_CLUSTER}"
    end

    desc 'Show kind node container + k8s nodes'
    task :status do
      use_qa_podman_connection
      sh "podman ps -a --filter name=#{KIND_CLUSTER} --format \"table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}\""
      sh 'kubectl get nodes -o wide'
    end
  end

  # QA fast-path: bootJars are built locally (warm Gradle cache) and packaged
  # into minimal runtime images. Same artifact as the multi-stage Dockerfiles,
  # without re-running Gradle inside Podman for every service.
  QA_SERVICES = [
    { :dir => 'services/training-catalog-service', :jar => 'training-catalog-service-1.0.0.jar', :port => 8081, :image => 'training-catalog-service:1.0.0', :task => ':services:training-catalog-service:bootJar' },
    { :dir => 'services/enrollment-service',       :jar => 'enrollment-service-1.0.0.jar',       :port => 8082, :image => 'training-enrollment-service:1.0.0', :task => ':services:enrollment-service:bootJar' },
    { :dir => 'services/certification-service',    :jar => 'certification-service-1.0.0.jar',    :port => 8083, :image => 'training-certification-service:1.0.0', :task => ':services:certification-service:bootJar' },
    { :dir => 'services/notification-service',     :jar => 'notification-service-1.0.0.jar',     :port => 8084, :image => 'training-notification-service:1.0.0', :task => ':services:notification-service:bootJar' },
    { :dir => 'services/employee-service',         :jar => 'employee-service-1.0.0.jar',         :port => 8085, :image => 'training-employee-service:1.0.0', :task => ':services:employee-service:bootJar' },
  ].freeze

  def qa_dockerfile(port)
    <<~DOCKER
      FROM eclipse-temurin:21-jre-alpine
      RUN addgroup -S appgroup && adduser -S appuser -G appgroup
      WORKDIR /app
      COPY app.jar /app/app.jar
      RUN chown -R appuser:appgroup /app
      USER appuser
      EXPOSE #{port}
      HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD wget -qO- http://localhost:#{port}/actuator/health || exit 1
      ENTRYPOINT ["java", "-jar", "app.jar"]
    DOCKER
  end

  desc 'Build the 5 microservice images with Podman (local bootJars + minimal runtime image)'
  task :build do
    use_qa_podman_connection
    gradle = Rake::Win32.windows? ? '.\\gradlew.bat' : './gradlew'
    targets = QA_SERVICES.map { |s| s[:task] }.join(' ')
    sh "#{gradle} #{targets} -x test --no-daemon"
    require 'tmpdir'
    QA_SERVICES.each do |s|
      Dir.mktmpdir do |stage|
        cp "#{s[:dir]}/build/libs/#{s[:jar]}", File.join(stage, 'app.jar')
        File.write(File.join(stage, 'Dockerfile'), qa_dockerfile(s[:port]))
        puts "==> podman build -t docker.io/library/#{s[:image]} (staged #{s[:jar]})"
        sh "podman build -t docker.io/library/#{s[:image]} #{stage}"
      end
    end
  end

  INFRA_IMAGES = %w[
    docker.io/library/postgres:16-alpine
    docker.io/library/rabbitmq:3.13-management-alpine
    quay.io/keycloak/keycloak:26.0
    docker.io/library/kong:3.6
    docker.io/prom/prometheus:v2.50.0
    docker.io/grafana/grafana:10.3.1
    docker.io/jaegertracing/all-in-one:1.54
    docker.io/grafana/loki:2.9.4
    docker.io/otel/opentelemetry-collector-contrib:0.94.0
  ].freeze

  desc 'Load app + infra images into the kind node (machine-local save/cp/ctr-import; Podman provider)'
  task :load do
    use_qa_podman_connection
    machine = 'podman-machine-default'
    node = "#{KIND_CLUSTER}-control-plane"
    puts '==> pull infra images inside the machine'
    INFRA_IMAGES.each { |image| sh "podman machine ssh #{machine} sudo podman pull #{image}" }

    app_images = QA_SERVICES.map { |s| "docker.io/library/#{s[:image]}" }
    images = app_images + INFRA_IMAGES
    puts "==> save + copy + import #{images.length} images one by one (multi-image archives are unreliable with ctr)"
    images.each_with_index do |image, index|
      # /tmp lives on a small tmpfs inside the Podman machine; /var/tmp is on the ext4 disk.
      tar = "/var/tmp/kind-image-#{index}.tar"
      puts "==> [#{index + 1}/#{images.length}] #{image}"
      sh "podman machine ssh #{machine} sudo rm -f #{tar}"
      sh "podman machine ssh #{machine} sudo podman save -o #{tar} #{image}"
      sh "podman machine ssh #{machine} sudo podman cp #{tar} #{node}:#{tar}"
      sh "podman machine ssh #{machine} sudo podman exec #{node} ctr -n k8s.io images import #{tar}"
      sh "podman machine ssh #{machine} sudo rm -f #{tar}"
    end
  end

  desc 'Generate the Keycloak realm ConfigMap from infra/keycloak/realm-export.json'
  task :realm do
    sh "kubectl create configmap training-keycloak-realm --namespace #{K8S_NS} --from-file=realm-export.json=infra/keycloak/realm-export.json --dry-run=client -o yaml | kubectl apply -f -"
  end

  desc 'Deploy to Kubernetes (namespace + infra + services + observability + kong)'
  task :deploy do
    sh "kubectl apply -f infra/k8s/namespace.yml"
    Rake::Task['k8s:realm'].invoke
    sh "kubectl apply -f infra/k8s/postgresql/"
    sh "kubectl apply -f infra/k8s/rabbitmq/"
    sh "kubectl apply -f infra/k8s/keycloak/"
    sh "kubectl apply -f infra/k8s/network-policies.yml"
    # The services fetch OIDC metadata at startup, so the identity provider and
    # data stores must be Ready before the Spring Boot pods are created.
    sh "kubectl rollout status statefulset/postgresql --namespace #{K8S_NS} --timeout=300s"
    sh "kubectl rollout status deployment/training-rabbitmq --namespace #{K8S_NS} --timeout=300s"
    sh "kubectl rollout status deployment/training-keycloak --namespace #{K8S_NS} --timeout=600s"
    sh "kubectl apply -f infra/k8s/catalog-service/"
    sh "kubectl apply -f infra/k8s/employee-service/"
    sh "kubectl apply -f infra/k8s/enrollment-service/"
    sh "kubectl apply -f infra/k8s/certification-service/"
    sh "kubectl apply -f infra/k8s/notification-service/"
    sh "kubectl apply -f infra/k8s/observability/"
    sh "kubectl apply -f infra/k8s/kong/"
  end

  desc 'Wait until all workloads are rolled out and every pod is Ready'
  task :wait do
    sh "kubectl rollout status statefulset/postgresql --namespace #{K8S_NS} --timeout=300s"
    K8S_DEPLOYMENTS.each do |dep|
      sh "kubectl rollout status deployment/#{dep} --namespace #{K8S_NS} --timeout=600s"
    end
    sh "kubectl wait --for=condition=Ready pods --all --namespace #{K8S_NS} --timeout=300s"
    Rake::Task['k8s:status'].invoke
  end

  desc 'Show platform status (pods, services, pvc, hpa, recent events)'
  task :status do
    sh "kubectl get pods -o wide --namespace #{K8S_NS}"
    sh "kubectl get svc,pvc,hpa --namespace #{K8S_NS}"
    sh "kubectl get events --namespace #{K8S_NS} --sort-by=.lastTimestamp"
  end

  desc 'Run smoke validation: startup evidence + pod-to-pod connectivity (qa/smoke.rb)'
  task :smoke do
    ruby 'qa/smoke.rb'
  end

  desc 'Run the full E2E suite against the K8s platform (qa/run_all.rb)'
  task :test do
    ruby 'qa/run_all.rb'
  end

  desc 'Tail logs of a workload, e.g. rake k8s:logs[training-catalog-service]'
  task :logs, [:svc] do |_t, args|
    svc = args[:svc] || 'training-catalog-service'
    sh "kubectl logs deployment/#{svc} --namespace #{K8S_NS} --tail=200"
  end

  desc 'Expose Keycloak/Grafana/Prometheus/RabbitMQ-mgmt on localhost (Ctrl+C / Enter to stop)'
  task :port_forward do
    forwards = [
      %w[svc/keycloak 18080:8080],
      %w[svc/grafana 13000:3000],
      %w[svc/prometheus 19090:9090],
      %w[svc/rabbitmq 15672:15672],
    ]
    pids = forwards.map do |res, ports|
      puts "Forwarding #{res} -> localhost:#{ports.split(':').first}"
      Process.spawn('kubectl', 'port-forward', res, ports, '--namespace', K8S_NS,
                    :out => k8s_null, :err => k8s_null)
    end
    puts 'Press Enter to stop port-forwards...'
    $stdin.gets
  ensure
    pids&.each { |pid| Process.kill('KILL', pid) rescue nil }
  end

  desc 'Full bring-up: cluster + build + load + deploy + wait + smoke'
  task :up => ['k8s:cluster:create', 'k8s:build', 'k8s:load', 'k8s:deploy', 'k8s:wait', 'k8s:smoke']

  desc 'Undeploy from Kubernetes'
  task :undeploy do
    sh "kubectl delete namespace #{K8S_NS} --ignore-not-found"
  end
end

desc 'Show all available tasks'
task :help do
  puts <<~HELP
    Corporate Training Platform - Available Tasks
    ================================================
      rake infra:start      Start infrastructure
      rake infra:stop       Stop infrastructure
      rake test:run         Run all tests + coverage
      rake k8s:cluster:create   Create kind cluster (Podman) for local QA
      rake k8s:cluster:delete   Delete kind cluster
      rake k8s:cluster:status   Show kind node + k8s nodes
      rake k8s:build        Build the 5 microservice images (Podman)
      rake k8s:load         Load images into the kind node
      rake k8s:realm        Generate Keycloak realm ConfigMap
      rake k8s:deploy       Deploy to Kubernetes
      rake k8s:wait         Wait for rollout + all pods Ready
      rake k8s:status       Show platform status
      rake k8s:smoke        Smoke: startup + pod-to-pod connectivity
      rake k8s:test         Full E2E suite against K8s
      rake k8s:logs[SVC]    Tail logs of a workload
      rake k8s:port_forward Expose Keycloak/Grafana/Prometheus/RabbitMQ locally
      rake k8s:up           Full bring-up: cluster+build+load+deploy+wait+smoke
      rake k8s:undeploy     Undeploy from Kubernetes
  HELP
end

task :default => :help
