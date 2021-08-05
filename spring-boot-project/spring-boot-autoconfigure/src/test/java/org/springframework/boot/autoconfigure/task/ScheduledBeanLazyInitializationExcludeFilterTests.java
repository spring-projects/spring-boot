/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.task;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScheduledBeanLazyInitializationExcludeFilter}.
 *
 * @author Stephane Nicoll
 */
class ScheduledBeanLazyInitializationExcludeFilterTests {

	private final ScheduledBeanLazyInitializationExcludeFilter filter = new ScheduledBeanLazyInitializationExcludeFilter();

	@Test
	void beanWithScheduledMethodIsDetected() {
		assertThat(isExcluded(TestBean.class)).isTrue();
	}

	@Test
	void beanWithSchedulesMethodIsDetected() {
		assertThat(isExcluded(AnotherTestBean.class)).isTrue();
	}

	@Test
	void beanWithoutScheduledMethodIsDetected() {
		assertThat(isExcluded(ScheduledBeanLazyInitializationExcludeFilterTests.class)).isFalse();
	}

	private boolean isExcluded(Class<?> type) {
		return this.filter.isExcluded("test", new RootBeanDefinition(type), type);
	}

	private static class TestBean {

		@Scheduled
		void doStuff() {
		}

	}

	private static class AnotherTestBean {

		@Schedules({ @Scheduled(fixedRate = 5000), @Scheduled(fixedRate = 2500) })
		void doStuff() {
		}

	}

}
