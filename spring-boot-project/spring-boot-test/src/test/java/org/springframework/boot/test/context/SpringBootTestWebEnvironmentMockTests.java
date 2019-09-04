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

import javax.servlet.ServletContext;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} configured with
 * {@link WebEnvironment#MOCK}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SpringBootTest("value=123")
@DirtiesContext
class SpringBootTestWebEnvironmentMockTests {

	@Value("${value}")
	private int value = 0;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ServletContext servletContext;

	@Test
	void annotationAttributesOverridePropertiesFile() {
		assertThat(this.value).isEqualTo(123);
	}

	@Test
	void validateWebApplicationContextIsSet() {
		WebApplicationContext fromServletContext = WebApplicationContextUtils
				.getWebApplicationContext(this.servletContext);
		assertThat(fromServletContext).isSameAs(this.context);
	}

	@Test
	void setsRequestContextHolder() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		assertThat(attributes).isNotNull();
	}

	@Test
	void resourcePath() {
		assertThat(this.servletContext).hasFieldOrPropertyWithValue("resourceBasePath", "src/main/webapp");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class Config {

		@Bean
		static PropertySourcesPlaceholderConfigurer propertyPlaceholder() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

}
