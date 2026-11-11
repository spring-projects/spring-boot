/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.lang.reflect.Method;

/**
 * Base class for reflection based wrappers. Used to access internal Java classes without
 * needing tools.jar on the classpath.
 *
 * @author Phillip Webb
 */
class ReflectionWrapper {

	private final Class<?> type;

	private final Object instance;

	ReflectionWrapper(String type, Object instance) {
		this.type = findClass(instance.getClass().getClassLoader(), type);
		this.instance = this.type.cast(instance);
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
			return Class.forName(name, false, classLoader);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
		try {
			return type.getMethod(name, parameterTypes);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
