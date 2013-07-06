/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.zero.launcher;

import java.lang.reflect.Method;

/**
 * Utility class that used by {@link Launcher}s to call a main method. This class allows
 * methods to be executed within a thread configured with a specific context classloader.
 * 
 * @author Phillip Webb
 */
public class MainMethodRunner implements Runnable {

	private String mainClassName;

	private String[] args;

	/**
	 * Create a new {@link MainMethodRunner} instance.
	 * @param mainClass the main class
	 * @param args incoming arguments
	 */
	public MainMethodRunner(String mainClass, String[] args) {
		this.mainClassName = mainClass;
		this.args = args;
	}

	@Override
	public void run() {
		try {
			Class<?> mainClass = Thread.currentThread().getContextClassLoader()
					.loadClass(mainClassName);
			Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
			if (mainMethod == null) {
				throw new IllegalStateException(mainClassName
						+ " does not have a main method");
			}
			mainMethod.invoke(null, new Object[] { args });
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
