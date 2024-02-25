/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link UncaughtExceptionHandler} to suppress handling already logged exceptions and
 * dealing with system exit.
 *
 * @author Phillip Webb
 */
class SpringBootExceptionHandler implements UncaughtExceptionHandler {

	private static final Set<String> LOG_CONFIGURATION_MESSAGES;

	static {
		Set<String> messages = new HashSet<>();
		messages.add("Logback configuration error detected");
		LOG_CONFIGURATION_MESSAGES = Collections.unmodifiableSet(messages);
	}

	private static final LoggedExceptionHandlerThreadLocal handler = new LoggedExceptionHandlerThreadLocal();

	private final UncaughtExceptionHandler parent;

	private final List<Throwable> loggedExceptions = new ArrayList<>();

	private int exitCode = 0;

	/**
	 * Constructs a new SpringBootExceptionHandler with the specified parent
	 * UncaughtExceptionHandler.
	 * @param parent the parent UncaughtExceptionHandler to set
	 */
	SpringBootExceptionHandler(UncaughtExceptionHandler parent) {
		this.parent = parent;
	}

	/**
	 * Registers a logged exception.
	 * @param exception the exception to be logged
	 */
	void registerLoggedException(Throwable exception) {
		this.loggedExceptions.add(exception);
	}

	/**
	 * Registers the exit code for the application.
	 * @param exitCode the exit code to be registered
	 */
	void registerExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	/**
	 * Handles uncaught exceptions in a Spring Boot application.
	 * @param thread The thread in which the exception occurred.
	 * @param ex The throwable object representing the uncaught exception.
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		try {
			if (isPassedToParent(ex) && this.parent != null) {
				this.parent.uncaughtException(thread, ex);
			}
		}
		finally {
			this.loggedExceptions.clear();
			if (this.exitCode != 0) {
				System.exit(this.exitCode);
			}
		}
	}

	/**
	 * Checks if the given exception is passed to the parent handler.
	 * @param ex the exception to be checked
	 * @return true if the exception is a log configuration message or not registered,
	 * false otherwise
	 */
	private boolean isPassedToParent(Throwable ex) {
		return isLogConfigurationMessage(ex) || !isRegistered(ex);
	}

	/**
	 * Check if the exception is a log configuration message, i.e. the log call might not
	 * have actually output anything.
	 * @param ex the source exception
	 * @return {@code true} if the exception contains a log configuration message
	 */
	private boolean isLogConfigurationMessage(Throwable ex) {
		if (ex instanceof InvocationTargetException) {
			return isLogConfigurationMessage(ex.getCause());
		}
		String message = ex.getMessage();
		if (message != null) {
			for (String candidate : LOG_CONFIGURATION_MESSAGES) {
				if (message.contains(candidate)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the given exception is registered in the loggedExceptions list.
	 * @param ex the exception to check
	 * @return true if the exception is registered, false otherwise
	 */
	private boolean isRegistered(Throwable ex) {
		if (this.loggedExceptions.contains(ex)) {
			return true;
		}
		if (ex instanceof InvocationTargetException) {
			return isRegistered(ex.getCause());
		}
		return false;
	}

	/**
	 * Returns the SpringBootExceptionHandler instance associated with the current thread.
	 * @return the SpringBootExceptionHandler instance associated with the current thread
	 */
	static SpringBootExceptionHandler forCurrentThread() {
		return handler.get();
	}

	/**
	 * Thread local used to attach and track handlers.
	 */
	private static final class LoggedExceptionHandlerThreadLocal extends ThreadLocal<SpringBootExceptionHandler> {

		/**
		 * Returns the initial value for the LoggedExceptionHandlerThreadLocal.
		 *
		 * This method creates a new instance of SpringBootExceptionHandler and sets it as
		 * the uncaught exception handler for the current thread.
		 * @return the initial value for the LoggedExceptionHandlerThreadLocal
		 */
		@Override
		protected SpringBootExceptionHandler initialValue() {
			SpringBootExceptionHandler handler = new SpringBootExceptionHandler(
					Thread.currentThread().getUncaughtExceptionHandler());
			Thread.currentThread().setUncaughtExceptionHandler(handler);
			return handler;
		}

	}

}
