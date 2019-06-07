/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpWebServiceMessageSenderBuilder} when no preferred HTTP clients are
 * available
 *
 * @author Stephane Nicoll
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "httpclient-*.jar", "okhttp*.jar" })
public class HttpWebServiceMessageSenderBuilderSimpleIntegrationTests {

	private final HttpWebServiceMessageSenderBuilder builder = new HttpWebServiceMessageSenderBuilder();

	@Test
	public void buildUseUseSimpleClientByDefault() {
		WebServiceMessageSender messageSender = this.builder.build();
		assertSimpleClientRequestFactory(messageSender);
	}

	@Test
	public void buildWithCustomTimeouts() {
		WebServiceMessageSender messageSender = this.builder.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(2)).build();
		SimpleClientHttpRequestFactory requestFactory = assertSimpleClientRequestFactory(messageSender);
		assertThat(requestFactory).hasFieldOrPropertyWithValue("connectTimeout", 5000);
		assertThat(requestFactory).hasFieldOrPropertyWithValue("readTimeout", 2000);
	}

	private SimpleClientHttpRequestFactory assertSimpleClientRequestFactory(WebServiceMessageSender messageSender) {
		assertThat(messageSender).isInstanceOf(ClientHttpRequestMessageSender.class);
		ClientHttpRequestMessageSender sender = (ClientHttpRequestMessageSender) messageSender;
		ClientHttpRequestFactory requestFactory = sender.getRequestFactory();
		assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
		return (SimpleClientHttpRequestFactory) requestFactory;
	}

}
