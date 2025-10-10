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

package org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.servlet;

import javax.net.ssl.SSLHandshakeException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.testsupport.web.servlet.ExampleServlet;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test for {@link SkipSslVerificationHttpRequestFactory}.
 */
class SkipSslVerificationHttpRequestFactoryTests {

	private @Nullable WebServer webServer;

	@AfterEach
	void shutdownContainer() {
		if (this.webServer != null) {
			this.webServer.stop();
		}
	}

	@Test
	@WithPackageResources("test.jks")
	void restCallToSelfSignedServerShouldNotThrowSslException() {
		String httpsUrl = getHttpsUrl();
		SkipSslVerificationHttpRequestFactory requestFactory = new SkipSslVerificationHttpRequestFactory();
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		RestTemplate otherRestTemplate = new RestTemplate();
		ResponseEntity<String> responseEntity = restTemplate.getForEntity(httpsUrl, String.class);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThatExceptionOfType(ResourceAccessException.class)
			.isThrownBy(() -> otherRestTemplate.getForEntity(httpsUrl, String.class))
			.withCauseInstanceOf(SSLHandshakeException.class);
	}

	private String getHttpsUrl() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		factory.setSsl(getSsl("password", "classpath:test.jks"));
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(), "/hello"));
		this.webServer.start();
		return "https://localhost:" + this.webServer.getPort() + "/hello";
	}

	private Ssl getSsl(String keyPassword, String keyStore) {
		Ssl ssl = new Ssl();
		ssl.setEnabled(true);
		ssl.setKeyPassword(keyPassword);
		ssl.setKeyStore(keyStore);
		ssl.setKeyStorePassword("secret");
		return ssl;
	}

}
