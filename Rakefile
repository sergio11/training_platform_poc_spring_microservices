# Rakefile for Corporate Training Platform POC

CONTAINER_ENGINE = ENV['CONTAINER_ENGINE'] || 'podman'
PODMAN_DOCKER_HOST = "npipe:////./pipe/podman-machine-default"

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
      sh "#{cmd} clean test jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --no-build-cache --no-daemon"
    ensure
      print_coverage_table
    end
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
