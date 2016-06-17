/*
 * Copyright 2012-2016 the original author or authors.
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
import java.util.Arrays;

/**
 * {@link UncaughtExceptionHandler} decorator that allows a thread to exit silently.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SilentExitExceptionHandler implements UncaughtExceptionHandler {

	private final UncaughtExceptionHandler delegate;

	SilentExitExceptionHandler(UncaughtExceptionHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		if (exception instanceof SilentExitException) {
			if (jvmWillExit(thread)) {
				preventNonZeroExitCode();
			}
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

	private boolean jvmWillExit(Thread exceptionThread) {
		for (Thread thread : getAllThreads()) {
			if (thread != exceptionThread && thread.isAlive() && !thread.isDaemon()) {
				return false;
			}
		}
		return true;
	}

	protected void preventNonZeroExitCode() {
		System.exit(0);
	}

	protected Thread[] getAllThreads() {
		ThreadGroup rootThreadGroup = getRootThreadGroup();
		int size = 32;
		int threadCount;
		Thread[] threads;
		do {
			size *= 2;
			threads = new Thread[size];
			threadCount = rootThreadGroup.enumerate(threads);
		}
		while (threadCount == threads.length);
		return Arrays.copyOf(threads, threadCount);
	}

	private ThreadGroup getRootThreadGroup() {
		ThreadGroup candidate = Thread.currentThread().getThreadGroup();
		while (candidate.getParent() != null) {
			candidate = candidate.getParent();
		}
		return candidate;
	}

	private static class SilentExitException extends RuntimeException {

	}

}
