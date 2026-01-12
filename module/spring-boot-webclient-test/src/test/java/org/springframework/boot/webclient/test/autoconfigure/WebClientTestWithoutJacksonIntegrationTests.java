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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebClientTest @WebClientTest} without Jackson.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("jackson-*.jar")
@WebClientTest(ExampleWebClientService.class)
@Import(MockWebServerConfiguration.class)
class WebClientTestWithoutJacksonIntegrationTests {

	@Autowired
	private MockWebServer server;

	@Autowired
	private ExampleWebClientService client;

	@Test
	void webClientTestCanBeUsedWhenJacksonIsNotOnTheClassPath() throws InterruptedException {
		ClassLoader classLoader = getClass().getClassLoader();
		assertThat(ClassUtils.isPresent("com.fasterxml.jackson.databind.Module", classLoader)).isFalse();
		assertThat(ClassUtils.isPresent("tools.jackson.databind.JacksonModule", classLoader)).isFalse();
		this.server.enqueue(new MockResponse().setBody("hello"));
		assertThat(this.client.test()).isEqualTo("hello");
		this.server.takeRequest();
	}

}
