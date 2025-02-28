/*
 * Copyright 2012-2024 the original author or authors.
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
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
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
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.ofSslBundle(sslBundle());
		assertThatIllegalStateException().isThrownBy(() -> ofTestRequestFactory().build(settings))
			.withMessage("Unable to set SSL bundler using reflection");
	}

	@Override
	void redirectFollow(String httpMethod) throws Exception {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withRedirects(Redirects.FOLLOW);
		assertThatIllegalStateException().isThrownBy(() -> ofTestRequestFactory().build(settings))
			.withMessage("Unable to set redirect follow using reflection");
	}

	@Override
	void redirectDontFollow(String httpMethod) throws Exception {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withRedirects(Redirects.DONT_FOLLOW);
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
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(60));
		TestClientHttpRequestFactory requestFactory = ofTestRequestFactory().build(settings);
		assertThat(requestFactory.connectTimeout).isEqualTo(Duration.ofSeconds(60).toMillis());
	}

	@Test
	void buildWithClassWhenHasReadTimeout() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withReadTimeout(Duration.ofSeconds(90));
		TestClientHttpRequestFactory requestFactory = ofTestRequestFactory().build(settings);
		assertThat(requestFactory.readTimeout).isEqualTo(Duration.ofSeconds(90).toMillis());
	}

	@Test
	void buildWithClassWhenUnconfigurableTypeWithConnectTimeoutThrowsException() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(60));
		assertThatIllegalStateException().isThrownBy(() -> ofUnconfigurableRequestFactory().build(settings))
			.withMessageContaining("suitable setConnectTimeout method");
	}

	@Test
	void buildWithClassWhenUnconfigurableTypeWithReadTimeoutThrowsException() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withReadTimeout(Duration.ofSeconds(60));
		assertThatIllegalStateException().isThrownBy(() -> ofUnconfigurableRequestFactory().build(settings))
			.withMessageContaining("suitable setReadTimeout method");
	}

	@Test
	void buildWithClassWhenDeprecatedMethodsTypeWithConnectTimeoutThrowsException() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(60));
		assertThatIllegalStateException().isThrownBy(() -> ofDeprecatedMethodsRequestFactory().build(settings))
			.withMessageContaining("setConnectTimeout method marked as deprecated");
	}

	@Test
	void buildWithClassWhenDeprecatedMethodsTypeWithReadTimeoutThrowsException() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withReadTimeout(Duration.ofSeconds(60));
		assertThatIllegalStateException().isThrownBy(() -> ofDeprecatedMethodsRequestFactory().build(settings))
			.withMessageContaining("setReadTimeout method marked as deprecated");
	}

	@Test
	void buildWithSupplierWhenWrappedRequestFactoryTypeWithConnectTimeout() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofMillis(1234));
		SimpleClientHttpRequestFactory wrappedRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
			.of(() -> new BufferingClientHttpRequestFactory(wrappedRequestFactory))
			.build(settings);
		assertThat(requestFactory).extracting("requestFactory").isSameAs(wrappedRequestFactory);
		assertThat(wrappedRequestFactory).hasFieldOrPropertyWithValue("connectTimeout", 1234);
	}

	@Test
	void buildWithSupplierWhenWrappedRequestFactoryTypeWithReadTimeout() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withReadTimeout(Duration.ofMillis(1234));
		SimpleClientHttpRequestFactory wrappedRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
			.of(() -> new BufferingClientHttpRequestFactory(wrappedRequestFactory))
			.build(settings);
		assertThat(requestFactory).extracting("requestFactory").isSameAs(wrappedRequestFactory);
		assertThat(wrappedRequestFactory).hasFieldOrPropertyWithValue("readTimeout", 1234);
	}

	@Test
	void buildWithClassWhenHasMultipleTimeoutSettersFavorsDurationMethods() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
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
		return ((HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient")).getConnectTimeout();
	}

	@Override
	protected long readTimeout(ClientHttpRequestFactory requestFactory) {
		return (long) ReflectionTestUtils.getField(requestFactory, "readTimeout");
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

		private Duration readTimeoutDuration;

		private Duration connectTimeoutDuration;

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

		public void setConnectTimeout(Duration timeout) {
			this.connectTimeoutDuration = timeout;
		}

		public void setReadTimeout(Duration timeout) {
			this.readTimeoutDuration = timeout;
		}

	}

}
