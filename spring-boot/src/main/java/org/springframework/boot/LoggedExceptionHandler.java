/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link UncaughtExceptionHandler} to suppress handling already logged exceptions.
 *
 * @author Phillip Webb
 */
class LoggedExceptionHandler implements UncaughtExceptionHandler {

	private static Set<String> LOG_CONFIGURATION_MESSAGES;

	static {
		Set<String> messages = new HashSet<String>();
		messages.add("Logback configuration error detected");
		LOG_CONFIGURATION_MESSAGES = Collections.unmodifiableSet(messages);
	}

	private static LoggedExceptionHandlerThreadLocal handler = new LoggedExceptionHandlerThreadLocal();

	private final UncaughtExceptionHandler parent;

	private final List<Throwable> exceptions = new ArrayList<Throwable>();

	LoggedExceptionHandler(UncaughtExceptionHandler parent) {
		this.parent = parent;
	}

	public void register(Throwable exception) {
		this.exceptions.add(exception);
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		try {
			if (isPassedToParent(ex) && this.parent != null) {
				this.parent.uncaughtException(thread, ex);
			}
		}
		finally {
			this.exceptions.clear();
		}
	}

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

	private boolean isRegistered(Throwable ex) {
		if (this.exceptions.contains(ex)) {
			return true;
		}
		if (ex instanceof InvocationTargetException) {
			return isRegistered(ex.getCause());
		}
		return false;
	}

	static LoggedExceptionHandler forCurrentThread() {
		return handler.get();
	}

	/**
	 * Thread local used to attach and track handlers.
	 */
	private static class LoggedExceptionHandlerThreadLocal
			extends ThreadLocal<LoggedExceptionHandler> {

		@Override
		protected LoggedExceptionHandler initialValue() {
			LoggedExceptionHandler handler = new LoggedExceptionHandler(
					Thread.currentThread().getUncaughtExceptionHandler());
			Thread.currentThread().setUncaughtExceptionHandler(handler);
			return handler;
		};

	}

}
