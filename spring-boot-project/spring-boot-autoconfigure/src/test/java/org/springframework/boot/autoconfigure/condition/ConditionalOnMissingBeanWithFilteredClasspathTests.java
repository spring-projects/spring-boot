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

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ConditionalOnMissingBean @ConditionalOnMissingBean} with filtered
 * classpath.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@ClassPathExclusions("spring-context-support-*.jar")
class ConditionalOnMissingBeanWithFilteredClasspathTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(OnBeanTypeConfiguration.class);

	@Test
	void testNameOnMissingBeanTypeWithMissingImport() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Configuration(proxyBeanMethods = false)
	static class OnBeanTypeConfiguration {

		@Bean
		@ConditionalOnMissingBean(
				type = "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBeanWithFilteredClasspathTests.TestCacheManager")
		String foo() {
			return "foo";
		}

	}

	static class TestCacheManager extends CaffeineCacheManager {

	}

}
