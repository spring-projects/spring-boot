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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestClientTest @RestClientTest} without Jackson.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("jackson-*.jar")
@RestClientTest(ExampleRestClient.class)
class RestClientTestWithoutJacksonIntegrationTests {

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ExampleRestClient client;

	@Test
	void restClientTestCanBeUsedWhenJacksonIsNotOnTheClassPath() {
		ClassLoader classLoader = getClass().getClassLoader();
		assertThat(ClassUtils.isPresent("com.fasterxml.jackson.databind.Module", classLoader)).isFalse();
		this.server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		assertThat(this.client.test()).isEqualTo("hello");
	}

}
