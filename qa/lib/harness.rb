# Shared QA helpers for the local kind/Podman platform.
# Requires: kubectl configured against the `poc-qa` kind cluster.
require 'base64'
require 'json'
require 'net/http'
require 'open3'
require 'openssl'
require 'socket'
require 'uri'

module QA
  NAMESPACE = ENV.fetch('K8S_NAMESPACE', 'training-platform')
  KUBECTL   = ENV.fetch('KUBECTL', 'kubectl')

  KEYCLOAK_LOCAL_PORT = Integer(ENV.fetch('KEYCLOAK_LOCAL_PORT', '18080'))
  KONG_LOCAL_PORT     = Integer(ENV.fetch('KONG_LOCAL_PORT', '18000'))
  PROMETHEUS_LOCAL_PORT = Integer(ENV.fetch('PROMETHEUS_LOCAL_PORT', '19090'))

  KEYCLOAK_URL = "http://127.0.0.1:#{KEYCLOAK_LOCAL_PORT}"
  KONG_URL     = "http://127.0.0.1:#{KONG_LOCAL_PORT}"
  REALM        = 'training-realm'
  ISSUER       = "http://keycloak:8080/realms/#{REALM}"
  CLIENT_ID    = 'training-catalog-client'
  CLIENT_SECRET = 'catalog-secret-2024'

  class Failure < StandardError; end

  module_function

  def kubectl(*args, allow_failure: false)
    out, err, status = Open3.capture3(KUBECTL, *args)
    unless status.success? || allow_failure
      raise Failure, "kubectl #{args.join(' ')} failed:\n#{err}\n#{out}"
    end
    [out, err, status]
  end

  def kubectl_json(*args)
    out, = kubectl(*args)
    JSON.parse(out)
  end

  def port_forward(service, local_port, remote_port)
    pid = Process.spawn(
      KUBECTL, 'port-forward', service, "#{local_port}:#{remote_port}",
      '--namespace', NAMESPACE,
      out: File::NULL, err: File::NULL
    )
    @forwards ||= []
    @forwards << pid

    wait_until(timeout: 60, interval: 1, label: "port-forward #{service}") do
      begin
        TCPSocket.new('127.0.0.1', local_port).close
        true
      rescue StandardError
        false
      end
    end
    pid
  rescue StandardError
    Process.kill('KILL', pid) rescue nil
    raise
  end

  def stop_forwards
    (@forwards || []).each do |pid|
      Process.kill('KILL', pid)
    rescue StandardError
      nil
    end
    @forwards = []
  end

  def wait_until(timeout: 300, interval: 2, label: 'condition')
    deadline = Time.now + timeout
    last_error = nil
    loop do
      begin
        value = yield
        return value if value
      rescue StandardError => e
        last_error = e
      end
      if Time.now >= deadline
        raise Failure, "Timed out waiting for #{label}: #{last_error}"
      end
      sleep interval
    end
  end

  def http(method, url, token: nil, body: nil, form: nil, timeout: 60)
    uri = URI(url)
    request_class = {
      get: Net::HTTP::Get,
      post: Net::HTTP::Post,
      patch: Net::HTTP::Patch,
      put: Net::HTTP::Put,
      delete: Net::HTTP::Delete
    }.fetch(method)

    request = request_class.new(uri)
    request['Accept'] = 'application/json'
    request['Authorization'] = "Bearer #{token}" if token

    if body
      request['Content-Type'] = 'application/json'
      request.body = JSON.generate(body)
    elsif form
      request['Content-Type'] = 'application/x-www-form-urlencoded'
      request.body = URI.encode_www_form(form)
    end

    response = Net::HTTP.start(
      uri.host, uri.port,
      use_ssl: uri.scheme == 'https',
      open_timeout: timeout,
      read_timeout: timeout
    ) { |client| client.request(request) }

    [response.code.to_i, response.body.to_s, response]
  end

  def http_json(method, url, **options)
    code, body, response = http(method, url, **options)
    parsed = body.empty? ? nil : JSON.parse(body)
    [code, parsed, response]
  rescue JSON::ParserError
    [code, body, response]
  end

  def token(username, password = username)
    code, body, = http_json(
      :post,
      "#{KEYCLOAK_URL}/realms/#{REALM}/protocol/openid-connect/token",
      form: {
        grant_type: 'password',
        client_id: CLIENT_ID,
        client_secret: CLIENT_SECRET,
        username: username,
        password: password
      }
    )
    unless code == 200 && body.is_a?(Hash) && body['access_token']
      raise Failure, "Could not obtain token for #{username} (HTTP #{code}): #{body}"
    end
    body['access_token']
  end

  def sql(database, statement)
    out, err, status = kubectl(
      'exec', 'postgresql-0', '--namespace', NAMESPACE, '--',
      'psql', '-U', 'postgres', '-d', database, '-tAc', statement
    )
    raise Failure, "SQL failed on #{database}: #{err}" unless status.success?
    out.strip
  end

  def rabbitmq_queues
    out, err, status = kubectl(
      'exec', 'deployment/training-rabbitmq', '--namespace', NAMESPACE, '--',
      'rabbitmqctl', 'list_queues', 'name', 'messages_ready',
      'messages_unacknowledged', '--formatter', 'json'
    )
    raise Failure, "rabbitmqctl failed: #{err}" unless status.success?
    JSON.parse(out)
  end

  # Fixed RS256 public key configured in Kong, also used to prove that Keycloak
  # imports the deterministic realm key instead of generating a random one.
  def kong_rsa_public_key
    configmap = File.read(File.expand_path('../../infra/k8s/kong/configmap.yml', __dir__))
    pem = configmap[/-----BEGIN PUBLIC KEY-----\n(.*?)-----END PUBLIC KEY-----/m, 1]
    raise Failure, 'Kong public key not found in configmap' unless pem
    body = pem.lines.map(&:strip).reject(&:empty?).join("\n")
    OpenSSL::PKey::RSA.new("-----BEGIN PUBLIC KEY-----\n#{body}\n-----END PUBLIC KEY-----\n")
  end

  def rsa_modulus_b64url(rsa_key)
    Base64.urlsafe_encode64(rsa_key.n.to_s(2), padding: false)
  end

  def deployment_ready?(name)
    data = kubectl_json('get', 'deployment', name, '--namespace', NAMESPACE, '-o', 'json')
    data.dig('status', 'readyReplicas').to_i >= 1
  rescue Failure
    false
  end

  def statefulset_ready?(name)
    data = kubectl_json('get', 'statefulset', name, '--namespace', NAMESPACE, '-o', 'json')
    data.dig('status', 'readyReplicas').to_i >= 1
  rescue Failure
    false
  end

  def pod_restart_total
    pods = kubectl_json('get', 'pods', '--namespace', NAMESPACE, '-o', 'json')
    pods.fetch('items', []).sum do |pod|
      pod.dig('status', 'containerStatuses').to_a.sum { |cs| cs['restartCount'].to_i }
    end
  end

  def now_stamp
    Time.now.utc.strftime('%Y%m%d%H%M%S')
  end

  def check(label, condition, details = nil)
    if condition
      puts "  [PASS] #{label}"
      true
    else
      puts "  [FAIL] #{label}#{details ? " - #{details}" : ''}"
      false
    end
  end

  def assert(label, condition, details = nil)
    return if check(label, condition, details)
    raise Failure, label
  end
end
