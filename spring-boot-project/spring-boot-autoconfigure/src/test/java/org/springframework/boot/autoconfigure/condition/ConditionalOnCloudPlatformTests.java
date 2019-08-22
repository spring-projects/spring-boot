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
				.withPropertyValues("VCAP_APPLICATION:---").run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void outcomeWhenCloudfoundryPlatformPresentAndMethodTargetShouldMatch() {
		this.contextRunner.withUserConfiguration(CloudFoundryPlatformOnMethodConfig.class)
				.withPropertyValues("VCAP_APPLICATION:---").run((context) -> assertThat(context).hasBean("foo"));
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
