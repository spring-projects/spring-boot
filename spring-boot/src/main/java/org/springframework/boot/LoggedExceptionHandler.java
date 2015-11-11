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
import java.util.List;

/**
 * {@link UncaughtExceptionHandler} to suppress handling already logged exceptions.
 *
 * @author Phillip Webb
 */
class LoggedExceptionHandler implements UncaughtExceptionHandler {

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
			if (!isRegistered(ex) && this.parent != null) {
				this.parent.uncaughtException(thread, ex);
			}
		}
		finally {
			this.exceptions.clear();
		}
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
