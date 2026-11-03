/*
 * Copyright 2012-present the original author or authors.
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

		public TestClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

	public static class ClearableClassLoader extends TestClassLoader {

		private boolean cleared = false;

		public ClearableClassLoader(ClassLoader parent) {
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

		public ExtendedClearableClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

}
