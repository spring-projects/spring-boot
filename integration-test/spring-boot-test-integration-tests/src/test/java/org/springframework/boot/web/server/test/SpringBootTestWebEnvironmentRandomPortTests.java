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

package org.springframework.boot.web.server.test;

import org.junit.jupiter.api.Test;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} configured with
 * {@link WebEnvironment#RANDOM_PORT}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "value=123" })
class SpringBootTestWebEnvironmentRandomPortTests extends AbstractSpringBootTestWebServerWebEnvironmentTests {

	@Test
	void testRestTemplateShouldUseBuilder() {
		assertThat(getRestTemplate().getRestTemplate().getMessageConverters())
			.hasAtLeastOneElementOfType(MyConverter.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	@RestController
	static class Config extends AbstractConfig {

		@Bean
		RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder().additionalMessageConverters(new MyConverter());
		}

	}

	static class MyConverter extends StringHttpMessageConverter {

	}

}
