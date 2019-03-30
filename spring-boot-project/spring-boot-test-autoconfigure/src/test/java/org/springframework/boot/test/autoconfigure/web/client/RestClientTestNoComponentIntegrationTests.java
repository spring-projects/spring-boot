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

package org.springframework.boot.test.autoconfigure.web.client;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestClientTest} with no specific client.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@RestClientTest
public class RestClientTestNoComponentIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

	@Autowired
	private MockRestServiceServer server;

	@Test
	public void exampleRestClientIsNotInjected() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(
				() -> this.applicationContext.getBean(ExampleRestClient.class));
	}

	@Test
	public void manuallyCreateBean() {
		ExampleRestClient client = new ExampleRestClient(this.restTemplateBuilder);
		this.server.expect(requestTo("/test"))
				.andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		assertThat(client.test()).isEqualTo("hello");
	}

}
