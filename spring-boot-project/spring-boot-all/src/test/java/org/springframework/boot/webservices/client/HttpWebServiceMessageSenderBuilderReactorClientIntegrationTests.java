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

import io.netty.channel.ChannelOption;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpWebServiceMessageSenderBuilder} when Reactor Netty is the
 * predominant HTTP client.
 *
 * @author Andy Wilkinson
 * @deprecated since 3.4.0 for removal in 3.6.0
 */
@ClassPathExclusions({ "httpclient5-*.jar", "jetty-client-*.jar" })
@SuppressWarnings("removal")
@Deprecated(since = "3.4.0", forRemoval = true)
class HttpWebServiceMessageSenderBuilderReactorClientIntegrationTests {

	private final HttpWebServiceMessageSenderBuilder builder = new HttpWebServiceMessageSenderBuilder();

	@Test
	void buildUsesReactorClientIfHttpComponentsAndJettyAreNotAvailable() {
		WebServiceMessageSender messageSender = this.builder.build();
		assertReactorClientHttpRequestFactory(messageSender);
	}

	@Test
	void buildWithCustomTimeouts() {
		WebServiceMessageSender messageSender = this.builder.setConnectTimeout(Duration.ofSeconds(5))
			.setReadTimeout(Duration.ofSeconds(2))
			.build();
		ReactorClientHttpRequestFactory factory = assertReactorClientHttpRequestFactory(messageSender);
		assertThat(factory).extracting("httpClient", InstanceOfAssertFactories.type(HttpClient.class))
			.extracting((httpClient) -> httpClient.configuration().options(), InstanceOfAssertFactories.MAP)
			.containsEntry(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
		assertThat(factory).hasFieldOrPropertyWithValue("readTimeout", Duration.ofSeconds(2));
	}

	private ReactorClientHttpRequestFactory assertReactorClientHttpRequestFactory(
			WebServiceMessageSender messageSender) {
		assertThat(messageSender).isInstanceOf(ClientHttpRequestMessageSender.class);
		ClientHttpRequestMessageSender sender = (ClientHttpRequestMessageSender) messageSender;
		ClientHttpRequestFactory requestFactory = sender.getRequestFactory();
		assertThat(requestFactory).isInstanceOf(ReactorClientHttpRequestFactory.class);
		return (ReactorClientHttpRequestFactory) requestFactory;
	}

}
