/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import javax.net.ssl.SSLHandshakeException;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.ExampleServlet;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Test for {@link SkipSslVerificationHttpRequestFactory}.
 */
public class SkipSslVerificationHttpRequestFactoryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private EmbeddedServletContainer container;

	@After
	public void shutdownContainer() {
		if (this.container != null) {
			this.container.stop();
		}
	}

	@Test
	public void restCallToSelfSignedServershouldNotThrowSslException() throws Exception {
		String httpsUrl = getHttpsUrl();
		SkipSslVerificationHttpRequestFactory requestFactory = new SkipSslVerificationHttpRequestFactory();
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		ResponseEntity<String> responseEntity = restTemplate.getForEntity(httpsUrl,
				String.class);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		this.thrown.expect(ResourceAccessException.class);
		this.thrown.expectCause(isSSLHandshakeException());
		RestTemplate otherRestTemplate = new RestTemplate();
		otherRestTemplate.getForEntity(httpsUrl, String.class);
	}

	private Matcher<Throwable> isSSLHandshakeException() {
		return instanceOf(SSLHandshakeException.class);
	}

	private String getHttpsUrl() {
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory(
				0);
		factory.setSsl(getSsl("password", "classpath:test.jks"));
		this.container = factory.getEmbeddedServletContainer(
				new ServletRegistrationBean(new ExampleServlet(), "/hello"));
		this.container.start();
		return "https://localhost:" + this.container.getPort() + "/hello";
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
