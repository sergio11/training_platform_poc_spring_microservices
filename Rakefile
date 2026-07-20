# Rakefile for Corporate Training Platform POC

require 'open3'

# Container engine: 'podman' or 'docker'
CONTAINER_ENGINE = ENV['CONTAINER_ENGINE'] || 'podman'

# Podman DOCKER_HOST for Windows
PODMAN_DOCKER_HOST = "npipe:////./pipe/podman-machine-default"

def setup_podman_env
  ENV['DOCKER_HOST'] ||= PODMAN_DOCKER_HOST
  ENV['TESTCONTAINERS_RYUK_DISABLED'] = 'true'
  puts "  DOCKER_HOST=#{ENV['DOCKER_HOST']}"
  puts "  TESTCONTAINERS_RYUK_DISABLED=true"
end

namespace :infra do
  desc 'Start all infrastructure services'
  task :start do
    puts "Starting infrastructure services with #{CONTAINER_ENGINE}..."
    sh "cd infra && #{CONTAINER_ENGINE}-compose up -d"
    puts "Infrastructure started successfully!"
    puts ""
    puts "Services available at:"
    puts "  - PostgreSQL:  localhost:5432"
    puts "  - Keycloak:    http://localhost:8080"
    puts "  - RabbitMQ:    http://localhost:15672 (guest/guest)"
    puts "  - Kong:        http://localhost:8000"
    puts "  - Prometheus:  http://localhost:9090"
    puts "  - Jaeger:      http://localhost:16686"
    puts "  - Grafana:     http://localhost:3000 (admin/admin)"
  end

  desc 'Stop all infrastructure services'
  task :stop do
    puts "Stopping infrastructure services..."
    sh "cd infra && #{CONTAINER_ENGINE}-compose down"
    puts "Infrastructure stopped."
  end

  desc 'Restart infrastructure services'
  task :restart => [:stop, :start]

  desc 'Show infrastructure services status'
  task :status do
    sh "cd infra && #{CONTAINER_ENGINE}-compose ps"
  end

  desc 'View infrastructure logs'
  task :logs do
    sh "cd infra && #{CONTAINER_ENGINE}-compose logs -f"
  end

  desc 'Remove infrastructure volumes and data'
  task :clean do
    puts "WARNING: This will delete all data!"
    puts "Press Ctrl+C to cancel, wait 3 seconds to continue..."
    sleep 3
    sh "cd infra && #{CONTAINER_ENGINE}-compose down -v"
    puts "Volumes removed."
  end
end

namespace :build do
  desc 'Build all services'
  task :all do
    puts "Building all services..."
    sh "./gradlew clean build -x test"
    puts "All services built successfully!"
  end

  desc 'Build specific service'
  task :service, [:name] do |t, args|
    service_name = args[:name] || abort("Usage: rake build:service[service-name]")
    puts "Building #{service_name}..."
    sh "./gradlew :services:#{service_name}:build -x test"
    puts "#{service_name} built successfully!"
  end

  desc 'Build Podman images'
  task :images do
    puts "Building container images with #{CONTAINER_ENGINE}..."
    services = [
      { name: 'training-catalog-service', port: 8081 },
      { name: 'training-enrollment-service', port: 8082 },
      { name: 'training-certification-service', port: 8083 },
      { name: 'training-notification-service', port: 8084 },
      { name: 'training-config-server', port: 9999 }
    ]
    services.each do |service|
      puts "Building #{service[:name]}..."
      sh "#{CONTAINER_ENGINE} build -t #{service[:name]}:latest -f services/#{service[:name].sub('training-', '')}/Dockerfile ."
    end
    puts "All images built successfully!"
  end
end

namespace :run do
  desc 'Run specific service'
  task :service, [:name] do |t, args|
    service_name = args[:name] || abort("Usage: rake run:service[service-name]")
    puts "Running #{service_name}..."
    sh "./gradlew :services:#{service_name}:bootRun"
  end

  desc 'Run all services locally'
  task :all do
    puts "Starting all services..."
    services = [
      'config-server',
      'training-catalog-service',
      'enrollment-service',
      'certification-service',
      'notification-service'
    ]
    pids = services.map do |service|
      puts "Starting #{service}..."
      pid = Process.spawn("./gradlew :services:#{service}:bootRun")
      sleep 2
      pid
    end
    puts "All services started. Press Ctrl+C to stop."
    puts "PIDs: #{pids.join(', ')}"
    sleep
  end
end

