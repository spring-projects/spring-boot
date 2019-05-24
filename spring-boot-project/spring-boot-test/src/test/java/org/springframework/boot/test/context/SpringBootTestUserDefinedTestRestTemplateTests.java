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

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} configured with a user-defined
 * {@link RestTemplate} that is named {@code testRestTemplate}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "value=123" })
class SpringBootTestUserDefinedTestRestTemplateTests extends AbstractSpringBootTestWebServerWebEnvironmentTests {

	@Test
	void restTemplateIsUserDefined() {
		assertThat(getContext().getBean("testRestTemplate")).isInstanceOf(RestTemplate.class);
	}

	// gh-7711

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	@RestController
	protected static class Config extends AbstractConfig {

		@Bean
		public RestTemplate testRestTemplate() {
			return new RestTemplate();
		}

	}

}
