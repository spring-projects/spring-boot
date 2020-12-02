/*
 * Copyright 2012-2020 the original author or authors.
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

package io.spring.concourse.releasescripts.sdkman;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.Base64Utils;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link SdkmanService}.
 *
 * @author Madhura Bhave
 */
@EnableConfigurationProperties(SdkmanProperties.class)
@RestClientTest(SdkmanService.class)
class SdkmanServiceTests {

	@Autowired
	private SdkmanService service;

	@Autowired
	private SdkmanProperties properties;

	@Autowired
	private MockRestServiceServer server;

	@AfterEach
	void tearDown() {
		this.server.reset();
	}

	@Test
	void publishWhenMakeDefaultTrue() throws Exception {
		setupExpectation("https://vendors.sdkman.io/release",
				"{\"candidate\": \"springboot\", \"version\": \"1.2.3\", \"url\": \"https://repo.spring.io/simple/libs-release-local/org/springframework/boot/spring-boot-cli/1.2.3/spring-boot-cli-1.2.3-bin.zip\"}");
		setupExpectation("https://vendors.sdkman.io/default",
				"{\"candidate\": \"springboot\", \"version\": \"1.2.3\"}");
		setupExpectation("https://vendors.sdkman.io/announce/struct",
				"{\"candidate\": \"springboot\", \"version\": \"1.2.3\", \"hashtag\": \"springboot\"}");
		this.service.publish("1.2.3", true);
		this.server.verify();
	}

	@Test
	void publishWhenMakeDefaultFalse() throws Exception {
		setupExpectation("https://vendors.sdkman.io/release",
				"{\"candidate\": \"springboot\", \"version\": \"1.2.3\", \"url\": \"https://repo.spring.io/simple/libs-release-local/org/springframework/boot/spring-boot-cli/1.2.3/spring-boot-cli-1.2.3-bin.zip\"}");
		setupExpectation("https://vendors.sdkman.io/announce/struct",
				"{\"candidate\": \"springboot\", \"version\": \"1.2.3\", \"hashtag\": \"springboot\"}");
		this.service.publish("1.2.3", false);
		this.server.verify();
	}

	private void setupExpectation(String url, String body) {
		this.server.expect(requestTo(url)).andExpect(method(HttpMethod.POST)).andExpect(content().json(body))
				.andExpect(header("Authorization",
						"Basic " + Base64Utils.encodeToString(String
								.format("%s:%s", this.properties.getConsumerKey(), this.properties.getConsumerToken())
								.getBytes())))
				.andExpect(header("Content-Type", MediaType.APPLICATION_JSON.toString())).andRespond(withSuccess());
	}

}