namespace :test do
  desc 'Run all tests (unit + integration)'
  task :all do
    puts "Running all tests (unit + integration)..."
    setup_podman_env
    sh "./gradlew test"
    puts "All tests completed!"
  end

  desc 'Run unit tests only (no Testcontainers)'
  task :unit do
    puts "Running unit tests only..."
    sh "./gradlew unitTest"
    puts "Unit tests completed!"
  end

  desc 'Run integration tests only (Podman required)'
  task :integration do
    puts "Running integration tests (Testcontainers + Podman)..."
    setup_podman_env
    sh "./gradlew integrationTest"
    puts "Integration tests completed!"
  end

  desc 'Run specific service tests'
  task :service, [:name] do |t, args|
    service_name = args[:name] || abort("Usage: rake test:service[service-name]")
    puts "Running tests for #{service_name}..."
    setup_podman_env
    sh "./gradlew :services:#{service_name}:test"
    puts "Tests completed!"
  end

  desc 'Run tests with coverage report'
  task :coverage do
    puts "Running tests with coverage..."
    setup_podman_env
    sh "./gradlew test jacocoTestReport jacocoTestCoverageVerification"
    puts ""
    puts "Test reports generated at:"
    puts "  - HTML:  build/reports/jacoco/test/html/index.html"
    puts "  - XML:   build/reports/jacoco/test/jacocoTestReport.xml"
    puts "  - CSV:   build/reports/jacoco/test/jacocoTestReport.csv"
  end

  desc 'Run E2E tests (Podman required)'
  task :e2e do
    puts "Running E2E tests with Podman..."
    puts ""

    # Verify Podman is available
    begin
      sh "#{CONTAINER_ENGINE} --version"
    rescue
      abort("#{CONTAINER_ENGINE} not found. Install Podman Desktop.")
    end

    setup_podman_env

    sh "./gradlew :e2e-tests:test --info"
    puts ""
    puts "E2E tests completed!"
  end

  desc 'Run E2E tests with coverage'
  task :e2e_coverage do
    puts "Running E2E tests with coverage..."
    setup_podman_env
    sh "./gradlew :e2e-tests:test :e2e-tests:jacocoTestReport"
    puts "E2E coverage report at e2e-tests/build/reports/jacoco/"
  end

  desc 'Clean up E2E test containers'
  task :e2e_cleanup do
    puts "Cleaning up E2E test containers..."
    setup_podman_env
    sh "#{CONTAINER_ENGINE} rm -f $(#{CONTAINER_ENGINE} ps -aq --filter label=org.testcontainers 2>/dev/null) 2>/dev/null || true"
    puts "Cleanup done."
  end

  desc 'Run a specific test class'
  task :class, [:class_name] do |t, args|
    class_name = args[:class_name] || abort("Usage: rake test:class[com.example.MyTest]")
    puts "Running test class: #{class_name}..."
    setup_podman_env
    sh "./gradlew test --tests #{class_name}"
    puts "Test class completed!"
  end

  desc 'Run tests with verbose output'
  task :verbose do
    puts "Running all tests with verbose output..."
    setup_podman_env
    sh "./gradlew test --info --stacktrace"
    puts "Tests completed!"
  end

  desc 'Run tests for a specific module'
  task :module, [:module_name] do |t, args|
    module_name = args[:module_name] || abort("Usage: rake test:module[enrollment-service]")
    puts "Running tests for module: #{module_name}..."
    setup_podman_env
    sh "./gradlew :services:#{module_name}:test --info"
    puts "Module tests completed!"
  end

  desc 'Run shared module tests'
  task :shared do
    puts "Running shared module tests..."
    sh "./gradlew :shared:test"
    puts "Shared tests completed!"
  end

  desc 'Run all tests with detailed coverage analysis'
  task :full_coverage do
    puts "Running full test suite with coverage analysis..."
    setup_podman_env
    sh "./gradlew clean test jacocoTestReport jacocoTestCoverageVerification --info"
    puts ""
    puts "=" * 60
    puts "COVERAGE REPORT SUMMARY"
    puts "=" * 60
    puts ""
    puts "Coverage verification: PASSED (minimum 98%)"
    puts "=" * 60
  end
end

