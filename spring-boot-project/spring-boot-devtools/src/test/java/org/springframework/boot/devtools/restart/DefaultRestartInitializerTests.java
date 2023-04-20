/*
 * Copyright 2012-2023 the original author or authors.
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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

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
class DefaultRestartInitializerTests {

	@Test
	void jUnitStackShouldReturnNull() {
		testSkippedStacks("org.junit.runners.Something");
	}

	@Test
	void jUnit5StackShouldReturnNull() {
		testSkippedStacks("org.junit.platform.Something");
	}

	@Test
	void springTestStackShouldReturnNull() {
		testSkippedStacks("org.springframework.boot.test.Something");
	}

	@Test
	void cucumberStackShouldReturnNull() {
		testSkippedStacks("cucumber.runtime.Runtime.run");
	}

	@Test
	void validMainThreadShouldReturnUrls() {
		DefaultRestartInitializer initializer = new DefaultRestartInitializer();
		ClassLoader classLoader = new MockAppClassLoader(getClass().getClassLoader());
		Thread thread = new Thread();
		thread.setName("main");
		thread.setContextClassLoader(classLoader);
		assertThat(initializer.getInitialUrls(thread)).isNotNull();
	}

	@Test
	void threadNotNamedMainShouldReturnNull() {
		DefaultRestartInitializer initializer = new DefaultRestartInitializer();
		ClassLoader classLoader = new MockAppClassLoader(getClass().getClassLoader());
		Thread thread = new Thread();
		thread.setName("buscuit");
		thread.setContextClassLoader(classLoader);
		assertThat(initializer.getInitialUrls(thread)).isNull();
	}

	@Test
	void threadNotUsingAppClassLoader() {
		DefaultRestartInitializer initializer = new DefaultRestartInitializer();
		ClassLoader classLoader = new MockLauncherClassLoader(getClass().getClassLoader());
		Thread thread = new Thread();
		thread.setName("main");
		thread.setContextClassLoader(classLoader);
		assertThat(initializer.getInitialUrls(thread)).isNull();
	}

	@Test
	void urlsCanBeRetrieved() throws IOException {
		Thread thread = Thread.currentThread();
		ClassLoader classLoader = thread.getContextClassLoader();
		try (URLClassLoader contextClassLoader = new URLClassLoader(
				new URL[] { new URL("file:test-app/build/classes/main/") }, classLoader)) {
			thread.setContextClassLoader(contextClassLoader);
			assertThat(new DefaultRestartInitializer().getUrls(thread)).isNotEmpty();
		}
		finally {
			thread.setContextClassLoader(classLoader);
		}
	}

	protected void testSkippedStacks(String s) {
		DefaultRestartInitializer initializer = new DefaultRestartInitializer();
		ClassLoader classLoader = new MockAppClassLoader(getClass().getClassLoader());
		Thread thread = mock(Thread.class);
		given(thread.getName()).willReturn("main");
		StackTraceElement element = new StackTraceElement(s, "someMethod", "someFile", 123);
		given(thread.getStackTrace()).willReturn(new StackTraceElement[] { element });
		given(thread.getContextClassLoader()).willReturn(classLoader);
		assertThat(initializer.getInitialUrls(thread)).isNull();
	}

	static class MockAppClassLoader extends ClassLoader {

		MockAppClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

	static class MockLauncherClassLoader extends ClassLoader {

		MockLauncherClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

}
