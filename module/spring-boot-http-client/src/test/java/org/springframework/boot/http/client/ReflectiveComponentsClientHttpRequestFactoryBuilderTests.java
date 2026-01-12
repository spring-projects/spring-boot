/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.http.client;

import java.net.URI;
import java.time.Duration;

import org.eclipse.jetty.client.HttpClient;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ReflectiveComponentsClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 */
class ReflectiveComponentsClientHttpRequestFactoryBuilderTests
		extends AbstractClientHttpRequestFactoryBuilderTests<ClientHttpRequestFactory> {

	ReflectiveComponentsClientHttpRequestFactoryBuilderTests() {
		super(ClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.of(JettyClientHttpRequestFactory::new));
	}

	@Override
	void connectWithSslBundle(String httpMethod) throws Exception {
		HttpClientSettings settings = HttpClientSettings.ofSslBundle(sslBundle());
		assertThatIllegalStateException().isThrownBy(() -> ofTestRequestFactory().build(settings))
			.withMessage("Unable to set SSL bundler using reflection");
	}

	@Override
	void redirectFollow(String httpMethod) throws Exception {
		HttpClientSettings settings = HttpClientSettings.defaults().withRedirects(HttpRedirects.FOLLOW);
		assertThatIllegalStateException().isThrownBy(() -> ofTestRequestFactory().build(settings))
			.withMessage("Unable to set redirect follow using reflection");
	}

	@Override
	void redirectDontFollow(String httpMethod) throws Exception {
		HttpClientSettings settings = HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);
		assertThatIllegalStateException().isThrownBy(() -> ofTestRequestFactory().build(settings))
			.withMessage("Unable to set redirect follow using reflection");
	}

	@Override
	void connectWithSslBundleAndOptionsMismatch(String httpMethod) throws Exception {
		assertThatIllegalStateException().isThrownBy(() -> super.connectWithSslBundleAndOptionsMismatch(httpMethod))
			.withMessage("Unable to set SSL bundler using reflection");
	}

	@Test
	void buildWithClassCreatesFactory() {
		assertThat(ofTestRequestFactory().build()).isInstanceOf(TestClientHttpRequestFactory.class);
	}

	@Test
	void buildWithClassWhenHasConnectTimeout() {
		HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(60));
		TestClientHttpRequestFactory requestFactory = ofTestRequestFactory().build(settings);
		assertThat(requestFactory.connectTimeout).isEqualTo(Duration.ofSeconds(60).toMillis());
	}

	@Test
	void buildWithClassWhenHasReadTimeout() {
		HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofSeconds(90));
		TestClientHttpRequestFactory requestFactory = ofTestRequestFactory().build(settings);
		assertThat(requestFactory.readTimeout).isEqualTo(Duration.ofSeconds(90).toMillis());
	}

	@Test
	void buildWithClassWhenUnconfigurableTypeWithConnectTimeoutThrowsException() {
		HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(60));
		assertThatIllegalStateException().isThrownBy(() -> ofUnconfigurableRequestFactory().build(settings))
			.withMessageContaining("suitable setConnectTimeout method");
	}

	@Test
	void buildWithClassWhenUnconfigurableTypeWithReadTimeoutThrowsException() {
		HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofSeconds(60));
		assertThatIllegalStateException().isThrownBy(() -> ofUnconfigurableRequestFactory().build(settings))
			.withMessageContaining("suitable setReadTimeout method");
	}

	@Test
	void buildWithClassWhenDeprecatedMethodsTypeWithConnectTimeoutThrowsException() {
		HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(60));
		assertThatIllegalStateException().isThrownBy(() -> ofDeprecatedMethodsRequestFactory().build(settings))
			.withMessageContaining("setConnectTimeout method marked as deprecated");
	}

	@Test
	void buildWithClassWhenDeprecatedMethodsTypeWithReadTimeoutThrowsException() {
		HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofSeconds(60));
		assertThatIllegalStateException().isThrownBy(() -> ofDeprecatedMethodsRequestFactory().build(settings))
			.withMessageContaining("setReadTimeout method marked as deprecated");
	}

	@Test
	void buildWithSupplierWhenWrappedRequestFactoryTypeWithConnectTimeout() {
		HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofMillis(1234));
		SimpleClientHttpRequestFactory wrappedRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
			.of(() -> new BufferingClientHttpRequestFactory(wrappedRequestFactory))
			.build(settings);
		assertThat(requestFactory).extracting("requestFactory").isSameAs(wrappedRequestFactory);
		assertThat(wrappedRequestFactory).hasFieldOrPropertyWithValue("connectTimeout", 1234);
	}

	@Test
	void buildWithSupplierWhenWrappedRequestFactoryTypeWithReadTimeout() {
		HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofMillis(1234));
		SimpleClientHttpRequestFactory wrappedRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
			.of(() -> new BufferingClientHttpRequestFactory(wrappedRequestFactory))
			.build(settings);
		assertThat(requestFactory).extracting("requestFactory").isSameAs(wrappedRequestFactory);
		assertThat(wrappedRequestFactory).hasFieldOrPropertyWithValue("readTimeout", 1234);
	}

	@Test
	void buildWithClassWhenHasMultipleTimeoutSettersFavorsDurationMethods() {
		HttpClientSettings settings = HttpClientSettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(1))
			.withReadTimeout(Duration.ofSeconds(2));
		IntAndDurationTimeoutsClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
			.of(IntAndDurationTimeoutsClientHttpRequestFactory.class)
			.build(settings);
		assertThat((requestFactory).connectTimeout).isZero();
		assertThat((requestFactory).readTimeout).isZero();
		assertThat((requestFactory).connectTimeoutDuration).isEqualTo(Duration.ofSeconds(1));
		assertThat((requestFactory).readTimeoutDuration).isEqualTo(Duration.ofSeconds(2));
	}

	private ClientHttpRequestFactoryBuilder<TestClientHttpRequestFactory> ofTestRequestFactory() {
		return ClientHttpRequestFactoryBuilder.of(TestClientHttpRequestFactory.class);
	}

	private ClientHttpRequestFactoryBuilder<UnconfigurableClientHttpRequestFactory> ofUnconfigurableRequestFactory() {
		return ClientHttpRequestFactoryBuilder.of(UnconfigurableClientHttpRequestFactory.class);
	}

	private ClientHttpRequestFactoryBuilder<DeprecatedMethodsClientHttpRequestFactory> ofDeprecatedMethodsRequestFactory() {
		return ClientHttpRequestFactoryBuilder.of(DeprecatedMethodsClientHttpRequestFactory.class);
	}

	@Override
	protected long connectTimeout(ClientHttpRequestFactory requestFactory) {
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");
		assertThat(httpClient).isNotNull();
		return httpClient.getConnectTimeout();
	}

	@Override
	protected long readTimeout(ClientHttpRequestFactory requestFactory) {
		Object field = ReflectionTestUtils.getField(requestFactory, "readTimeout");
		assertThat(field).isNotNull();
		return (long) field;
	}

	public static class TestClientHttpRequestFactory implements ClientHttpRequestFactory {

		private int connectTimeout;

		private int readTimeout;

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
			throw new UnsupportedOperationException();
		}

		public void setConnectTimeout(int timeout) {
			this.connectTimeout = timeout;
		}

		public void setReadTimeout(int timeout) {
			this.readTimeout = timeout;
		}

	}

	public static class UnconfigurableClientHttpRequestFactory implements ClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
			throw new UnsupportedOperationException();
		}

	}

	public static class DeprecatedMethodsClientHttpRequestFactory implements ClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
			throw new UnsupportedOperationException();
		}

		@Deprecated(since = "3.0.0", forRemoval = false)
		public void setConnectTimeout(int timeout) {
		}

		@Deprecated(since = "3.0.0", forRemoval = false)
		public void setReadTimeout(int timeout) {
		}

		@Deprecated(since = "3.0.0", forRemoval = false)
		public void setBufferRequestBody(boolean bufferRequestBody) {
		}

	}

	public static class IntAndDurationTimeoutsClientHttpRequestFactory implements ClientHttpRequestFactory {

		private int readTimeout;

		private int connectTimeout;

		private @Nullable Duration readTimeoutDuration;

		private @Nullable Duration connectTimeoutDuration;

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
			throw new UnsupportedOperationException();
		}

		public void setConnectTimeout(int timeout) {
			this.connectTimeout = timeout;
		}

		public void setReadTimeout(int timeout) {
			this.readTimeout = timeout;
		}

		public void setConnectTimeout(@Nullable Duration timeout) {
			this.connectTimeoutDuration = timeout;
		}

		public void setReadTimeout(@Nullable Duration timeout) {
			this.readTimeoutDuration = timeout;
		}

	}

}
