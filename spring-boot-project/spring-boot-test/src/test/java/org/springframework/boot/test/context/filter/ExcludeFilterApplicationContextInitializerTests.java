/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.context.filter;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ExcludeFilterApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
class ExcludeFilterApplicationContextInitializerTests {

	@Test
	void testConfigurationIsExcluded() {
		SpringApplication application = new SpringApplication(TestApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		AssertableApplicationContext applicationContext = AssertableApplicationContext.get(() -> application.run());
		assertThat(applicationContext).hasSingleBean(TestApplication.class);
		assertThat(applicationContext).doesNotHaveBean(ExcludedTestConfiguration.class);
	}

	@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class) })
	static class TestApplication {

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ExcludedTestConfiguration {

	}

}
