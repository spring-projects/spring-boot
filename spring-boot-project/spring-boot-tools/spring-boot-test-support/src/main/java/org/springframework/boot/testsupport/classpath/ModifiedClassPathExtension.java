/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.boot.testsupport.junit.platform.Launcher;
import org.springframework.boot.testsupport.junit.platform.LauncherDiscoveryRequest;
import org.springframework.boot.testsupport.junit.platform.LauncherDiscoveryRequestBuilder;
import org.springframework.boot.testsupport.junit.platform.SummaryGeneratingListener;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * A custom {@link Extension} that runs tests using a modified class path. Entries are
 * excluded from the class path using {@link ClassPathExclusions @ClassPathExclusions} and
 * overridden using {@link ClassPathOverrides @ClassPathOverrides} on the test class. A
 * class loader is created with the customized class path and is used both to load the
 * test class and as the thread context class loader while the test is being run.
 *
 * @author Christoph Dreis
 */
class ModifiedClassPathExtension implements InvocationInterceptor {

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
		invocation.skip();
		runTestWithModifiedClassPath(invocationContext, extensionContext);
	}

	private void runTestWithModifiedClassPath(ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		Class<?> testClass = extensionContext.getRequiredTestClass();
		Method testMethod = invocationContext.getExecutable();
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader modifiedClassLoader = ModifiedClassPathClassLoader.get(testClass);
		Thread.currentThread().setContextClassLoader(modifiedClassLoader);
		try {
			runTest(modifiedClassLoader, testClass.getName(), testMethod.getName());
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private void runTest(ClassLoader classLoader, String testClassName, String testMethodName) throws Throwable {
		Class<?> testClass = Class.forName(testClassName, false, classLoader);
		Method testMethod = findMethod(testClass, testMethodName);
		LauncherDiscoveryRequest request = new LauncherDiscoveryRequestBuilder(classLoader)
				.selectors(DiscoverySelectors.selectMethod(testClass, testMethod)).build();
		Launcher launcher = new Launcher(classLoader);
		SummaryGeneratingListener listener = new SummaryGeneratingListener(classLoader);
		launcher.registerTestExecutionListeners(listener);
		launcher.execute(request);
		Throwable failure = listener.getSummary().getFailure();
		if (failure != null) {
			throw failure;
		}
	}

	private Method findMethod(Class<?> testClass, String testMethodName) {
		Method method = ReflectionUtils.findMethod(testClass, testMethodName);
		if (method == null) {
			Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(testClass);
			for (Method candidate : methods) {
				if (candidate.getName().equals(testMethodName)) {
					return candidate;
				}
			}
		}
		Assert.state(method != null, () -> "Unable to find " + testClass + "." + testMethodName);
		return method;
	}

	private void intercept(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
		if (isModifiedClassPathClassLoader(extensionContext)) {
			invocation.proceed();
			return;
		}
		invocation.skip();
	}

	private boolean isModifiedClassPathClassLoader(ExtensionContext extensionContext) {
		Class<?> testClass = extensionContext.getRequiredTestClass();
		ClassLoader classLoader = testClass.getClassLoader();
		return classLoader.getClass().getName().equals(ModifiedClassPathClassLoader.class.getName());
	}

}
