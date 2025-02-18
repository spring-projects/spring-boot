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

package org.springframework.boot.webservices.client;

import java.time.Duration;

import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link WebServiceMessageSenderFactory}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class WebServiceMessageSenderFactoryTests {

	@Test
	void httpWhenDetectedHttpComponents() {
		WebServiceMessageSender sender = WebServiceMessageSenderFactory.http().getWebServiceMessageSender();
		assertRequestFactoryInstanceOf(sender, HttpComponentsClientHttpRequestFactory.class);
	}

	@Test
	@ClassPathExclusions("httpclient5-*.jar")
	void httpWhenDetectedJetty() {
		WebServiceMessageSender sender = WebServiceMessageSenderFactory.http().getWebServiceMessageSender();
		assertRequestFactoryInstanceOf(sender, JettyClientHttpRequestFactory.class);
	}

	@Test
	@ClassPathExclusions({ "httpclient5-*.jar", "jetty-client-*.jar" })
	void httpWhenDetectedReactor() {
		WebServiceMessageSender sender = WebServiceMessageSenderFactory.http().getWebServiceMessageSender();
		assertRequestFactoryInstanceOf(sender, ReactorClientHttpRequestFactory.class);
	}

	@Test
	@ClassPathExclusions({ "httpclient5-*.jar", "jetty-client-*.jar", "reactor-netty-http-*.jar" })
	void httpWhenDetectedJdk() {
		WebServiceMessageSender sender = WebServiceMessageSenderFactory.http().getWebServiceMessageSender();
		assertRequestFactoryInstanceOf(sender, JdkClientHttpRequestFactory.class);
	}

	@Test
	@ClassPathExclusions("httpclient5-*.jar")
	void httpWithSettingsUsesSettings() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(5))
			.withReadTimeout(Duration.ofSeconds(2));
		WebServiceMessageSender sender = WebServiceMessageSenderFactory.http(settings).getWebServiceMessageSender();
		assertTimeoutsOnJetty(sender);
	}

	@Test
	void httpWithFactoryAndSettingsUsesFactoryAndSettings() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(5))
			.withReadTimeout(Duration.ofSeconds(2));
		WebServiceMessageSender sender = WebServiceMessageSenderFactory
			.http(ClientHttpRequestFactoryBuilder.jetty(), settings)
			.getWebServiceMessageSender();
		assertTimeoutsOnJetty(sender);
	}

	@Test
	void httpWithFactoryAndSettingsWhenFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> WebServiceMessageSenderFactory.http(null, null))
			.withMessage("'requestFactoryBuilder' must not be null");
	}

	private void assertTimeoutsOnJetty(WebServiceMessageSender sender) {
		ClientHttpRequestFactory requestFactory = getRequestFactory(sender);
		HttpClient client = (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");
		assertThat(client).isNotNull();
		assertThat(client.getConnectTimeout()).isEqualTo(5000);
		assertThat(requestFactory).hasFieldOrPropertyWithValue("readTimeout", 2000L);
	}

	private void assertRequestFactoryInstanceOf(WebServiceMessageSender sender, Class<?> expectedRequestFactoryType) {
		assertThat(getRequestFactory(sender)).isInstanceOf(expectedRequestFactoryType);
	}

	private ClientHttpRequestFactory getRequestFactory(WebServiceMessageSender sender) {
		assertThat(sender).isInstanceOf(ClientHttpRequestMessageSender.class);
		ClientHttpRequestFactory requestFactory = ((ClientHttpRequestMessageSender) sender).getRequestFactory();
		return requestFactory;
	}

}
