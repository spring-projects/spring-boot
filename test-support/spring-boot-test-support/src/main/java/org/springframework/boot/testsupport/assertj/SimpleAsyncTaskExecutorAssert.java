/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.assertj;

import java.lang.reflect.Field;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assert;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.CheckReturnValue;
import org.springframework.util.ReflectionUtils;

/**
 * AssertJ {@link Assert} for {@link SimpleAsyncTaskExecutor}.
 *
 * @author Moritz Halbritter
 */
public final class SimpleAsyncTaskExecutorAssert
		extends AbstractAssert<SimpleAsyncTaskExecutorAssert, SimpleAsyncTaskExecutor> {

	private SimpleAsyncTaskExecutorAssert(SimpleAsyncTaskExecutor actual) {
		super(actual, SimpleAsyncTaskExecutorAssert.class);
	}

	/**
	 * Verifies that the actual executor uses platform threads.
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual executor doesn't use platform threads
	 */
	public SimpleAsyncTaskExecutorAssert usesPlatformThreads() {
		isNotNull();
		if (producesVirtualThreads()) {
			failWithMessage("Expected executor to use platform threads, but it uses virtual threads");
		}
		return this;
	}

	/**
	 * Verifies that the actual executor uses virtual threads.
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual executor doesn't use virtual threads
	 */
	public SimpleAsyncTaskExecutorAssert usesVirtualThreads() {
		isNotNull();
		if (!producesVirtualThreads()) {
			failWithMessage("Expected executor to use virtual threads, but it uses platform threads");
		}
		return this;
	}

	private boolean producesVirtualThreads() {
		Field field = ReflectionUtils.findField(SimpleAsyncTaskExecutor.class, "virtualThreadDelegate");
		if (field == null) {
			throw new IllegalStateException("Field SimpleAsyncTaskExecutor.virtualThreadDelegate not found");
		}
		ReflectionUtils.makeAccessible(field);
		Object virtualThreadDelegate = ReflectionUtils.getField(field, this.actual);
		return virtualThreadDelegate != null;
	}

	/**
	 * Creates a new assertion instance with the given {@link SimpleAsyncTaskExecutor}.
	 * @param actual the {@link SimpleAsyncTaskExecutor}
	 * @return the assertion instance
	 */
	@CheckReturnValue
	public static SimpleAsyncTaskExecutorAssert assertThat(SimpleAsyncTaskExecutor actual) {
		return new SimpleAsyncTaskExecutorAssert(actual);
	}

}
