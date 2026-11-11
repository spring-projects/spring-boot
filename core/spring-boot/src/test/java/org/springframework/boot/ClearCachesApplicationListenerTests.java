/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import org.assertj.core.api.SoftAssertionsProvider.ThrowingRunnable;
import org.junit.jupiter.api.Test;

import org.springframework.context.event.ContextRefreshedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClearCachesApplicationListener}.
 *
 * @author Phillip Webb
 */
class ClearCachesApplicationListenerTests {

	@Test
	void onContextRefreshedEventWhenNoMethodDoesNothing() {
		TestClassLoader classLoader = new TestClassLoader(Thread.currentThread().getContextClassLoader());
		assertThatNoException().isThrownBy(() -> triggerOnContextRefreshedEvent(classLoader, () -> {
		}));
	}

	@Test
	void onContextRefreshedEventWhenDirectMethodInvokesMethod() throws Exception {
		ClearableClassLoader classLoader = new ClearableClassLoader(Thread.currentThread().getContextClassLoader());
		triggerOnContextRefreshedEvent(classLoader, () -> assertThat(classLoader.cleared()).isTrue());
	}

	@Test
	void onContextRefreshedEventWhenInheritedMethodInvokesMethod() throws Exception {
		ExtendedClearableClassLoader classLoader = new ExtendedClearableClassLoader(
				Thread.currentThread().getContextClassLoader());
		triggerOnContextRefreshedEvent(classLoader, () -> assertThat(classLoader.cleared()).isTrue());
	}

	@Test
	void onContextRefreshedEventWhenInParentInvokesMethod() throws Exception {
		ClearableClassLoader parentClassLoader = new ClearableClassLoader(
				Thread.currentThread().getContextClassLoader());
		TestClassLoader classLoader = new TestClassLoader(parentClassLoader);
		triggerOnContextRefreshedEvent(classLoader, () -> assertThat(parentClassLoader.cleared()).isTrue());
	}

	private void triggerOnContextRefreshedEvent(ClassLoader classLoader, ThrowingRunnable action) throws Exception {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			new ClearCachesApplicationListener().onApplicationEvent(mock(ContextRefreshedEvent.class));
			action.run();
		}
		finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	public static class TestClassLoader extends ClassLoader {

		TestClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

	public static class ClearableClassLoader extends TestClassLoader {

		private boolean cleared;

		ClearableClassLoader(ClassLoader parent) {
			super(parent);
		}

		public void clearCache() {
			this.cleared = true;
		}

		boolean cleared() {
			return this.cleared;
		}

	}

	public static class ExtendedClearableClassLoader extends ClearableClassLoader {

		ExtendedClearableClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

}
