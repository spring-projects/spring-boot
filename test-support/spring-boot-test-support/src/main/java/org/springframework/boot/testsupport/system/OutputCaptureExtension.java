/*
 * Copyright 2012-present the original author or authors.
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
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Sam Brannen
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
		return getStore(context).computeIfAbsent(OutputCapture.class);
	}

	private Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(getClass()));
	}

}
