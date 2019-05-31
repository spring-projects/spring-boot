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

package org.springframework.boot.test.extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * JUnit5 {@code @Extension} to capture output {@link System#out System.out} and
 * {@link System#err System.err}. Can be used on a test class via
 * {@link ExtendWith @ExtendWith}, or on field using
 * {@link RegisterExtension @RegisterExtension}. This extension provides access to
 * {@link CapturedOutput} instances which can be used to assert that the correct output
 * was written.
 * <p>
 * To use with {@link ExtendWith @ExtendWith}, inject the {@link CapturedOutput} as a test
 * argument: <pre class="code">
 * &#064;ExtendWith(OutputExtension.class)
 * class MyTest {
 *
 *   &#064;Test
 *   void test(CapturedOutput output) {
 *       assertThat(output).contains("ok");
 *   }
 *
 * }
 * </pre>
 * <p>
 * To use with {@link RegisterExtension @RegisterExtension}, use the {@link #capture()
 * capture} factory method: argument: <pre class="code">
 * class MyTest {
 *
 *   &#064;RegisterExtension
 *   CapturedOutput output = OutputExtension.capture();
 *
 *   &#064;Test
 *   void test() {
 *       assertThat(output).contains("ok");
 *   }
 *
 * }
 * </pre>
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.2.0
 * @see CapturedOutput
 */
public class OutputExtension extends CapturedOutput implements BeforeAllCallback,
		AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

	OutputExtension() {
		// Package private to prevent users from directly creating an instance.
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		push();
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		pop();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		push();
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		pop();
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return CapturedOutput.class.equals(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return this;
	}

	/**
	 * Factory method for use with {@link RegisterExtension @RegisterExtension} fields.
	 * @return a new {@link CapturedOutput} instance
	 */
	public static CapturedOutput capture() {
		return new OutputExtension();
	}

}
