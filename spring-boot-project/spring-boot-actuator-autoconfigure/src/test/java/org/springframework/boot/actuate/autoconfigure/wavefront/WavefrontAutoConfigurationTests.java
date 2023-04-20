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

package org.springframework.boot.actuate.autoconfigure.wavefront;

import java.util.ArrayList;
import java.util.List;

import com.wavefront.sdk.common.application.ApplicationTags;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WavefrontAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class WavefrontAutoConfigurationTests {

	ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WavefrontAutoConfiguration.class));

	@Test
	void wavefrontApplicationTagsWhenHasUserBeanBacksOff() {
		this.contextRunner.withUserConfiguration(TestApplicationTagsConfiguration.class).run((context) -> {
			ApplicationTags tags = context.getBean(ApplicationTags.class);
			assertThat(tags.getApplication()).isEqualTo("test-application");
			assertThat(tags.getService()).isEqualTo("test-service");
		});
	}

	@Test
	void wavefrontApplicationTagsMapsProperties() {
		List<String> properties = new ArrayList<>();
		properties.add("management.wavefront.application.name=test-application");
		properties.add("management.wavefront.application.service-name=test-service");
		properties.add("management.wavefront.application.cluster-name=test-cluster");
		properties.add("management.wavefront.application.shard-name=test-shard");
		properties.add("management.wavefront.application.custom-tags.foo=FOO");
		properties.add("management.wavefront.application.custom-tags.bar=BAR");
		this.contextRunner.withPropertyValues(properties.toArray(String[]::new)).run((context) -> {
			ApplicationTags tags = context.getBean(ApplicationTags.class);
			assertThat(tags.getApplication()).isEqualTo("test-application");
			assertThat(tags.getService()).isEqualTo("test-service");
			assertThat(tags.getCluster()).isEqualTo("test-cluster");
			assertThat(tags.getShard()).isEqualTo("test-shard");
			assertThat(tags.getCustomTags()).hasSize(2).containsEntry("foo", "FOO").containsEntry("bar", "BAR");
		});
	}

	@Test
	void wavefrontApplicationTagsWhenNoPropertiesUsesDefaults() {
		this.contextRunner.withPropertyValues("spring.application.name=spring-app").run((context) -> {
			ApplicationTags tags = context.getBean(ApplicationTags.class);
			assertThat(tags.getApplication()).isEqualTo("unnamed_application");
			assertThat(tags.getService()).isEqualTo("spring-app");
			assertThat(tags.getCluster()).isNull();
			assertThat(tags.getShard()).isNull();
			assertThat(tags.getCustomTags()).isEmpty();
		});
	}

	@Test
	void wavefrontApplicationTagsWhenHasNoServiceNamePropertyAndNoSpringApplicationNameUsesDefault() {
		this.contextRunner.run((context) -> {
			ApplicationTags tags = context.getBean(ApplicationTags.class);
			assertThat(tags.getService()).isEqualTo("unnamed_service");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class TestApplicationTagsConfiguration {

		@Bean
		ApplicationTags applicationTags() {
			return new ApplicationTags.Builder("test-application", "test-service").build();
		}

	}

}
