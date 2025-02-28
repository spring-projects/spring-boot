/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.http.client.reactive;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClientHttpConnectorBuilder}.
 *
 * @author Phillip Webb
 */
class ClientHttpConnectorBuilderTests {

	@Test
	void withCustomizerAppliesCustomizers() {
		ClientHttpConnectorBuilder<JdkClientHttpConnector> builder = (settings) -> new JdkClientHttpConnector();
		builder = builder.withCustomizer(this::setJdkReadTimeout);
		JdkClientHttpConnector connector = builder.build(null);
		assertThat(connector).extracting("readTimeout").isEqualTo(Duration.ofSeconds(5));
	}

	@Test
	void withCustomizersAppliesCustomizers() {
		ClientHttpConnectorBuilder<JdkClientHttpConnector> builder = (settings) -> new JdkClientHttpConnector();
		builder = builder.withCustomizers(List.of(this::setJdkReadTimeout));
		JdkClientHttpConnector connector = builder.build(null);
		assertThat(connector).extracting("readTimeout").isEqualTo(Duration.ofSeconds(5));
	}

	@Test
	void reactorReturnsReactorFactoryBuilder() {
		assertThat(ClientHttpConnectorBuilder.reactor()).isInstanceOf(ReactorClientHttpConnectorBuilder.class);
	}

	@Test
	void jettyReturnsJettyFactoryBuilder() {
		assertThat(ClientHttpConnectorBuilder.jetty()).isInstanceOf(JettyClientHttpConnectorBuilder.class);
	}

	@Test
	void httpComponentsReturnsHttpComponentsFactoryBuilder() {
		assertThat(ClientHttpConnectorBuilder.httpComponents())
			.isInstanceOf(HttpComponentsClientHttpConnectorBuilder.class);
	}

	@Test
	void jdkReturnsJdkFactoryBuilder() {
		assertThat(ClientHttpConnectorBuilder.jdk()).isInstanceOf(JdkClientHttpConnectorBuilder.class);
	}

	@Test
	void ofWhenExactlyClientHttpRequestFactoryTypeThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ClientHttpConnectorBuilder.of(ClientHttpConnector.class))
			.withMessage("'clientHttpConnectorType' must be an implementation of ClientHttpConnector");
	}

	@Test
	void ofWhenReactorFactoryReturnsReactorFactoryBuilder() {
		assertThat(ClientHttpConnectorBuilder.of(ReactorClientHttpConnector.class))
			.isInstanceOf(ReactorClientHttpConnectorBuilder.class);
	}

	@Test
	void ofWhenJettyFactoryReturnsReactorFactoryBuilder() {
		assertThat(ClientHttpConnectorBuilder.of(JettyClientHttpConnector.class))
			.isInstanceOf(JettyClientHttpConnectorBuilder.class);
	}

	@Test
	void ofWhenHttpComponentsFactoryReturnsHttpComponentsFactoryBuilder() {
		assertThat(ClientHttpConnectorBuilder.of(HttpComponentsClientHttpConnector.class))
			.isInstanceOf(HttpComponentsClientHttpConnectorBuilder.class);
	}

	@Test
	void ofWhenJdkFactoryReturnsJdkFactoryBuilder() {
		assertThat(ClientHttpConnectorBuilder.of(JdkClientHttpConnector.class))
			.isInstanceOf(JdkClientHttpConnectorBuilder.class);
	}

	@Test
	void ofWhenUnknownTypeThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ClientHttpConnectorBuilder.of(TestClientHttpConnector.class))
			.withMessage("'clientHttpConnectorType' " + TestClientHttpConnector.class.getName() + " is not supported");
	}

	@Test
	void detectWhenReactor() {
		assertThat(ClientHttpConnectorBuilder.detect()).isInstanceOf(ReactorClientHttpConnectorBuilder.class);
	}

	@Test
	@ClassPathExclusions({ "reactor-netty-http-*.jar" })
	void detectWhenJetty() {
		assertThat(ClientHttpConnectorBuilder.detect()).isInstanceOf(JettyClientHttpConnectorBuilder.class);
	}

	@Test
	@ClassPathExclusions({ "reactor-netty-http-*.jar", "jetty-client-*.jar" })
	void detectWhenHttpComponents() {
		assertThat(ClientHttpConnectorBuilder.detect()).isInstanceOf(HttpComponentsClientHttpConnectorBuilder.class);
	}

	@Test
	@ClassPathExclusions({ "reactor-netty-http-*.jar", "jetty-client-*.jar", "httpclient5-*.jar" })
	void detectWhenJdk() {
		assertThat(ClientHttpConnectorBuilder.detect()).isInstanceOf(JdkClientHttpConnectorBuilder.class);
	}

	private void setJdkReadTimeout(JdkClientHttpConnector factory) {
		factory.setReadTimeout(Duration.ofSeconds(5));
	}

	public static class TestClientHttpConnector implements ClientHttpConnector {

		@Override
		public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
				Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {
			return null;
		}

	}

}
