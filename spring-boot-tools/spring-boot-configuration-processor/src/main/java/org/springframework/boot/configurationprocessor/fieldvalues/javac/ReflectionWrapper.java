/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.lang.reflect.Method;

/**
 * Base class for reflection based wrappers. Used to access internal Java classes without
 * needing tools.jar on the classpath.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
class ReflectionWrapper {

	private final Class<?> type;

	private final Object instance;

	ReflectionWrapper(Object instance) {
		this.type = instance.getClass();
		this.instance = instance;
	}

	ReflectionWrapper(String type, Object instance) {
		this.type = findClass(instance.getClass().getClassLoader(), type);
		this.instance = instance;
	}

	protected final Object getInstance() {
		return this.instance;
	}

	@Override
	public String toString() {
		return this.instance.toString();
	}

	protected Class<?> findClass(String name) {
		return findClass(getInstance().getClass().getClassLoader(), name);
	}

	protected Method findMethod(String name, Class<?>... parameterTypes) {
		return findMethod(this.type, name, parameterTypes);
	}

	protected static Class<?> findClass(ClassLoader classLoader, String name) {
		try {
			return classLoader.loadClass(name);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected static Method findMethod(Class<?> type, String name,
			Class<?>... parameterTypes) {
		try {
			return type.getMethod(name, parameterTypes);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
