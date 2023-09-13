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

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnCheckpointRestore @ConditionalOnCheckpointRestore}.
 *
 * @author Andy Wilkinson
 */
class ConditionalOnCheckpointRestoreTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(BasicConfiguration.class);

	@Test
	void whenCracIsUnavailableThenConditionDoesNotMatch() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("someBean"));
	}

	@Test
	@ClassPathOverrides("org.crac:crac:1.3.0")
	void whenCracIsAvailableThenConditionMatches() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("someBean"));
	}

	@Configuration(proxyBeanMethods = false)
	static class BasicConfiguration {

		@Bean
		@ConditionalOnCheckpointRestore
		String someBean() {
			return "someBean";
		}

	}

}
