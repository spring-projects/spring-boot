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

package org.springframework.boot.testsupport.system;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit Jupiter {@code @Extension} to capture {@link System#out System.out} and
 * {@link System#err System.err}. Can be registered for an entire test class or for an
 * individual test method via {@link ExtendWith @ExtendWith}. This extension provides
 * {@linkplain ParameterResolver parameter resolution} for a {@link CapturedOutput}
 * instance which can be used to assert that the correct output was written.
 * <p>
 * To use with {@link ExtendWith @ExtendWith}, inject the {@link CapturedOutput} as an
 * argument to your test class constructor, test method, or lifecycle methods:
 *
 * <pre class="code">
 * &#064;ExtendWith(OutputCaptureExtension.class)
 * class MyTest {
 *
 *     &#064;Test
 *     void test(CapturedOutput output) {
 *         System.out.println("ok");
 *         assertThat(output).contains("ok");
 *         System.err.println("error");
 *     }
 *
 *     &#064;AfterEach
 *     void after(CapturedOutput output) {
 *         assertThat(output.getOut()).contains("ok");
 *         assertThat(output.getErr()).contains("error");
 *     }
 *
 * }
 * </pre>
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 2.2.0
 * @see CapturedOutput
 */
public class OutputCaptureExtension
		implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

	OutputCaptureExtension() {
		// Package private to prevent users from directly creating an instance.
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		getOutputCapture(context).push();
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		getOutputCapture(context).pop();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		getOutputCapture(context).push();
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		getOutputCapture(context).pop();
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return CapturedOutput.class.equals(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return getOutputCapture(extensionContext);
	}

	private OutputCapture getOutputCapture(ExtensionContext context) {
		return getStore(context).getOrComputeIfAbsent(OutputCapture.class);
	}

	private Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(getClass()));
	}

}
