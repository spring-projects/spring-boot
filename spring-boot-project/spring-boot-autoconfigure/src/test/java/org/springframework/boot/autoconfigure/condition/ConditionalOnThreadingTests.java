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
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnThreading}.
 *
 * @author Moritz Halbritter
 */
class ConditionalOnThreadingTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(BasicConfiguration.class);

	@Test
	@EnabledForJreRange(max = JRE.JAVA_20)
	void platformThreadsOnJdkBelow21IfVirtualThreadsPropertyIsEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true")
			.run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.PLATFORM));
	}

	@Test
	@EnabledForJreRange(max = JRE.JAVA_20)
	void platformThreadsOnJdkBelow21IfVirtualThreadsPropertyIsDisabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=false")
			.run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.PLATFORM));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void virtualThreadsOnJdk21IfVirtualThreadsPropertyIsEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true")
			.run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.VIRTUAL));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void platformThreadsOnJdk21IfVirtualThreadsPropertyIsDisabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=false")
			.run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.PLATFORM));
	}

	private enum ThreadType {

		PLATFORM, VIRTUAL

	}

	@Configuration(proxyBeanMethods = false)
	static class BasicConfiguration {

		@Bean
		@ConditionalOnThreading(Threading.VIRTUAL)
		ThreadType virtual() {
			return ThreadType.VIRTUAL;
		}

		@Bean
		@ConditionalOnThreading(Threading.PLATFORM)
		ThreadType platform() {
			return ThreadType.PLATFORM;
		}

	}

}
