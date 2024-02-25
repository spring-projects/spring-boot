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
import java.lang.reflect.Modifier;

import org.springframework.util.Assert;

/**
 * The "main" method located from a running thread.
 *
 * @author Phillip Webb
 */
class MainMethod {

	private final Method method;

	/**
     * This method is the entry point of the MainMethod class.
     * It creates an instance of the MainMethod class and passes the current thread as a parameter.
     * 
     * @param thread the current thread
     */
    MainMethod() {
		this(Thread.currentThread());
	}

	/**
     * Sets the main method for the specified thread.
     * 
     * @param thread the thread for which the main method is to be set (must not be null)
     * @throws IllegalArgumentException if the thread is null
     */
    MainMethod(Thread thread) {
		Assert.notNull(thread, "Thread must not be null");
		this.method = getMainMethod(thread);
	}

	/**
     * Returns the main method of a given thread.
     * 
     * @param thread the thread to search for the main method
     * @return the main method of the given thread
     * @throws IllegalStateException if the main method cannot be found
     */
    private Method getMainMethod(Thread thread) {
		StackTraceElement[] stackTrace = thread.getStackTrace();
		for (int i = stackTrace.length - 1; i >= 0; i--) {
			StackTraceElement element = stackTrace[i];
			if ("main".equals(element.getMethodName())) {
				Method method = getMainMethod(element);
				if (method != null) {
					return method;
				}
			}
		}
		throw new IllegalStateException("Unable to find main method");
	}

	/**
     * Retrieves the main method from the given StackTraceElement.
     * 
     * @param element The StackTraceElement representing the method.
     * @return The main method if found and static, null otherwise.
     */
    private Method getMainMethod(StackTraceElement element) {
		try {
			Class<?> elementClass = Class.forName(element.getClassName());
			Method method = elementClass.getDeclaredMethod("main", String[].class);
			if (Modifier.isStatic(method.getModifiers())) {
				return method;
			}
		}
		catch (Exception ex) {
			// Ignore
		}
		return null;
	}

	/**
	 * Returns the actual main method.
	 * @return the main method
	 */
	Method getMethod() {
		return this.method;
	}

	/**
	 * Return the name of the declaring class.
	 * @return the declaring class name
	 */
	String getDeclaringClassName() {
		return this.method.getDeclaringClass().getName();
	}

}
