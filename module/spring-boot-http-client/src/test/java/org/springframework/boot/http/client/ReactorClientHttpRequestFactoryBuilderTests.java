/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ReactorClientHttpRequestFactoryBuilder} and
 * {@link ReactorHttpClientBuilder}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ReactorClientHttpRequestFactoryBuilderTests
		extends AbstractClientHttpRequestFactoryBuilderTests<ReactorClientHttpRequestFactory> {

	ReactorClientHttpRequestFactoryBuilderTests() {
		super(ReactorClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.reactor());
	}

	@Test
	void withHttpClientFactory() {
		boolean[] called = new boolean[1];
		Supplier<HttpClient> httpClientFactory = () -> {
			called[0] = true;
			return HttpClient.create();
		};
		ClientHttpRequestFactoryBuilder.reactor().withHttpClientFactory(httpClientFactory).build();
		assertThat(called).containsExactly(true);
	}

	@Test
	void springHttpClientDefaults() {
		ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.reactor().build();
		assertThat(factory).extracting("httpClient.config.acceptGzip").isEqualTo(true);
	}

	@Test
	void withoutHttpClientDefaults() {
		ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.reactor()
			.withoutHttpClientDefaults()
			.build();
		assertThat(factory).extracting("httpClient.config.acceptGzip").isEqualTo(false);
	}

	@Test
	void withHttpClientDefaults() {
		ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.reactor()
			.withHttpClientDefaults((httpClient) -> httpClient.baseUrl("test"))
			.build();
		assertThat(factory).extracting("httpClient.config.acceptGzip").isEqualTo(false);
		assertThat(factory).extracting("httpClient.config.baseUrl").isEqualTo("test");
	}

	@Test
	void withReactorResourceFactory() {
		ReactorResourceFactory resourceFactory = spy(new ReactorResourceFactory());
		ClientHttpRequestFactoryBuilder.reactor().withReactorResourceFactory(resourceFactory).build();
		then(resourceFactory).should().getConnectionProvider();
		then(resourceFactory).should().getLoopResources();
	}

	@Test
	void withCustomizers() {
		List<HttpClient> httpClients = new ArrayList<>();
		UnaryOperator<HttpClient> httpClientCustomizer1 = (httpClient) -> {
			httpClients.add(httpClient);
			return httpClient;
		};
		UnaryOperator<HttpClient> httpClientCustomizer2 = (httpClient) -> {
			httpClients.add(httpClient);
			return httpClient;
		};
		ClientHttpRequestFactoryBuilder.reactor()
			.withHttpClientCustomizer(httpClientCustomizer1)
			.withHttpClientCustomizer(httpClientCustomizer2)
			.build();
		assertThat(httpClients).hasSize(2);
	}

	@Test
	void with() {
		boolean[] called = new boolean[1];
		Supplier<HttpClient> httpClientFactory = () -> {
			called[0] = true;
			return HttpClient.create();
		};
		ClientHttpRequestFactoryBuilder.reactor()
			.with((builder) -> builder.withHttpClientFactory(httpClientFactory))
			.build();
		assertThat(called).containsExactly(true);
	}

	@ParameterizedTest
	@EnumSource(names = { "DISABLE", "ENABLE_WHEN_POSSIBLE" })
	void doesNotThrowWhenCookieHandlingNotEnabled(HttpCookieHandling cookieHandling) {
		assertThatNoException().isThrownBy(() -> ClientHttpRequestFactoryBuilder.reactor()
			.build(HttpClientSettings.defaults().withCookieHandling(cookieHandling)));
	}

	@Test
	void throwsWhenCookieHandlingEnabled() {
		assertThatIllegalArgumentException().isThrownBy(() -> ClientHttpRequestFactoryBuilder.reactor()
			.build(HttpClientSettings.defaults().withCookieHandling(HttpCookieHandling.ENABLE)));
	}

	@Override
	protected long connectTimeout(ReactorClientHttpRequestFactory requestFactory) {
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");
		assertThat(httpClient).isNotNull();
		Object connectTimeout = httpClient.configuration().options().get(ChannelOption.CONNECT_TIMEOUT_MILLIS);
		assertThat(connectTimeout).isNotNull();
		return (int) connectTimeout;
	}

	@Override
	protected long readTimeout(ReactorClientHttpRequestFactory requestFactory) {
		Duration readTimeout = (Duration) ReflectionTestUtils.getField(requestFactory, "readTimeout");
		assertThat(readTimeout).isNotNull();
		return readTimeout.toMillis();
	}

}
