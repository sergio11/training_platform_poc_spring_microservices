# Rakefile for Corporate Training Platform POC

CONTAINER_ENGINE = ENV['CONTAINER_ENGINE'] || 'podman'
PODMAN_DOCKER_HOST = "npipe:////./pipe/podman-machine-default"

def setup_podman_env
  ENV['DOCKER_HOST'] ||= PODMAN_DOCKER_HOST
  ENV['TESTCONTAINERS_RYUK_DISABLED'] = 'true'
end

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
    sh "./gradlew clean test jacocoTestReport jacocoTestCoverageVerification --info"
  end
end

namespace :k8s do
  desc 'Deploy to Kubernetes'
  task :deploy do
    sh "kubectl apply -f infra/k8s/namespace.yml"
    sh "kubectl apply -f infra/k8s/postgresql/"
    sh "kubectl apply -f infra/k8s/rabbitmq/"
    sh "kubectl apply -f infra/k8s/keycloak/"
    sh "kubectl apply -f infra/k8s/config-server/"
    sh "kubectl apply -f infra/k8s/catalog-service/"
    sh "kubectl apply -f infra/k8s/employee-service/"
    sh "kubectl apply -f infra/k8s/enrollment-service/"
    sh "kubectl apply -f infra/k8s/certification-service/"
    sh "kubectl apply -f infra/k8s/notification-service/"
    sh "kubectl apply -f infra/k8s/observability/"
    sh "kubectl apply -f infra/k8s/kong/"
  end

  desc 'Undeploy from Kubernetes'
  task :undeploy do
    sh "kubectl delete namespace training-platform --ignore-not-found"
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
      rake k8s:deploy       Deploy to Kubernetes
      rake k8s:undeploy     Undeploy from Kubernetes
  HELP
end

task :default => :help
