/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.restart;

import java.net.URL;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DefaultRestartInitializer}.
 *
 * @author Phillip Webb
 */
public class DefaultRestartInitializerTests {

	@Test
	public void nullForTests() throws Exception {
		MockRestartInitializer initializer = new MockRestartInitializer(true);
		assertThat(initializer.getInitialUrls(Thread.currentThread()), nullValue());
	}

	@Test
	public void validMainThread() throws Exception {
		MockRestartInitializer initializer = new MockRestartInitializer(false);
		ClassLoader classLoader = new MockAppClassLoader(getClass().getClassLoader());
		Thread thread = new Thread();
		thread.setName("main");
		thread.setContextClassLoader(classLoader);
		assertThat(initializer.isMain(thread), equalTo(true));
		assertThat(initializer.getInitialUrls(thread), not(nullValue()));
	}

	@Test
	public void threadNotNamedMain() throws Exception {
		MockRestartInitializer initializer = new MockRestartInitializer(false);
		ClassLoader classLoader = new MockAppClassLoader(getClass().getClassLoader());
		Thread thread = new Thread();
		thread.setName("buscuit");
		thread.setContextClassLoader(classLoader);
		assertThat(initializer.isMain(thread), equalTo(false));
		assertThat(initializer.getInitialUrls(thread), nullValue());
	}

	@Test
	public void threadNotUsingAppClassLoader() throws Exception {
		MockRestartInitializer initializer = new MockRestartInitializer(false);
		ClassLoader classLoader = new MockLauncherClassLoader(
				getClass().getClassLoader());
		Thread thread = new Thread();
		thread.setName("main");
		thread.setContextClassLoader(classLoader);
		assertThat(initializer.isMain(thread), equalTo(false));
		assertThat(initializer.getInitialUrls(thread), nullValue());
	}

	@Test
	public void skipsDueToJUnitStacks() throws Exception {
		testSkipStack("org.junit.runners.Something", true);
	}

	@Test
	public void skipsDueToSpringTest() throws Exception {
		testSkipStack("org.springframework.boot.test.Something", true);
	}

	private void testSkipStack(String className, boolean expected) {
		MockRestartInitializer initializer = new MockRestartInitializer(true);
		StackTraceElement element = new StackTraceElement(className, "someMethod",
				"someFile", 123);
		assertThat(initializer.isSkippedStackElement(element), equalTo(expected));
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

	private static class MockRestartInitializer extends DefaultRestartInitializer {

		private final boolean considerStackElements;

		MockRestartInitializer(boolean considerStackElements) {
			this.considerStackElements = considerStackElements;
		}

		@Override
		protected boolean isSkippedStackElement(StackTraceElement element) {
			if (!this.considerStackElements) {
				return false;
			}
			return true;
		}

		@Override
		protected URL[] getUrls(Thread thread) {
			return new URL[0];
		}

	}

}
