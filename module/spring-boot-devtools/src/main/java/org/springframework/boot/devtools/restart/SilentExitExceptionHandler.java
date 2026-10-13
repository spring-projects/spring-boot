/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;

/**
 * {@link UncaughtExceptionHandler} decorator that allows a thread to exit silently.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SilentExitExceptionHandler implements UncaughtExceptionHandler {

	private final @Nullable UncaughtExceptionHandler delegate;

	SilentExitExceptionHandler(@Nullable UncaughtExceptionHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		if (exception instanceof SilentExitException || (exception instanceof InvocationTargetException targetException
				&& targetException.getTargetException() instanceof SilentExitException)) {
			if (isJvmExiting(thread)) {
				preventNonZeroExitCode();
			}
			return;
		}
		if (this.delegate != null) {
			this.delegate.uncaughtException(thread, exception);
		}
	}

	private boolean isJvmExiting(Thread exceptionThread) {
		for (Thread thread : getAllThreads()) {
			if (thread != exceptionThread && thread.isAlive() && !thread.isDaemon()) {
				return false;
			}
		}
		return true;
	}

	protected Thread[] getAllThreads() {
		ThreadGroup rootThreadGroup = getRootThreadGroup();
		Thread[] threads = new Thread[32];
		int count = rootThreadGroup.enumerate(threads);
		while (count == threads.length) {
			threads = new Thread[threads.length * 2];
			count = rootThreadGroup.enumerate(threads);
		}
		return Arrays.copyOf(threads, count);
	}

	private ThreadGroup getRootThreadGroup() {
		ThreadGroup candidate = Thread.currentThread().getThreadGroup();
		while (candidate.getParent() != null) {
			candidate = candidate.getParent();
		}
		return candidate;
	}

	protected void preventNonZeroExitCode() {
		System.exit(0);
	}

	static void setup(Thread thread) {
		UncaughtExceptionHandler handler = thread.getUncaughtExceptionHandler();
		if (!(handler instanceof SilentExitExceptionHandler)) {
			handler = new SilentExitExceptionHandler(handler);
			thread.setUncaughtExceptionHandler(handler);
		}
	}

	static void exitCurrentThread() {
		throw new SilentExitException();
	}

	private static final class SilentExitException extends RuntimeException {

	}

}
