/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.assertj;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Tests for {@link SimpleAsyncTaskExecutorAssert}.
 *
 * @author Moritz Halbritter
 */
class SimpleAsyncTaskExecutorAssertTests {

	@Test
	void usesPlatformThreads() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setVirtualThreads(false);
		SimpleAsyncTaskExecutorAssert.assertThat(executor).usesPlatformThreads();
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void usesVirtualThreads() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setVirtualThreads(true);
		SimpleAsyncTaskExecutorAssert.assertThat(executor).usesVirtualThreads();
	}

}
