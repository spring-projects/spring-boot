/*
 * Copyright 2012-2019 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MainMethod}.
 *
 * @author Phillip Webb
 */
class MainMethodTests {

	private static ThreadLocal<MainMethod> mainMethod = new ThreadLocal<>();

	private Method actualMain;

	@BeforeEach
	void setup() throws Exception {
		this.actualMain = Valid.class.getMethod("main", String[].class);
	}

	@Test
	void threadMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new MainMethod(null))
				.withMessageContaining("Thread must not be null");
	}

	@Test
	void validMainMethod() throws Exception {
		MainMethod method = new TestThread(Valid::main).test();
		assertThat(method.getMethod()).isEqualTo(this.actualMain);
		assertThat(method.getDeclaringClassName()).isEqualTo(this.actualMain.getDeclaringClass().getName());
	}

	@Test
	void missingArgsMainMethod() throws Exception {
		assertThatIllegalStateException().isThrownBy(() -> new TestThread(MissingArgs::main).test())
				.withMessageContaining("Unable to find main method");
	}

	@Test
	void nonStatic() throws Exception {
		assertThatIllegalStateException().isThrownBy(() -> new TestThread(() -> new NonStaticMain().main()).test())
				.withMessageContaining("Unable to find main method");
	}

	static class TestThread extends Thread {

		private final Runnable runnable;

		private Exception exception;

		private MainMethod mainMethod;

		TestThread(Runnable runnable) {
			this.runnable = runnable;
		}

		MainMethod test() throws InterruptedException {
			start();
			join();
			if (this.exception != null) {
				ReflectionUtils.rethrowRuntimeException(this.exception);
			}
			return this.mainMethod;
		}

		@Override
		public void run() {
			try {
				this.runnable.run();
				this.mainMethod = MainMethodTests.mainMethod.get();
			}
			catch (Exception ex) {
				this.exception = ex;
			}
		}

	}

	public static class Valid {

		public static void main(String... args) {
			someOtherMethod();
		}

		private static void someOtherMethod() {
			mainMethod.set(new MainMethod());
		}

	}

	public static class MissingArgs {

		public static void main() {
			mainMethod.set(new MainMethod());
		}

	}

	public static class NonStaticMain {

		void main(String... args) {
			mainMethod.set(new MainMethod());
		}

	}

}
