# Rakefile for Corporate Training Platform POC

require 'open3'

# Container engine: 'podman' or 'docker'
CONTAINER_ENGINE = ENV['CONTAINER_ENGINE'] || 'podman'

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

  desc 'Build Docker/Podman images'
  task :images do
    puts "Building container images..."
    services = [
      { name: 'training-catalog-service', port: 8081 },
      { name: 'enrollment-service', port: 8082 },
      { name: 'certification-service', port: 8083 },
      { name: 'notification-service', port: 8084 }
    ]
    services.each do |service|
      puts "Building #{service[:name]}..."
      sh "cd services/#{service[:name]} && #{CONTAINER_ENGINE} build -t #{service[:name]}:latest ."
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
  desc 'Run all tests'
  task :all do
    puts "Running all tests..."
    sh "./gradlew test"
    puts "All tests completed!"
  end

  desc 'Run specific service tests'
  task :service, [:name] do |t, args|
    service_name = args[:name] || abort("Usage: rake test:service[service-name]")
    puts "Running tests for #{service_name}..."
    sh "./gradlew :services:#{service_name}:test"
    puts "Tests completed!"
  end

  desc 'Run tests with coverage'
  task :coverage do
    puts "Running tests with coverage..."
    sh "./gradlew test jacocoTestReport"
    puts "Test report generated at build/reports/jacoco/"
  end
end

namespace :k8s do
  desc 'Deploy to Kubernetes'
  task :deploy do
    puts "Deploying to Kubernetes..."
    sh "kubectl apply -f infra/k8s/namespace.yml"
    sh "kubectl apply -f infra/k8s/config-server/"
    sh "kubectl apply -f infra/k8s/catalog-service/"
    sh "kubectl apply -f infra/k8s/enrollment-service/"
    sh "kubectl apply -f infra/k8s/certification-service/"
    sh "kubectl apply -f infra/k8s/notification-service/"
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
    databases = ['training_catalog', 'training_enrollment', 'training_certification', 'training_notification']
    databases.each do |db|
      puts "Creating database #{db}..."
      sh "podman exec training-postgresql psql -U postgres -c \"CREATE DATABASE #{db};\" || true"
    end
    puts "Databases created!"
  end

  desc 'Drop all databases'
  task :drop do
    puts "WARNING: This will delete all data!"
    puts "Press Ctrl+C to cancel, wait 3 seconds to continue..."
    sleep 3
    databases = ['training_catalog', 'training_enrollment', 'training_certification', 'training_notification']
    databases.each do |db|
      puts "Dropping database #{db}..."
      sh "podman exec training-postgresql psql -U postgres -c \"DROP DATABASE IF EXISTS #{db};\" || true"
    end
    puts "Databases dropped!"
  end
end

desc 'Show all available tasks'
task :help do
  puts "Corporate Training Platform - Available Tasks"
  puts "=" * 50
  puts ""
  puts "Infrastructure:"
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
  puts "  rake build:images      - Build container images"
  puts ""
  puts "Run:"
  puts "  rake run:service[X]    - Run specific service"
  puts "  rake run:all           - Run all services"
  puts ""
  puts "Test:"
  puts "  rake test:all          - Run all tests"
  puts "  rake test:service[X]   - Run specific service tests"
  puts "  rake test:coverage     - Run tests with coverage"
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
end

task :default => :help
