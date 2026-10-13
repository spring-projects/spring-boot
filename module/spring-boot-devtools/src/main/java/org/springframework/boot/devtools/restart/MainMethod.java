/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jspecify.annotations.Nullable;

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
		Assert.notNull(thread, "'thread' must not be null");
		this.method = getMainMethod(thread);
	}

	private Method getMainMethod(Thread thread) {
		StackTraceElement[] stackTrace = thread.getStackTrace();
		for (int i = stackTrace.length - 1; i >= 0; i--) {
			StackTraceElement element = stackTrace[i];
			if ("main".equals(element.getMethodName()) && !isLoaderClass(element.getClassName())) {
				Method method = getMainMethod(element);
				if (method != null) {
					return method;
				}
			}
		}
		throw new IllegalStateException("Unable to find main method");
	}

	private boolean isLoaderClass(String className) {
		return className.startsWith("org.springframework.boot.loader.");
	}

	private @Nullable Method getMainMethod(StackTraceElement element) {
		try {
			Class<?> elementClass = Class.forName(element.getClassName());
			Method method = getMainMethod(elementClass);
			if (Modifier.isStatic(method.getModifiers())) {
				return method;
			}
		}
		catch (Exception ex) {
			// Ignore
		}
		return null;
	}

	private static Method getMainMethod(Class<?> clazz) throws Exception {
		try {
			return clazz.getDeclaredMethod("main", String[].class);
		}
		catch (NoSuchMethodException ex) {
			return clazz.getDeclaredMethod("main");
		}
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
