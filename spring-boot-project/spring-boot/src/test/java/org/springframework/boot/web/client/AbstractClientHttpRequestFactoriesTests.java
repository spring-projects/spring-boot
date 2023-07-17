/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.web.client;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.net.ssl.SSLHandshakeException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Base classes for testing of {@link ClientHttpRequestFactories} with different HTTP
 * clients on the classpath.
 *
 * @param <T> the {@link ClientHttpRequestFactory} to be produced
 * @author Andy Wilkinson
 */
@DirtiesUrlFactories
abstract class AbstractClientHttpRequestFactoriesTests<T extends ClientHttpRequestFactory> {

	private final Class<T> requestFactoryType;

	protected AbstractClientHttpRequestFactoriesTests(Class<T> requestFactoryType) {
		this.requestFactoryType = requestFactoryType;
	}

	@Test
	void getReturnsRequestFactoryOfExpectedType() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
			.get(ClientHttpRequestFactorySettings.DEFAULTS);
		assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
	}

	@Test
	void getOfGeneralTypeReturnsRequestFactoryOfExpectedType() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(ClientHttpRequestFactory.class,
				ClientHttpRequestFactorySettings.DEFAULTS);
		assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
	}

	@Test
	void getOfSpecificTypeReturnsRequestFactoryOfExpectedType() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(this.requestFactoryType,
				ClientHttpRequestFactorySettings.DEFAULTS);
		assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getReturnsRequestFactoryWithConfiguredConnectTimeout() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
			.get(ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofSeconds(60)));
		assertThat(connectTimeout((T) requestFactory)).isEqualTo(Duration.ofSeconds(60).toMillis());
	}

	@Test
	@SuppressWarnings("unchecked")
	void getReturnsRequestFactoryWithConfiguredReadTimeout() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
			.get(ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofSeconds(120)));
		assertThat(readTimeout((T) requestFactory)).isEqualTo(Duration.ofSeconds(120).toMillis());
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST" })
	void connectWithSslBundle(String httpMethod) throws Exception {
		TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
		Ssl ssl = new Ssl();
		ssl.setClientAuth(ClientAuth.NEED);
		ssl.setKeyPassword("password");
		ssl.setKeyStore("classpath:test.jks");
		ssl.setTrustStore("classpath:test.jks");
		webServerFactory.setSsl(ssl);
		WebServer webServer = webServerFactory
			.getWebServer((context) -> context.addServlet("test", TestServlet.class).addMapping("/"));
		try {
			webServer.start();
			int port = webServer.getPort();
			URI uri = new URI("https://localhost:%s".formatted(port));
			ClientHttpRequestFactory insecureRequestFactory = ClientHttpRequestFactories
				.get(ClientHttpRequestFactorySettings.DEFAULTS);
			ClientHttpRequest insecureRequest = insecureRequestFactory.createRequest(uri, HttpMethod.GET);
			assertThatExceptionOfType(SSLHandshakeException.class)
				.isThrownBy(() -> insecureRequest.execute().getBody());
			JksSslStoreDetails storeDetails = JksSslStoreDetails.forLocation("classpath:test.jks");
			JksSslStoreBundle stores = new JksSslStoreBundle(storeDetails, storeDetails);
			SslBundle sslBundle = SslBundle.of(stores, SslBundleKey.of("password"));
			ClientHttpRequestFactory secureRequestFactory = ClientHttpRequestFactories
				.get(ClientHttpRequestFactorySettings.DEFAULTS.withSslBundle(sslBundle));
			ClientHttpRequest secureRequest = secureRequestFactory.createRequest(uri, HttpMethod.valueOf(httpMethod));
			String secureResponse = StreamUtils.copyToString(secureRequest.execute().getBody(), StandardCharsets.UTF_8);
			assertThat(secureResponse).contains("Received " + httpMethod + " request to /");
		}
		finally {
			webServer.stop();
		}
	}

	protected abstract long connectTimeout(T requestFactory);

	protected abstract long readTimeout(T requestFactory);

	public static class TestServlet extends HttpServlet {

		@Override
		public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
			res.getWriter().println("Received " + req.getMethod() + " request to " + req.getRequestURI());
		}

	}

}
