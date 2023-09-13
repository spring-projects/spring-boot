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

package org.springframework.boot.webservices.client;

import java.time.Duration;

import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpWebServiceMessageSenderBuilder} when Http Components is not
 * available and, therefore, Jetty's client is used instead.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("httpclient5-*.jar")
class HttpWebServiceMessageSenderBuilderJettyClientIntegrationTests {

	private final HttpWebServiceMessageSenderBuilder builder = new HttpWebServiceMessageSenderBuilder();

	@Test
	void buildUseJettyClientIfHttpComponentsIsNotAvailable() {
		WebServiceMessageSender messageSender = this.builder.build();
		assertJettyClientHttpRequestFactory(messageSender);
	}

	@Test
	void buildWithCustomTimeouts() {
		WebServiceMessageSender messageSender = this.builder.setConnectTimeout(Duration.ofSeconds(5))
			.setReadTimeout(Duration.ofSeconds(2))
			.build();
		JettyClientHttpRequestFactory factory = assertJettyClientHttpRequestFactory(messageSender);
		HttpClient client = (HttpClient) ReflectionTestUtils.getField(factory, "httpClient");
		assertThat(client).isNotNull();
		assertThat(client.getConnectTimeout()).isEqualTo(5000);
		assertThat(factory).hasFieldOrPropertyWithValue("readTimeout", 2000L);
	}

	private JettyClientHttpRequestFactory assertJettyClientHttpRequestFactory(WebServiceMessageSender messageSender) {
		assertThat(messageSender).isInstanceOf(ClientHttpRequestMessageSender.class);
		ClientHttpRequestMessageSender sender = (ClientHttpRequestMessageSender) messageSender;
		ClientHttpRequestFactory requestFactory = sender.getRequestFactory();
		assertThat(requestFactory).isInstanceOf(JettyClientHttpRequestFactory.class);
		return (JettyClientHttpRequestFactory) requestFactory;
	}

}
