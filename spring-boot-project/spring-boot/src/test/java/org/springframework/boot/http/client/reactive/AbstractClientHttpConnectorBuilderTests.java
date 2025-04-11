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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Set;
import java.util.function.Function;

import javax.net.ssl.SSLHandshakeException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunctions;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Base class for {@link ClientHttpConnectorBuilder} tests.
 *
 * @param <T> The {@link ClientHttpConnector} type
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@DirtiesUrlFactories
abstract class AbstractClientHttpConnectorBuilderTests<T extends ClientHttpConnector> {

	private static final Function<HttpMethod, HttpStatus> ALWAYS_FOUND = (method) -> HttpStatus.FOUND;

	private final Class<T> connectorType;

	private final ClientHttpConnectorBuilder<T> builder;

	AbstractClientHttpConnectorBuilderTests(Class<T> connectorType, ClientHttpConnectorBuilder<T> builder) {
		this.connectorType = connectorType;
		this.builder = builder;
	}

	@Test
	void buildReturnsConnectorOfExpectedType() {
		T connector = this.builder.build();
		assertThat(connector).isInstanceOf(this.connectorType);
	}

	@Test
	void buildWhenHasConnectTimeout() {
		ClientHttpConnectorSettings settings = ClientHttpConnectorSettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(60));
		T connector = this.builder.build(settings);
		assertThat(connectTimeout(connector)).isEqualTo(Duration.ofSeconds(60).toMillis());
	}

	@Test
	void buildWhenHadReadTimeout() {
		ClientHttpConnectorSettings settings = ClientHttpConnectorSettings.defaults()
			.withReadTimeout(Duration.ofSeconds(120));
		T connector = this.builder.build(settings);
		assertThat(readTimeout(connector)).isEqualTo(Duration.ofSeconds(120).toMillis());
	}

	@ParameterizedTest
	@WithPackageResources("test.jks")
	@ValueSource(strings = { "GET", "POST" })
	void connectWithSslBundle(String httpMethod) throws Exception {
		TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
		webServerFactory.setSsl(ssl());
		WebServer webServer = webServerFactory
			.getWebServer((context) -> context.addServlet("test", TestServlet.class).addMapping("/"));
		try {
			webServer.start();
			int port = webServer.getPort();
			URI uri = new URI("https://localhost:%s".formatted(port));
			ClientHttpConnector insecureConnector = this.builder.build();
			ClientRequest insecureRequest = createRequest(httpMethod, uri);
			assertThatExceptionOfType(WebClientRequestException.class)
				.isThrownBy(() -> getResponse(insecureConnector, insecureRequest))
				.withCauseInstanceOf(SSLHandshakeException.class);
			ClientHttpConnector secureConnector = this.builder
				.build(ClientHttpConnectorSettings.ofSslBundle(sslBundle()));
			ClientRequest secureRequest = createRequest(httpMethod, uri);
			ClientResponse secureResponse = getResponse(secureConnector, secureRequest);
			assertThat(secureResponse.bodyToMono(String.class).block())
				.contains("Received " + httpMethod + " request to /");
		}
		finally {
			webServer.stop();
		}
	}

	@ParameterizedTest
	@WithPackageResources("test.jks")
	@ValueSource(strings = { "GET", "POST" })
	void connectWithSslBundleAndOptionsMismatch(String httpMethod) throws Exception {
		TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
		webServerFactory.setSsl(ssl("TLS_AES_128_GCM_SHA256"));
		WebServer webServer = webServerFactory
			.getWebServer((context) -> context.addServlet("test", TestServlet.class).addMapping("/"));
		try {
			webServer.start();
			int port = webServer.getPort();
			URI uri = new URI("https://localhost:%s".formatted(port));
			ClientHttpConnector secureConnector = this.builder.build(ClientHttpConnectorSettings
				.ofSslBundle(sslBundle(SslOptions.of(Set.of("TLS_AES_256_GCM_SHA384"), null))));
			ClientRequest secureRequest = createRequest(httpMethod, uri);
			assertThatExceptionOfType(WebClientRequestException.class)
				.isThrownBy(() -> getResponse(secureConnector, secureRequest))
				.withCauseInstanceOf(SSLHandshakeException.class);
		}
		finally {
			webServer.stop();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
	void redirectDefault(String httpMethod) throws Exception {
		testRedirect(null, HttpMethod.valueOf(httpMethod), this::getExpectedRedirect);
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
	void redirectFollow(String httpMethod) throws Exception {
		ClientHttpConnectorSettings settings = ClientHttpConnectorSettings.defaults()
			.withRedirects(HttpRedirects.FOLLOW);
		testRedirect(settings, HttpMethod.valueOf(httpMethod), this::getExpectedRedirect);
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
	void redirectDontFollow(String httpMethod) throws Exception {
		ClientHttpConnectorSettings settings = ClientHttpConnectorSettings.defaults()
			.withRedirects(HttpRedirects.DONT_FOLLOW);
		testRedirect(settings, HttpMethod.valueOf(httpMethod), ALWAYS_FOUND);
	}

	protected final void testRedirect(ClientHttpConnectorSettings settings, HttpMethod httpMethod,
			Function<HttpMethod, HttpStatus> expectedStatusForMethod) throws URISyntaxException {
		HttpStatus expectedStatus = expectedStatusForMethod.apply(httpMethod);
		TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
		WebServer webServer = webServerFactory
			.getWebServer((context) -> context.addServlet("test", TestServlet.class).addMapping("/"));
		try {
			webServer.start();
			int port = webServer.getPort();
			URI uri = new URI("http://localhost:%s".formatted(port) + "/redirect");
			ClientHttpConnector connector = this.builder.build(settings);
			ClientRequest request = createRequest(httpMethod, uri);
			ClientResponse response = getResponse(connector, request);
			assertThat(response.statusCode()).isEqualTo(expectedStatus);
			if (expectedStatus == HttpStatus.OK) {
				assertThat(response.bodyToMono(String.class).block()).contains("request to /redirected");
			}
		}
		finally {
			webServer.stop();
		}
	}

	private ClientRequest createRequest(String httpMethod, URI uri) {
		return createRequest(HttpMethod.valueOf(httpMethod), uri);
	}

	private ClientRequest createRequest(HttpMethod httpMethod, URI uri) {
		return ClientRequest.create(httpMethod, uri).build();
	}

	private ClientResponse getResponse(ClientHttpConnector connector, ClientRequest request) {
		return ExchangeFunctions.create(connector).exchange(request).block();
	}

	private Ssl ssl(String... ciphers) {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(ClientAuth.NEED);
		ssl.setKeyPassword("password");
		ssl.setKeyStore("classpath:test.jks");
		ssl.setTrustStore("classpath:test.jks");
		if (ciphers.length > 0) {
			ssl.setCiphers(ciphers);
		}
		return ssl;
	}

	protected final SslBundle sslBundle() {
		return sslBundle(SslOptions.NONE);
	}

	protected final SslBundle sslBundle(SslOptions sslOptions) {
		JksSslStoreDetails storeDetails = JksSslStoreDetails.forLocation("classpath:test.jks");
		JksSslStoreBundle stores = new JksSslStoreBundle(storeDetails, storeDetails);
		return SslBundle.of(stores, SslBundleKey.of("password"), sslOptions);
	}

	protected HttpStatus getExpectedRedirect(HttpMethod httpMethod) {
		return HttpStatus.OK;
	}

	protected abstract long connectTimeout(T connector);

	protected abstract long readTimeout(T connector);

	public static class TestServlet extends HttpServlet {

		@Override
		public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
			if ("/redirect".equals(req.getRequestURI())) {
				res.sendRedirect("/redirected");
				return;
			}
			res.getWriter().println("Received " + req.getMethod() + " request to " + req.getRequestURI());
		}

	}

}
