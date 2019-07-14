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

package org.springframework.boot.testsupport.runner.classpath;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.springframework.util.ReflectionUtils;

/**
 * A custom {@link Extension} that runs tests using a modified class path. Entries are
 * excluded from the class path using {@link ClassPathExclusions @ClassPathExclusions} and
 * overridden using {@link ClassPathOverrides @ClassPathOverrides} on the test class. A
 * class loader is created with the customized class path and is used both to load the
 * test class and as the thread context class loader while the test is being run.
 *
 * @author Christoph Dreis
 * @since 2.2.0
 */
public class ModifiedClassPathExtension implements InvocationInterceptor {

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		interceptInvocation(invocation, extensionContext);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		interceptInvocation(invocation, extensionContext);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		interceptInvocation(invocation, extensionContext);
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		interceptInvocation(invocation, extensionContext);
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		if (isModifiedClassPathClassLoader(extensionContext)) {
			invocation.proceed();
			return;
		}
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader classLoader = ModifiedClassPathClassLoaderFactory
				.createTestClassLoader(extensionContext.getRequiredTestClass());
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
			fakeInvocation(invocation);
			TestExecutionSummary summary = launchTests(invocationContext, extensionContext, classLoader);
			if (!CollectionUtils.isEmpty(summary.getFailures())) {
				throw summary.getFailures().get(0).getException();
			}
		}
		catch (Exception ex) {
			throw ex;
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private TestExecutionSummary launchTests(ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext, URLClassLoader classLoader) throws ClassNotFoundException {
		Class<?> testClass = classLoader.loadClass(extensionContext.getRequiredTestClass().getName());
		Method method = ReflectionUtils.findMethod(testClass, invocationContext.getExecutable().getName());
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(DiscoverySelectors.selectMethod(testClass, method)).build();
		Launcher launcher = LauncherFactory.create();
		TestPlan testPlan = launcher.discover(request);
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(listener);
		launcher.execute(testPlan);
		return listener.getSummary();
	}

	private boolean isModifiedClassPathClassLoader(ExtensionContext extensionContext) {
		return extensionContext.getRequiredTestClass().getClassLoader().getClass().getName()
				.equals(ModifiedClassPathClassLoader.class.getName());
	}

	private void interceptInvocation(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
		if (isModifiedClassPathClassLoader(extensionContext)) {
			invocation.proceed();
		}
		else {
			fakeInvocation(invocation);
		}
	}

	private void fakeInvocation(Invocation invocation) {
		try {
			Field field = ReflectionUtils.findField(invocation.getClass(), "invoked");
			ReflectionUtils.makeAccessible(field);
			ReflectionUtils.setField(field, invocation, new AtomicBoolean(true));
		}
		catch (Throwable ignore) {

		}
	}

}
