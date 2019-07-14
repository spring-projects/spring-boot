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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

/**
 * A custom {@link BlockJUnit4ClassRunner} that runs tests using a modified class path.
 * Entries are excluded from the class path using
 * {@link ClassPathExclusions @ClassPathExclusions} and overridden using
 * {@link ClassPathOverrides @ClassPathOverrides} on the test class. A class loader is
 * created with the customized class path and is used both to load the test class and as
 * the thread context class loader while the test is being run.
 *
 * @author Andy Wilkinson
 * @since 1.5.0
 */
public class ModifiedClassPathRunner extends BlockJUnit4ClassRunner {

	public ModifiedClassPathRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	@Override
	protected TestClass createTestClass(Class<?> testClass) {
		try {
			ClassLoader classLoader = ModifiedClassPathClassLoaderFactory.createTestClassLoader(testClass);
			return new ModifiedClassPathTestClass(classLoader, testClass.getName());
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	protected Object createTest() throws Exception {
		ModifiedClassPathTestClass testClass = (ModifiedClassPathTestClass) getTestClass();
		return testClass
				.doWithModifiedClassPathThreadContextClassLoader(() -> ModifiedClassPathRunner.super.createTest());
	}

	/**
	 * Custom {@link TestClass} that uses a modified class path.
	 */
	private static final class ModifiedClassPathTestClass extends TestClass {

		private final ClassLoader classLoader;

		ModifiedClassPathTestClass(ClassLoader classLoader, String testClassName) throws ClassNotFoundException {
			super(classLoader.loadClass(testClassName));
			this.classLoader = classLoader;
		}

		@Override
		public List<FrameworkMethod> getAnnotatedMethods(Class<? extends Annotation> annotationClass) {
			try {
				return getAnnotatedMethods(annotationClass.getName());
			}
			catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}

		@SuppressWarnings("unchecked")
		private List<FrameworkMethod> getAnnotatedMethods(String annotationClassName) throws ClassNotFoundException {
			Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) this.classLoader
					.loadClass(annotationClassName);
			List<FrameworkMethod> methods = super.getAnnotatedMethods(annotationClass);
			return wrapFrameworkMethods(methods);
		}

		private List<FrameworkMethod> wrapFrameworkMethods(List<FrameworkMethod> methods) {
			List<FrameworkMethod> wrapped = new ArrayList<>(methods.size());
			for (FrameworkMethod frameworkMethod : methods) {
				wrapped.add(new ModifiedClassPathFrameworkMethod(frameworkMethod.getMethod()));
			}
			return wrapped;
		}

		private <T, E extends Throwable> T doWithModifiedClassPathThreadContextClassLoader(
				ModifiedClassPathTcclAction<T, E> action) throws E {
			ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(this.classLoader);
			try {
				return action.perform();
			}
			finally {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}

		/**
		 * An action to be performed with the {@link ModifiedClassPathClassLoader} set as
		 * the thread context class loader.
		 */
		private interface ModifiedClassPathTcclAction<T, E extends Throwable> {

			T perform() throws E;

		}

		/**
		 * Custom {@link FrameworkMethod} that runs methods with
		 * {@link ModifiedClassPathClassLoader} as the thread context class loader.
		 */
		private final class ModifiedClassPathFrameworkMethod extends FrameworkMethod {

			private ModifiedClassPathFrameworkMethod(Method method) {
				super(method);
			}

			@Override
			public Object invokeExplosively(Object target, Object... params) throws Throwable {
				return doWithModifiedClassPathThreadContextClassLoader(
						() -> ModifiedClassPathFrameworkMethod.super.invokeExplosively(target, params));
			}

		}

	}

}
