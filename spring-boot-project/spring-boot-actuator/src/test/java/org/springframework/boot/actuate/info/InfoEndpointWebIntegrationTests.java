/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.info;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointRunners;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link InfoEndpoint} exposed by Jersey, Spring MVC, and WebFlux.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@RunWith(WebEndpointRunners.class)
@TestPropertySource(properties = { "info.app.name=MyService" })
public class InfoEndpointWebIntegrationTests {

	private static WebTestClient client;

	@Test
	public void info() {
		client.get().uri("/actuator/info").accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isOk().expectBody().jsonPath("beanName1.key11")
				.isEqualTo("value11").jsonPath("beanName1.key12").isEqualTo("value12")
				.jsonPath("beanName2.key21").isEqualTo("value21")
				.jsonPath("beanName2.key22").isEqualTo("value22");
	}

	@Configuration
	public static class TestConfiguration {

		@Bean
		public InfoEndpoint endpoint() {
			return new InfoEndpoint(Arrays.asList(beanName1(), beanName2()));
		}

		@Bean
		public InfoContributor beanName1() {
			return (builder) -> {
				Map<String, Object> content = new LinkedHashMap<>();
				content.put("key11", "value11");
				content.put("key12", "value12");
				builder.withDetail("beanName1", content);
			};
		}

		@Bean
		public InfoContributor beanName2() {
			return (builder) -> {
				Map<String, Object> content = new LinkedHashMap<>();
				content.put("key21", "value21");
				content.put("key22", "value22");
				builder.withDetail("beanName2", content);
			};
		}

	}

}
