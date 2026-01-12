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

import java.nio.charset.StandardCharsets;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebClientTest @WebClientTest}.
 *
 * @author Scott Frederick
 */
@WebClientTest(ExampleWebClientService.class)
@Import(MockWebServerConfiguration.class)
class WebClientTestIntegrationTests {

	@Autowired
	private MockWebServer server;

	@Autowired
	private ExampleWebClientService client;

	@Test
	void mockServerCall1() throws InterruptedException {
		this.server.enqueue(new MockResponse().setBody("1"));
		assertThat(this.client.test()).isEqualTo("1");
		this.server.takeRequest();
	}

	@Test
	void mockServerCall2() throws InterruptedException {
		this.server.enqueue(new MockResponse().setBody("2"));
		assertThat(this.client.test()).isEqualTo("2");
		this.server.takeRequest();
	}

	@Test
	void mockServerCallWithContent() throws InterruptedException {
		this.server.enqueue(new MockResponse().setBody("1"));
		this.client.testPostWithBody("test");
		assertThat(this.server.takeRequest().getBody().readString(StandardCharsets.UTF_8)).isEqualTo("test");
	}

}
