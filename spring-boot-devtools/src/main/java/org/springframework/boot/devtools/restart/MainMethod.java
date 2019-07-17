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

	MainMethod() {
		this(Thread.currentThread());
	}

	MainMethod(Thread thread) {
		Assert.notNull(thread, "Thread must not be null");
		this.method = getMainMethod(thread);
	}

	private Method getMainMethod(Thread thread) {
		for (StackTraceElement element : thread.getStackTrace()) {
			if ("main".equals(element.getMethodName())) {
				Method method = getMainMethod(element);
				if (method != null) {
					return method;
				}
			}
		}
		throw new IllegalStateException("Unable to find main method");
	}

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
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return the name of the declaring class.
	 * @return the declaring class name
	 */
	public String getDeclaringClassName() {
		return this.method.getDeclaringClass().getName();
	}

}
