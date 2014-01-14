/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli.command.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.groovy.DelegateTestRunner;

/**
 * Compile and run groovy based tests.
 * 
 * @author Phillip Webb
 */
public class TestRunner {

	private static final String JUNIT_TEST_ANNOTATION = "org.junit.Test";

	private final String[] sources;

	private final GroovyCompiler compiler;

	/**
	 * Create a new {@link TestRunner} instance.
	 * @param configuration
	 * @param sources
	 * @param args
	 */
	public TestRunner(TestRunnerConfiguration configuration, String[] sources,
			String[] args) {
		this.sources = sources.clone();
		this.compiler = new GroovyCompiler(configuration);
	}

	public void compileAndRunTests() throws Exception {
		Object[] sources = this.compiler.sources(this.sources);
		if (sources.length == 0) {
			throw new RuntimeException("No classes found in '" + this.sources + "'");
		}

		// Run in new thread to ensure that the context classloader is setup
		RunThread runThread = new RunThread(sources);
		runThread.start();
		runThread.join();
	}

	/**
	 * Thread used to launch the Spring Application with the correct context classloader.
	 */
	private class RunThread extends Thread {

		private final Class<?>[] testClasses;

		private final Class<?> spockSpecificationClass;

		/**
		 * Create a new {@link RunThread} instance.
		 * @param sources the sources to launch
		 */
		public RunThread(Object... sources) {
			super("testrunner");
			setDaemon(true);
			if (sources.length != 0 && sources[0] instanceof Class) {
				setContextClassLoader(((Class<?>) sources[0]).getClassLoader());
			}
			this.spockSpecificationClass = loadSpockSpecificationClass(getContextClassLoader());
			this.testClasses = getTestClasses(sources);
		}

		private Class<?> loadSpockSpecificationClass(ClassLoader contextClassLoader) {
			try {
				return getContextClassLoader().loadClass("spock.lang.Specification");
			}
			catch (Exception ex) {
				return null;
			}
		}

		private Class<?>[] getTestClasses(Object[] sources) {
			List<Class<?>> testClasses = new ArrayList<Class<?>>();
			for (Object source : sources) {
				if ((source instanceof Class) && isTestable((Class<?>) source)) {
					testClasses.add((Class<?>) source);
				}
			}
			return testClasses.toArray(new Class<?>[testClasses.size()]);
		}

		private boolean isTestable(Class<?> sourceClass) {
			return (isJunitTest(sourceClass) || isSpockTest(sourceClass));
		}

		private boolean isJunitTest(Class<?> sourceClass) {
			for (Method method : sourceClass.getMethods()) {
				for (Annotation annotation : method.getAnnotations()) {
					if (annotation.annotationType().getName()
							.equals(JUNIT_TEST_ANNOTATION)) {
						return true;
					}
				}
			}
			return false;
		}

		private boolean isSpockTest(Class<?> sourceClass) {
			return (this.spockSpecificationClass != null && this.spockSpecificationClass
					.isAssignableFrom(sourceClass));
		}

		@Override
		public void run() {
			try {
				if (this.testClasses.length == 0) {
					System.out.println("No tests found");
				}
				else {
					Class<?> delegateClass = Thread.currentThread()
							.getContextClassLoader()
							.loadClass(DelegateTestRunner.class.getName());
					Method runMethod = delegateClass.getMethod("run", Class[].class);
					runMethod.invoke(null, new Object[] { this.testClasses });
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}
