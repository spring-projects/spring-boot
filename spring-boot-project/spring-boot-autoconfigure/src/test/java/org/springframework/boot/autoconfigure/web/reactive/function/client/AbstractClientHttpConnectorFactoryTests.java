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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.server.WebServer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Abstract base class for {@link ClientHttpConnectorFactory} tests.
 *
 * @author Phillip Webb
 */
abstract class AbstractClientHttpConnectorFactoryTests {

	@Test
	void insecureConnection() {
		TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
		WebServer webServer = webServerFactory.getWebServer();
		try {
			webServer.start();
			int port = webServer.getPort();
			String url = "http://localhost:%s".formatted(port);
			WebClient insecureWebClient = WebClient.builder()
				.clientConnector(getFactory().createClientHttpConnector())
				.build();
			String insecureBody = insecureWebClient.get()
				.uri(url)
				.exchangeToMono((response) -> response.bodyToMono(String.class))
				.block();
			assertThat(insecureBody).contains("HTTP Status 404 – Not Found");
		}
		finally {
			webServer.stop();
		}
	}

	@Test
	void secureConnection() throws Exception {
		TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
		Ssl ssl = new Ssl();
		ssl.setClientAuth(ClientAuth.NEED);
		ssl.setKeyPassword("password");
		ssl.setKeyStore("classpath:test.jks");
		ssl.setTrustStore("classpath:test.jks");
		webServerFactory.setSsl(ssl);
		WebServer webServer = webServerFactory.getWebServer();
		try {
			webServer.start();
			int port = webServer.getPort();
			String url = "https://localhost:%s".formatted(port);
			WebClient insecureWebClient = WebClient.builder()
				.clientConnector(getFactory().createClientHttpConnector())
				.build();
			assertThatExceptionOfType(WebClientRequestException.class).isThrownBy(() -> insecureWebClient.get()
				.uri(url)
				.exchangeToMono((response) -> response.bodyToMono(String.class))
				.block());
			JksSslStoreDetails storeDetails = JksSslStoreDetails.forLocation("classpath:test.jks");
			JksSslStoreBundle stores = new JksSslStoreBundle(storeDetails, storeDetails);
			SslBundle sslBundle = SslBundle.of(stores, SslBundleKey.of("password"));
			WebClient secureWebClient = WebClient.builder()
				.clientConnector(getFactory().createClientHttpConnector(sslBundle))
				.build();
			String secureBody = secureWebClient.get()
				.uri(url)
				.exchangeToMono((response) -> response.bodyToMono(String.class))
				.block();
			assertThat(secureBody).contains("HTTP Status 404 – Not Found");
		}
		finally {
			webServer.stop();
		}
	}

	protected abstract ClientHttpConnectorFactory<?> getFactory();

}
