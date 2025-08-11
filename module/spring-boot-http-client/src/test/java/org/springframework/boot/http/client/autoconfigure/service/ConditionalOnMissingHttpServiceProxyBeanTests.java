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

package org.springframework.boot.http.client.autoconfigure.service;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.ImportHttpServices;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for
 * {@link ConditionalOnMissingHttpServiceProxyBean @ConditionalOnMissingHttpServiceProxyBean}.
 *
 * @author Phillip Webb
 */
class ConditionalOnMissingHttpServiceProxyBeanTests {

	@Test
	void attributeNameMatchesSpringFramework() {
		assertThat(OnMissingHttpServiceProxyBeanCondition.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE).isEqualTo(
				ReflectionTestUtils.getField(AbstractHttpServiceRegistrar.class, "HTTP_SERVICE_GROUP_NAME_ATTRIBUTE"));
	}

	@Test
	void getOutcomeWhenNoHttpServiceProxyMatches() {
		new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).hasBean("test"));
	}

	@Test
	void getOutcomeWhenHasHttpServiceProxyDoesNotMatch() {
		new ApplicationContextRunner()
			.withUserConfiguration(HttpServiceProxyConfiguration.class, TestConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(TestHttpService.class).doesNotHaveBean("test"));
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(TestHttpService.class)
	static class HttpServiceProxyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		@ConditionalOnMissingHttpServiceProxyBean
		String test() {
			return "test";
		}

	}

	interface TestHttpService {

		@GetExchange("/test")
		String test();

	}

}
