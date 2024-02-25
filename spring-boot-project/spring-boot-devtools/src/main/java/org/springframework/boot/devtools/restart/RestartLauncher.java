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

package org.springframework.boot.devtools.restart;

import java.lang.reflect.Method;

/**
 * Thread used to launch a restarted application.
 *
 * @author Phillip Webb
 */
class RestartLauncher extends Thread {

	private final String mainClassName;

	private final String[] args;

	private Throwable error;

	/**
	 * Restarts the launcher with the specified class loader, main class name, arguments,
	 * and exception handler.
	 * @param classLoader the class loader to use for restarting the launcher
	 * @param mainClassName the fully qualified name of the main class to be executed
	 * @param args the command line arguments to be passed to the main class
	 * @param exceptionHandler the uncaught exception handler to be set for the restarted
	 * main thread
	 */
	RestartLauncher(ClassLoader classLoader, String mainClassName, String[] args,
			UncaughtExceptionHandler exceptionHandler) {
		this.mainClassName = mainClassName;
		this.args = args;
		setName("restartedMain");
		setUncaughtExceptionHandler(exceptionHandler);
		setDaemon(false);
		setContextClassLoader(classLoader);
	}

	/**
	 * This method is responsible for running the main method of a specified class. It
	 * uses reflection to dynamically load the class and invoke its main method.
	 * @throws Throwable if an error occurs during the execution of the main method
	 */
	@Override
	public void run() {
		try {
			Class<?> mainClass = Class.forName(this.mainClassName, false, getContextClassLoader());
			Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
			mainMethod.setAccessible(true);
			mainMethod.invoke(null, new Object[] { this.args });
		}
		catch (Throwable ex) {
			this.error = ex;
			getUncaughtExceptionHandler().uncaughtException(this, ex);
		}
	}

	/**
	 * Returns the error associated with the RestartLauncher object.
	 * @return the error associated with the RestartLauncher object
	 */
	Throwable getError() {
		return this.error;
	}

}
