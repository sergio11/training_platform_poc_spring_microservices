package com.poc.training.enrollment.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebClientConfigTest {

    private final WebClientConfig webClientConfig = new WebClientConfig();

    private ClientRequest mockClientRequest() {
        ClientRequest request = mock(ClientRequest.class);
        when(request.url()).thenReturn(URI.create("http://localhost/test"));
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.headers()).thenReturn(new HttpHeaders());
        when(request.cookies()).thenReturn(MultiValueMap.fromSingleValue(Collections.emptyMap()));
        when(request.attributes()).thenReturn(Collections.emptyMap());
        return request;
    }

    @Test
    void shouldCreateWebClientBuilder() {
        WebClient.Builder builder = webClientConfig.webClientBuilder(3000, 5000);
        assertThat(builder).isNotNull();
    }

    @Test
    void shouldCreateWebClientFromBuilder() {
        WebClient.Builder builder = webClientConfig.webClientBuilder(3000, 5000);
        WebClient client = builder.baseUrl("http://localhost").build();
        assertThat(client).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRelayAuthorizationHeader() throws Exception {
        Method method = WebClientConfig.class.getDeclaredMethod("tokenRelayFilter");
        method.setAccessible(true);
        ExchangeFilterFunction filter = (ExchangeFilterFunction) method.invoke(webClientConfig);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest(HttpMethod.GET.name(), "/test");
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        try {
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
            when(exchangeFunction.exchange(any(ClientRequest.class)))
                    .thenReturn(Mono.just(mock(ClientResponse.class)));

            ClientRequest request = mockClientRequest();

            filter.filter(request, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());
            assertThat(captor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer test-token");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotRelayWhenNoAuthHeader() throws Exception {
        Method method = WebClientConfig.class.getDeclaredMethod("tokenRelayFilter");
        method.setAccessible(true);
        ExchangeFilterFunction filter = (ExchangeFilterFunction) method.invoke(webClientConfig);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest(HttpMethod.GET.name(), "/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        try {
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
            when(exchangeFunction.exchange(any(ClientRequest.class)))
                    .thenReturn(Mono.just(mock(ClientResponse.class)));

            ClientRequest request = mockClientRequest();

            filter.filter(request, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());
            assertThat(captor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void shouldReturnNullWhenNoRequestContext() throws Exception {
        Method method = WebClientConfig.class.getDeclaredMethod("extractAuthHeader");
        method.setAccessible(true);

        RequestContextHolder.resetRequestAttributes();
        String result = (String) method.invoke(webClientConfig);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenRequestContextThrowsException() throws Exception {
        Method method = WebClientConfig.class.getDeclaredMethod("extractAuthHeader");
        method.setAccessible(true);

        RequestContextHolder.setRequestAttributes(mock(ServletRequestAttributes.class));
        try {
            String result = (String) method.invoke(webClientConfig);
            assertThat(result).isNull();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
