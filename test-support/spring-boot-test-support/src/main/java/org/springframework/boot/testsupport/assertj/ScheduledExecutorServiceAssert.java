/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.assertj;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assert;

import org.springframework.lang.CheckReturnValue;
import org.springframework.util.ReflectionUtils;

/**
 * AssertJ {@link Assert} for {@link ScheduledExecutorService}.
 *
 * @author Mike Turbe
 * @author Moritz Halbritter
 */
public final class ScheduledExecutorServiceAssert
		extends AbstractAssert<ScheduledExecutorServiceAssert, ScheduledExecutorService> {

	private ScheduledExecutorServiceAssert(ScheduledExecutorService actual) {
		super(actual, ScheduledExecutorServiceAssert.class);
	}

	/**
	 * Verifies that the actual executor uses platform threads.
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual executor doesn't use platform threads
	 */
	public ScheduledExecutorServiceAssert usesPlatformThreads() {
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
	public ScheduledExecutorServiceAssert usesVirtualThreads() {
		isNotNull();
		if (!producesVirtualThreads()) {
			failWithMessage("Expected executor to use virtual threads, but it uses platform threads");
		}
		return this;
	}

	private boolean producesVirtualThreads() {
		try {
			return this.actual.schedule(() -> {
				Method isVirtual = ReflectionUtils.findMethod(Thread.class, "isVirtual");
				if (isVirtual == null) {
					return false;
				}
				return (boolean) ReflectionUtils.invokeMethod(isVirtual, Thread.currentThread());
			}, 0, TimeUnit.SECONDS).get();
		}
		catch (InterruptedException | ExecutionException ex) {
			throw new AssertionError(ex);
		}
	}

	/**
	 * Creates a new assertion instance with the given {@link ScheduledExecutorService}.
	 * @param actual the {@link ScheduledExecutorService}
	 * @return the assertion instance
	 */
	@CheckReturnValue
	public static ScheduledExecutorServiceAssert assertThat(ScheduledExecutorService actual) {
		return new ScheduledExecutorServiceAssert(actual);
	}

}
