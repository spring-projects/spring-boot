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
		intercept(invocation, extensionContext);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		intercept(invocation, extensionContext);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		intercept(invocation, extensionContext);
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		intercept(invocation, extensionContext);
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		if (isModifiedClassPathClassLoader(extensionContext)) {
			invocation.proceed();
			return;
		}
		fakeInvocation(invocation);
		runTestWithModifiedClassPath(invocationContext, extensionContext);
	}

	private void runTestWithModifiedClassPath(ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws ClassNotFoundException, Throwable {
		Class<?> testClass = extensionContext.getRequiredTestClass();
		Method testMethod = invocationContext.getExecutable();
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader modifiedClassLoader = ModifiedClassPathClassLoaderFactory.createTestClassLoader(testClass);
		Thread.currentThread().setContextClassLoader(modifiedClassLoader);
		try {
			runTest(modifiedClassLoader, testClass.getName(), testMethod.getName());
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private void runTest(URLClassLoader classLoader, String testClassName, String testMethodName)
			throws ClassNotFoundException, Throwable {
		Class<?> testClass = classLoader.loadClass(testClassName);
		Method testMethod = ReflectionUtils.findMethod(testClass, testMethodName);
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(DiscoverySelectors.selectMethod(testClass, testMethod)).build();
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

	private void intercept(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
		if (isModifiedClassPathClassLoader(extensionContext)) {
			invocation.proceed();
			return;
		}
		fakeInvocation(invocation);
	}

	private void fakeInvocation(Invocation<Void> invocation) {
		try {
			Field field = ReflectionUtils.findField(invocation.getClass(), "invoked");
			ReflectionUtils.makeAccessible(field);
			ReflectionUtils.setField(field, invocation, new AtomicBoolean(true));
		}
		catch (Throwable ex) {
		}
	}

	private boolean isModifiedClassPathClassLoader(ExtensionContext extensionContext) {
		Class<?> testClass = extensionContext.getRequiredTestClass();
		ClassLoader classLoader = testClass.getClassLoader();
		return classLoader.getClass().getName().equals(ModifiedClassPathClassLoader.class.getName());
	}

}
