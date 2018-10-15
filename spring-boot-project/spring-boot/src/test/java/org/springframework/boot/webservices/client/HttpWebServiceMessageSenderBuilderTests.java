/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.webservices.client;

import java.time.Duration;

import org.apache.http.client.config.RequestConfig;
import org.junit.Test;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpWebServiceMessageSenderBuilder}.
 *
 * @author Stephane Nicoll
 */
public class HttpWebServiceMessageSenderBuilderTests {

	@Test
	public void buildWithRequestFactorySupplier() {
		ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
		ClientHttpRequestMessageSender messageSender = build(
				new HttpWebServiceMessageSenderBuilder()
						.requestFactory(() -> requestFactory));
		assertThat(messageSender.getRequestFactory()).isSameAs(requestFactory);
	}

	@Test
	public void buildWithReadAndConnectTimeout() {
		ClientHttpRequestMessageSender messageSender = build(
				new HttpWebServiceMessageSenderBuilder()
						.requestFactory(SimpleClientHttpRequestFactory::new)
						.setConnectTimeout(Duration.ofSeconds(5))
						.setReadTimeout(Duration.ofSeconds(2)));
		SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) messageSender
				.getRequestFactory();
		assertThat(requestFactory).hasFieldOrPropertyWithValue("connectTimeout", 5000);
		assertThat(requestFactory).hasFieldOrPropertyWithValue("readTimeout", 2000);
	}

	@Test
	public void buildUsesHttpComponentsByDefault() {
		ClientHttpRequestMessageSender messageSender = build(
				new HttpWebServiceMessageSenderBuilder()
						.setConnectTimeout(Duration.ofSeconds(5))
						.setReadTimeout(Duration.ofSeconds(2)));
		ClientHttpRequestFactory requestFactory = messageSender.getRequestFactory();
		assertThat(requestFactory)
				.isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
		RequestConfig requestConfig = (RequestConfig) ReflectionTestUtils
				.getField(requestFactory, "requestConfig");
		assertThat(requestConfig).isNotNull();
		assertThat(requestConfig.getConnectTimeout()).isEqualTo(5000);
		assertThat(requestConfig.getSocketTimeout()).isEqualTo(2000);
	}

	private ClientHttpRequestMessageSender build(
			HttpWebServiceMessageSenderBuilder builder) {
		WebServiceMessageSender messageSender = builder.build();
		assertThat(messageSender).isInstanceOf(ClientHttpRequestMessageSender.class);
		return ((ClientHttpRequestMessageSender) messageSender);
	}

}
