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

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 */
class ClientHttpRequestFactoryBuilderTests {

	@Test
	void withCustomizerAppliesCustomizers() {
		ClientHttpRequestFactoryBuilder<JettyClientHttpRequestFactory> builder = (
				settings) -> new JettyClientHttpRequestFactory();
		builder = builder.withCustomizer(this::setJettyReadTimeout);
		JettyClientHttpRequestFactory factory = builder.build(null);
		assertThat(factory).extracting("readTimeout").isEqualTo(5000L);
	}

	@Test
	void withCustomizersAppliesCustomizers() {
		ClientHttpRequestFactoryBuilder<JettyClientHttpRequestFactory> builder = (
				settings) -> new JettyClientHttpRequestFactory();
		builder = builder.withCustomizers(List.of(this::setJettyReadTimeout));
		JettyClientHttpRequestFactory factory = builder.build(null);
		assertThat(factory).extracting("readTimeout").isEqualTo(5000L);
	}

	@Test
	void httpComponentsReturnsHttpComponentsFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.httpComponents())
			.isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void jettyReturnsJettyFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.jetty()).isInstanceOf(JettyClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void reactorReturnsReactorFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.reactor())
			.isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void jdkReturnsJdkFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.jdk()).isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void simpleReturnsSimpleFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.simple()).isInstanceOf(SimpleClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void ofWhenExactlyClientHttpRequestFactoryTypeThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ClientHttpRequestFactoryBuilder.of(ClientHttpRequestFactory.class))
			.withMessage("'requestFactoryType' must be an implementation of ClientHttpRequestFactory");
	}

	@Test
	void ofWhenSimpleFactoryReturnsSimpleFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.of(SimpleClientHttpRequestFactory.class))
			.isInstanceOf(SimpleClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void ofWhenHttpComponentsFactoryReturnsHttpComponentsFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.of(HttpComponentsClientHttpRequestFactory.class))
			.isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void ofWhenReactorFactoryReturnsReactorFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.of(ReactorClientHttpRequestFactory.class))
			.isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void ofWhenJdkFactoryReturnsJdkFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.of(JdkClientHttpRequestFactory.class))
			.isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void ofWhenUnknownTypeReturnsReflectiveFactoryBuilder() {
		ClientHttpRequestFactoryBuilder<TestClientHttpRequestFactory> builder = ClientHttpRequestFactoryBuilder
			.of(TestClientHttpRequestFactory.class);
		assertThat(builder).isInstanceOf(ReflectiveComponentsClientHttpRequestFactoryBuilder.class);
		assertThat(builder.build(null)).isInstanceOf(TestClientHttpRequestFactory.class);
	}

	@Test
	void ofWithSupplierWhenSupplierIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ClientHttpRequestFactoryBuilder.of((Supplier<ClientHttpRequestFactory>) null))
			.withMessage("'requestFactorySupplier' must not be null");
	}

	@Test
	void ofWithSupplierReturnsReflectiveFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.of(SimpleClientHttpRequestFactory::new))
			.isInstanceOf(ReflectiveComponentsClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void detectWhenHttpComponents() {
		assertThat(ClientHttpRequestFactoryBuilder.detect())
			.isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
	}

	@Test
	@ClassPathExclusions("httpclient5-*.jar")
	void detectWhenJetty() {
		assertThat(ClientHttpRequestFactoryBuilder.detect()).isInstanceOf(JettyClientHttpRequestFactoryBuilder.class);
	}

	@Test
	@ClassPathExclusions({ "httpclient5-*.jar", "jetty-client-*.jar" })
	void detectWhenReactor() {
		assertThat(ClientHttpRequestFactoryBuilder.detect()).isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
	}

	@Test
	@ClassPathExclusions({ "httpclient5-*.jar", "jetty-client-*.jar", "reactor-netty-http-*.jar" })
	void detectWhenJdk() {
		assertThat(ClientHttpRequestFactoryBuilder.detect()).isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
	}

	private void setJettyReadTimeout(JettyClientHttpRequestFactory factory) {
		factory.setReadTimeout(Duration.ofSeconds(5));
	}

	public static class TestClientHttpRequestFactory implements ClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
			throw new UnsupportedOperationException();
		}

	}

}
