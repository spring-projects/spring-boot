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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnAnnotation}.
 *
 * @author Liu Feng
 */
class ConditionalOnAnnotationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	@EnabledForJreRange(max = JRE.JAVA_20)
	void platformThreadsOnJdkBelow21IfVirtualThreadsPropertyIsEnabled() {
		this.contextRunner.withUserConfiguration(OnConfigurationWithoutTestAnnotation.class, BasicConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(LOADED_BEAN));
		this.contextRunner.withUserConfiguration(OnConfigurationWithTestAnnotation.class, BasicConfiguration.class)
			.run((context) -> assertThat(context).hasBean(LOADED_BEAN));
	}

	static final String LOADED_BEAN = "loaded_bean";

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnAnnotation(TestConditionalOnAnnotation.class)
	static class BasicConfiguration {

		@Bean(LOADED_BEAN)
		String loadWithAnnotation() {
			return LOADED_BEAN;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestConditionalOnAnnotation
	static class OnConfigurationWithTestAnnotation {

	}

	@Configuration(proxyBeanMethods = false)
	static class OnConfigurationWithoutTestAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestConditionalOnAnnotation {

	}

}
