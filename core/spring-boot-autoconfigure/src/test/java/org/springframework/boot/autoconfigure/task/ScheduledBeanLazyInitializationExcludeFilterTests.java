/*
 * Copyright 2012-present the original author or authors.
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

	private static final class TestBean {

		@Scheduled
		void doStuff() {
		}

	}

	private static final class AnotherTestBean {

		@Schedules({ @Scheduled(fixedRate = 5000), @Scheduled(fixedRate = 2500) })
		void doStuff() {
		}

	}

}
