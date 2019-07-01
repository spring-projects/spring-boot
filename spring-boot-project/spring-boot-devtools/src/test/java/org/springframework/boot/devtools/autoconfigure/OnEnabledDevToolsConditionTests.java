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

package org.springframework.boot.devtools.autoconfigure;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OnEnabledDevToolsCondition}.
 *
 * @author Madhura Bhave
 */
class OnEnabledDevToolsConditionTests {

	private AnnotationConfigApplicationContext context;

	@BeforeEach
	void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class);
	}

	@Test
	void outcomeWhenDevtoolsShouldBeEnabledIsTrueShouldMatch() throws Exception {
		AtomicBoolean containsBean = new AtomicBoolean();
		Thread thread = new Thread(() -> {
			OnEnabledDevToolsConditionTests.this.context.refresh();
			containsBean.set(OnEnabledDevToolsConditionTests.this.context.containsBean("test"));
		});
		thread.start();
		thread.join();
		assertThat(containsBean).isTrue();
	}

	@Test
	void outcomeWhenDevtoolsShouldBeEnabledIsFalseShouldNotMatch() {
		OnEnabledDevToolsConditionTests.this.context.refresh();
		assertThat(OnEnabledDevToolsConditionTests.this.context.containsBean("test")).isFalse();
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		@Conditional(OnEnabledDevToolsCondition.class)
		String test() {
			return "hello";
		}

	}

}
