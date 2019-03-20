/*
 * Copyright 2012-2016 the original author or authors.
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

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnCloudPlatform}.
 */
public class ConditionalOnCloudPlatformTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void outcomeWhenCloudfoundryPlatformNotPresentShouldNotMatch()
			throws Exception {
		load(CloudFoundryPlatformConfig.class, "");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void outcomeWhenCloudfoundryPlatformPresentShouldMatch() throws Exception {
		load(CloudFoundryPlatformConfig.class, "VCAP_APPLICATION:---");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void outcomeWhenCloudfoundryPlatformPresentAndMethodTargetShouldMatch()
			throws Exception {
		load(CloudFoundryPlatformOnMethodConfig.class, "VCAP_APPLICATION:---");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		this.context.register(config);
		this.context.refresh();
	}

	@Configuration
	@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
	static class CloudFoundryPlatformConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	static class CloudFoundryPlatformOnMethodConfig {

		@Bean
		@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
		public String foo() {
			return "foo";
		}

	}

}
