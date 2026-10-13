/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnCloudPlatform @ConditionalOnCloudPlatform}.
 */
class ConditionalOnCloudPlatformTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void outcomeWhenCloudfoundryPlatformNotPresentShouldNotMatch() {
		this.contextRunner.withUserConfiguration(CloudFoundryPlatformConfig.class)
			.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	void outcomeWhenCloudfoundryPlatformPresentShouldMatch() {
		this.contextRunner.withUserConfiguration(CloudFoundryPlatformConfig.class)
			.withPropertyValues("VCAP_APPLICATION:---")
			.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void outcomeWhenCloudfoundryPlatformPresentAndMethodTargetShouldMatch() {
		this.contextRunner.withUserConfiguration(CloudFoundryPlatformOnMethodConfig.class)
			.withPropertyValues("VCAP_APPLICATION:---")
			.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
	static class CloudFoundryPlatformConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CloudFoundryPlatformOnMethodConfig {

		@Bean
		@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
		String foo() {
			return "foo";
		}

	}

}
