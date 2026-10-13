/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * Thread used to launch a restarted application.
 *
 * @author Phillip Webb
 */
class RestartLauncher extends Thread {

	private final String mainClassName;

	private final String[] args;

	private @Nullable Throwable error;

	RestartLauncher(ClassLoader classLoader, String mainClassName, String[] args,
			UncaughtExceptionHandler exceptionHandler) {
		this.mainClassName = mainClassName;
		this.args = args;
		setName("restartedMain");
		setUncaughtExceptionHandler(exceptionHandler);
		setDaemon(false);
		setContextClassLoader(classLoader);
	}

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

	@Nullable Throwable getError() {
		return this.error;
	}

}
