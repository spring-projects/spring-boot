/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.lifecycle;

import java.lang.reflect.InaccessibleObjectException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.lifecycle.Startables;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit extension used by reset startables.
 *
 * @author Phillip Webb
 */
class ResetStartablesExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		reset();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		reset();
	}

	private void reset() {
		try {
			Object executor = ReflectionTestUtils.getField(Startables.class, "EXECUTOR");
			assertThat(executor).isNotNull();
			Object threadFactory = ReflectionTestUtils.getField(executor, "threadFactory");
			assertThat(threadFactory).isNotNull();
			AtomicLong counter = (AtomicLong) ReflectionTestUtils.getField(threadFactory, "COUNTER");
			assertThat(counter).isNotNull();
			counter.set(0);
		}
		catch (InaccessibleObjectException ex) {
			throw new IllegalStateException(
					"Unable to reset field. Please run with '--add-opens=java.base/java.util.concurrent=ALL-UNNAMED'",
					ex);
		}
	}

}
