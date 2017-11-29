/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.reactive.server;

import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import reactor.core.publisher.Mono;

import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.SocketUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for testing classes that extends {@link AbstractReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 */
public abstract class AbstractReactiveWebServerFactoryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public OutputCapture output = new OutputCapture();

	protected WebServer webServer;

	@After
	public void tearDown() {
		if (this.webServer != null) {
			try {
				this.webServer.stop();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
	}

	protected abstract AbstractReactiveWebServerFactory getFactory();

	@Test
	public void specificPort() throws Exception {
		AbstractReactiveWebServerFactory factory = getFactory();
		int specificPort = SocketUtils.findAvailableTcpPort(41000);
		factory.setPort(specificPort);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		Mono<String> result = getWebClient().build().post().uri("/test")
				.contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromObject("Hello World")).exchange()
				.flatMap((response) -> response.bodyToMono(String.class));
		assertThat(result.block()).isEqualTo("Hello World");
		assertThat(this.webServer.getPort()).isEqualTo(specificPort);
	}

	@Test
	public void basicSslFromClassPath() throws Exception {
		testBasicSslWithKeyStore("classpath:test.jks");
	}

	@Test
	public void basicSslFromFileSystem() throws Exception {
		testBasicSslWithKeyStore("src/test/resources/test.jks");
	}

	protected final void testBasicSslWithKeyStore(String keyStore) throws Exception {
		AbstractReactiveWebServerFactory factory = getFactory();
		Ssl ssl = new Ssl();
		ssl.setKeyStore(keyStore);
		ssl.setKeyPassword("password");
		factory.setSsl(ssl);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		ReactorClientHttpConnector connector = buildTrustAllSslConnector();
		WebClient client = WebClient.builder()
				.baseUrl("https://localhost:" + this.webServer.getPort())
				.clientConnector(connector).build();
		Mono<String> result = client.post().uri("/test").contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromObject("Hello World")).exchange()
				.flatMap((response) -> response.bodyToMono(String.class));
		assertThat(result.block()).isEqualTo("Hello World");
	}

	protected ReactorClientHttpConnector buildTrustAllSslConnector() {
		return new ReactorClientHttpConnector(
				(options) -> options.sslSupport(sslContextBuilder -> {
					sslContextBuilder.sslProvider(SslProvider.JDK)
							.trustManager(InsecureTrustManagerFactory.INSTANCE);
				}));
	}

	protected WebClient.Builder getWebClient() {
		return WebClient.builder()
				.baseUrl("http://localhost:" + this.webServer.getPort());
	}

	protected static class EchoHandler implements HttpHandler {

		public EchoHandler() {

		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			response.setStatusCode(HttpStatus.OK);
			return response.writeWith(request.getBody());
		}

	}

}
