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

package org.springframework.boot.testsupport.classpath;

import java.lang.reflect.Method;
import java.net.URLClassLoader;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import org.springframework.util.CollectionUtils;

/**
 * A custom {@link Extension} that runs tests using a modified class path. Entries are
 * excluded from the class path using {@link ClassPathExclusions @ClassPathExclusions} and
 * overridden using {@link ClassPathOverrides @ClassPathOverrides} on the test class. For
 * an unchanged copy of the class path {@link ForkedClassPath @ForkedClassPath} can be
 * used. A class loader is created with the customized class path and is used both to load
 * the test class and as the thread context class loader while the test is being run.
 *
 * @author Christoph Dreis
 */
class ModifiedClassPathExtension implements InvocationInterceptor {

	/**
	 * Intercepts the execution of a method before all methods.
	 * @param invocation the invocation object representing the method being intercepted
	 * @param invocationContext the reflective invocation context of the method being
	 * intercepted
	 * @param extensionContext the extension context of the test being executed
	 * @throws Throwable if an error occurs during interception
	 */
	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		intercept(invocation, extensionContext);
	}

	/**
	 * Intercepts the execution of a method before each test method is invoked.
	 * @param invocation the invocation object representing the method being invoked
	 * @param invocationContext the reflective invocation context of the method being
	 * invoked
	 * @param extensionContext the extension context of the test being executed
	 * @throws Throwable if an error occurs during the interception
	 */
	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		intercept(invocation, extensionContext);
	}

	/**
	 * This method is called after each test method is executed. It intercepts the method
	 * invocation and the extension context.
	 * @param invocation The invocation object representing the method invocation.
	 * @param invocationContext The reflective invocation context of the method.
	 * @param extensionContext The extension context of the test.
	 * @throws Throwable if an error occurs during the interception.
	 */
	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		intercept(invocation, extensionContext);
	}

	/**
	 * Intercepts the execution of a method after all methods have been invoked.
	 * @param invocation the invocation object representing the method call
	 * @param invocationContext the reflective invocation context of the method
	 * @param extensionContext the extension context of the test class or test method
	 * @throws Throwable if an error occurs during interception
	 */
	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		intercept(invocation, extensionContext);
	}

	/**
	 * Intercepts a test method invocation.
	 * @param invocation the invocation object representing the test method invocation
	 * @param invocationContext the reflective invocation context of the test method
	 * @param extensionContext the extension context of the test method
	 * @throws Throwable if an error occurs during the interception
	 */
	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		interceptMethod(invocation, invocationContext, extensionContext);
	}

	/**
	 * Intercepts the test template method.
	 * @param invocation the invocation object
	 * @param invocationContext the reflective invocation context
	 * @param extensionContext the extension context
	 * @throws Throwable if an error occurs during interception
	 */
	@Override
	public void interceptTestTemplateMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		interceptMethod(invocation, invocationContext, extensionContext);
	}

	/**
	 * Intercepts a method invocation and performs necessary actions based on the
	 * extension context. If the classpath class loader has been modified, the invocation
	 * proceeds without any modifications. Otherwise, it checks if the test class and
	 * method require a modified classpath class loader. If a modified classpath class
	 * loader is not required, the invocation proceeds without any modifications. If a
	 * modified classpath class loader is required, it skips the invocation and sets the
	 * modified classpath class loader as the current thread's context class loader. It
	 * then runs the test using the modified classpath class loader. Finally, it restores
	 * the original context class loader.
	 * @param invocation the invocation to be intercepted
	 * @param invocationContext the reflective invocation context of the method
	 * @param extensionContext the extension context of the test
	 * @throws Throwable if an error occurs during the interception or test execution
	 */
	private void interceptMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		if (isModifiedClassPathClassLoader(extensionContext)) {
			invocation.proceed();
			return;
		}
		Class<?> testClass = extensionContext.getRequiredTestClass();
		Method testMethod = invocationContext.getExecutable();
		URLClassLoader modifiedClassLoader = ModifiedClassPathClassLoader.get(testClass, testMethod,
				invocationContext.getArguments());
		if (modifiedClassLoader == null) {
			invocation.proceed();
			return;
		}
		invocation.skip();
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(modifiedClassLoader);
		try {
			runTest(extensionContext.getUniqueId());
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	/**
	 * Runs a test with the given test ID.
	 * @param testId the unique identifier of the test
	 * @throws Throwable if an error occurs during test execution
	 */
	private void runTest(String testId) throws Throwable {
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
			.selectors(DiscoverySelectors.selectUniqueId(testId))
			.build();
		Launcher launcher = LauncherFactory.create();
		TestPlan testPlan = launcher.discover(request);
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(listener);
		launcher.execute(testPlan);
		TestExecutionSummary summary = listener.getSummary();
		if (!CollectionUtils.isEmpty(summary.getFailures())) {
			throw summary.getFailures().get(0).getException();
		}
	}

	/**
	 * Intercepts the invocation and checks if the class path class loader has been
	 * modified. If the class path class loader has been modified, the invocation
	 * proceeds. Otherwise, the invocation is skipped.
	 * @param invocation the invocation to be intercepted
	 * @param extensionContext the extension context
	 * @throws Throwable if an error occurs during interception
	 */
	private void intercept(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
		if (isModifiedClassPathClassLoader(extensionContext)) {
			invocation.proceed();
			return;
		}
		invocation.skip();
	}

	/**
	 * Checks if the class loader used by the test class is an instance of
	 * ModifiedClassPathClassLoader.
	 * @param extensionContext the extension context of the test
	 * @return true if the class loader is an instance of ModifiedClassPathClassLoader,
	 * false otherwise
	 */
	private boolean isModifiedClassPathClassLoader(ExtensionContext extensionContext) {
		Class<?> testClass = extensionContext.getRequiredTestClass();
		ClassLoader classLoader = testClass.getClassLoader();
		return classLoader.getClass().getName().equals(ModifiedClassPathClassLoader.class.getName());
	}

}
