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

package org.springframework.boot.test.system;

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
 * individual test method through {@link ExtendWith @ExtendWith}. This extension provides
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
 * <p>
 * To ensure that their output can be captured, Java Util Logging (JUL) and Log4j2 require
 * additional configuration.
 * <p>
 * To reliably capture output from Java Util Logging, reset its configuration after each
 * test:
 *
 * <pre class="code">
 * &#064;AfterEach
 * void reset() throws Exception {
 *     LogManager.getLogManager().readConfiguration();
 * }
 * </pre>
 * <p>
 * To reliably capture output from Log4j2, set the <code>follow</code> attribute of the
 * console appender to <code>true</code>:
 *
 * <pre class="code">
 * &lt;Appenders&gt;
 *     &lt;Console name="Console" target="SYSTEM_OUT" follow="true"&gt;
 *         ...
 *     &lt;/Console&gt;
*  &lt;/Appenders&gt;
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

	/**
     * Constructor for the OutputCaptureExtension class.
     * 
     * This constructor is package private to prevent users from directly creating an instance.
     */
    OutputCaptureExtension() {
		// Package private to prevent users from directly creating an instance.
	}

	/**
     * This method is called before all test methods in the test class.
     * It is an overridden method from the ExtensionContext class.
     * It throws an Exception if an error occurs.
     * 
     * @param context the ExtensionContext object representing the current test context
     * @throws Exception if an error occurs during the execution of the method
     */
    @Override
	public void beforeAll(ExtensionContext context) throws Exception {
		getOutputCapture(context).push();
	}

	/**
     * This method is called after all tests have been executed.
     * It is used to pop the output capture from the context.
     *
     * @param context the extension context
     * @throws Exception if an error occurs while popping the output capture
     */
    @Override
	public void afterAll(ExtensionContext context) throws Exception {
		getOutputCapture(context).pop();
	}

	/**
     * This method is executed before each test method in the test class.
     * It is used to push the output capture for the current test context.
     *
     * @param context the extension context for the current test
     * @throws Exception if an error occurs during the execution of the method
     */
    @Override
	public void beforeEach(ExtensionContext context) throws Exception {
		getOutputCapture(context).push();
	}

	/**
     * This method is called after each test execution to remove the captured output from the stack.
     *
     * @param context the extension context
     * @throws Exception if an error occurs during the removal of the captured output
     */
    @Override
	public void afterEach(ExtensionContext context) throws Exception {
		getOutputCapture(context).pop();
	}

	/**
     * Determines if the given parameter is supported by this extension.
     * 
     * @param parameterContext the parameter context
     * @param extensionContext the extension context
     * @return true if the parameter is supported, false otherwise
     * @throws ParameterResolutionException if the parameter resolution fails
     */
    @Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return CapturedOutput.class.equals(parameterContext.getParameter().getType());
	}

	/**
     * Resolves the parameter for the given parameter context and extension context.
     * 
     * @param parameterContext the parameter context
     * @param extensionContext the extension context
     * @return the resolved parameter object
     */
    @Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return getOutputCapture(extensionContext);
	}

	/**
     * Retrieves the OutputCapture instance from the ExtensionContext's store, or creates a new instance if it does not exist.
     * 
     * @param context the ExtensionContext object representing the current extension context
     * @return the OutputCapture instance retrieved from the store or a new instance if it does not exist
     */
    private OutputCapture getOutputCapture(ExtensionContext context) {
		return getStore(context).getOrComputeIfAbsent(OutputCapture.class, (key) -> new OutputCapture(),
				OutputCapture.class);
	}

	/**
     * Retrieves the store associated with the given extension context.
     * 
     * @param context the extension context
     * @return the store associated with the extension context
     */
    private Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(getClass()));
	}

}