namespace :k8s do
  desc 'Deploy to Kubernetes'
  task :deploy do
    puts "Deploying to Kubernetes..."
    sh "kubectl apply -f infra/k8s/namespace.yml"
    sh "kubectl apply -f infra/k8s/postgresql/"
    sh "kubectl apply -f infra/k8s/rabbitmq/"
    sh "kubectl apply -f infra/k8s/keycloak/"
    sh "kubectl apply -f infra/k8s/config-server/"
    sh "kubectl apply -f infra/k8s/catalog-service/"
    sh "kubectl apply -f infra/k8s/enrollment-service/"
    sh "kubectl apply -f infra/k8s/certification-service/"
    sh "kubectl apply -f infra/k8s/notification-service/"
    sh "kubectl apply -f infra/k8s/observability/"
    sh "kubectl apply -f infra/k8s/kong/"
    puts "Deployment completed!"
  end

  desc 'Undeploy from Kubernetes'
  task :undeploy do
    puts "Undeploying from Kubernetes..."
    sh "kubectl delete namespace training-platform --ignore-not-found"
    puts "Undeploy completed!"
  end

  desc 'Show Kubernetes status'
  task :status do
    sh "kubectl get all -n training-platform"
  end

  desc 'View Kubernetes logs'
  task :logs, [:service] do |t, args|
    service_name = args[:service] || 'training-catalog-service'
    sh "kubectl logs -f deployment/#{service_name} -n training-platform"
  end
end

namespace :db do
  desc 'Create all databases'
  task :create do
    puts "Creating databases..."
    databases = ['training_catalog', 'training_enrollment', 'training_certification', 'training_notification', 'training_employee']
    databases.each do |db|
      puts "Creating database #{db}..."
      sh "#{CONTAINER_ENGINE} exec training-postgresql psql -U postgres -c \"CREATE DATABASE #{db};\" || true"
    end
    puts "Databases created!"
  end

  desc 'Drop all databases'
  task :drop do
    puts "WARNING: This will delete all data!"
    puts "Press Ctrl+C to cancel, wait 3 seconds to continue..."
    sleep 3
    databases = ['training_catalog', 'training_enrollment', 'training_certification', 'training_notification', 'training_employee']
    databases.each do |db|
      puts "Dropping database #{db}..."
      sh "#{CONTAINER_ENGINE} exec training-postgresql psql -U postgres -c \"DROP DATABASE IF EXISTS #{db};\" || true"
    end
    puts "Databases dropped!"
  end
end

desc 'Show all available tasks'
task :help do
  puts "Corporate Training Platform - Available Tasks"
  puts "=" * 60
  puts ""
  puts "Infrastructure (Podman):"
  puts "  rake infra:start       - Start infrastructure services"
  puts "  rake infra:stop        - Stop infrastructure services"
  puts "  rake infra:restart     - Restart infrastructure services"
  puts "  rake infra:status      - Show infrastructure status"
  puts "  rake infra:logs        - View infrastructure logs"
  puts "  rake infra:clean       - Remove all data and volumes"
  puts ""
  puts "Build:"
  puts "  rake build:all         - Build all services"
  puts "  rake build:service[X]  - Build specific service"
  puts "  rake build:images      - Build Podman images"
  puts ""
  puts "Run:"
  puts "  rake run:service[X]    - Run specific service"
  puts "  rake run:all           - Run all services"
  puts ""
  puts "Test (Core):"
  puts "  rake test:all          - Run all tests (unit + integration)"
  puts "  rake test:unit         - Run unit tests only (fast, no containers)"
  puts "  rake test:integration  - Run integration tests (Podman required)"
  puts "  rake test:service[X]   - Run specific service tests"
  puts "  rake test:module[X]    - Run tests for a module"
  puts "  rake test:shared       - Run shared module tests"
  puts "  rake test:class[X]     - Run a specific test class"
  puts "  rake test:verbose      - Run tests with verbose output"
  puts ""
  puts "Test (Coverage):"
  puts "  rake test:coverage     - Run tests with JaCoCo coverage report"
  puts "  rake test:full_coverage - Full test suite with 98% coverage verification"
  puts ""
  puts "Test (E2E):"
  puts "  rake test:e2e          - Run E2E tests (Podman required)"
  puts "  rake test:e2e_coverage - Run E2E tests with coverage"
  puts "  rake test:e2e_cleanup  - Clean up E2E test containers"
  puts ""
  puts "Kubernetes:"
  puts "  rake k8s:deploy        - Deploy to Kubernetes"
  puts "  rake k8s:undeploy      - Undeploy from Kubernetes"
  puts "  rake k8s:status        - Show Kubernetes status"
  puts "  rake k8s:logs[X]       - View Kubernetes logs"
  puts ""
  puts "Database:"
  puts "  rake db:create         - Create all databases"
  puts "  rake db:drop           - Drop all databases"
  puts ""
  puts "Help:"
  puts "  rake help              - Show this help"
  puts ""
  puts "Quick Start:"
  puts "  rake test:unit           - Fast feedback (no Podman needed)"
  puts "  rake test:integration    - Full integration (Podman needed)"
  puts "  rake test:full_coverage  - Complete verification"
end

task :default => :help
