/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultRestartInitializer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public class DefaultRestartInitializerTests {

	@Test
	public void jUnitStackShouldReturnNull() {
		testSkippedStacks("org.junit.runners.Something");
	}

	@Test
	public void jUnit5StackShouldReturnNull() {
		testSkippedStacks("org.junit.platform.Something");
	}

	@Test
	public void springTestStackShouldReturnNull() {
		testSkippedStacks("org.springframework.boot.test.Something");
	}

	@Test
	public void cucumberStackShouldReturnNull() {
		testSkippedStacks("cucumber.runtime.Runtime.run");
	}

	@Test
	public void validMainThreadShouldReturnUrls() {
		DefaultRestartInitializer initializer = new DefaultRestartInitializer();
		ClassLoader classLoader = new MockAppClassLoader(getClass().getClassLoader());
		Thread thread = new Thread();
		thread.setName("main");
		thread.setContextClassLoader(classLoader);
		assertThat(initializer.getInitialUrls(thread)).isNotNull();
	}

	@Test
	public void threadNotNamedMainShouldReturnNull() {
		DefaultRestartInitializer initializer = new DefaultRestartInitializer();
		ClassLoader classLoader = new MockAppClassLoader(getClass().getClassLoader());
		Thread thread = new Thread();
		thread.setName("buscuit");
		thread.setContextClassLoader(classLoader);
		assertThat(initializer.getInitialUrls(thread)).isNull();
	}

	@Test
	public void threadNotUsingAppClassLoader() {
		DefaultRestartInitializer initializer = new DefaultRestartInitializer();
		ClassLoader classLoader = new MockLauncherClassLoader(
				getClass().getClassLoader());
		Thread thread = new Thread();
		thread.setName("main");
		thread.setContextClassLoader(classLoader);
		assertThat(initializer.getInitialUrls(thread)).isNull();
	}

	@Test
	public void urlsCanBeRetrieved() {
		assertThat(new DefaultRestartInitializer().getUrls(Thread.currentThread()))
				.isNotEmpty();
	}

	protected void testSkippedStacks(String s) {
		DefaultRestartInitializer initializer = new DefaultRestartInitializer();
		ClassLoader classLoader = new MockAppClassLoader(getClass().getClassLoader());
		Thread thread = mock(Thread.class);
		thread.setName("main");
		StackTraceElement element = new StackTraceElement(s, "someMethod", "someFile",
				123);
		given(thread.getStackTrace()).willReturn(new StackTraceElement[] { element });
		given(thread.getContextClassLoader()).willReturn(classLoader);
		assertThat(initializer.getInitialUrls(thread)).isEqualTo(null);
	}

	private static class MockAppClassLoader extends ClassLoader {

		MockAppClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

	private static class MockLauncherClassLoader extends ClassLoader {

		MockLauncherClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

}
