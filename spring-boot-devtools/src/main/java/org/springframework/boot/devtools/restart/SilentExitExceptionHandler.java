/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * {@link UncaughtExceptionHandler} decorator that allows a thread to exit silently.
 *
 * @author Phillip Webb
 */
class SilentExitExceptionHandler implements UncaughtExceptionHandler {

	private final UncaughtExceptionHandler delegate;

	public SilentExitExceptionHandler(UncaughtExceptionHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		if (exception instanceof SilentExitException) {
			return;
		}
		if (this.delegate != null) {
			this.delegate.uncaughtException(thread, exception);
		}
	}

	public static void setup(Thread thread) {
		UncaughtExceptionHandler handler = thread.getUncaughtExceptionHandler();
		if (!(handler instanceof SilentExitExceptionHandler)) {
			handler = new SilentExitExceptionHandler(handler);
			thread.setUncaughtExceptionHandler(handler);
		}
	}

	public static void exitCurrentThread() {
		throw new SilentExitException();
	}

	private static class SilentExitException extends RuntimeException {

	}

}
