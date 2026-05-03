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

package org.springframework.boot.webclient.test.autoconfigure;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link WebClientTest @WebClientTest} with no specific client.
 *
 * @author Phillip Webb
 */
@WebClientTest
@Import(MockWebServerConfiguration.class)
class WebClientTestNoComponentIntegrationTests {

	@Autowired
	private MockWebServer server;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Test
	void exampleWebClientServiceIsNotInjected() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleWebClientService.class));
	}

	@Test
	void examplePropertiesIsNotInjected() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleProperties.class));
	}

	@Test
	void manuallyCreateBean() throws InterruptedException {
		ExampleWebClientService client = new ExampleWebClientService(this.webClientBuilder);
		this.server.enqueue(new MockResponse().setBody("hello"));
		assertThat(client.test()).isEqualTo("hello");
		this.server.takeRequest();
	}

}
