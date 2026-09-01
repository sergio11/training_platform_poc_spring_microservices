package com.poc.training.employee.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    @Primary
    public WebClient.Builder webClientBuilder(
            @Value("${client.connect-timeout:3000}") int connectTimeout,
            @Value("${client.read-timeout:5000}") int readTimeout) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(tokenRelayFilter());
    }

    private ExchangeFilterFunction tokenRelayFilter() {
        return (request, next) -> {
            String authHeader = extractAuthHeader();
            if (authHeader != null) {
                ClientRequest filteredRequest = ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .build();
                return next.exchange(filteredRequest);
            }
            return next.exchange(request);
        };
    }

    private String extractAuthHeader() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                String auth = servletAttrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (auth == null) {
                    log.debug("No Authorization header found in current request");
                }
                return auth;
            }
            log.debug("No ServletRequestAttributes in context — likely called from non-HTTP thread");
        } catch (Exception e) {
            log.debug("Could not extract auth header: {}", e.getMessage());
        }
        return null;
    }
}
