/*
 * Copyright 2012-present the original author or authors.
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
