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

package org.springframework.boot.devtools.restart;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * {@link UncaughtExceptionHandler} decorator that allows a thread to exit silently.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SilentExitExceptionHandler implements UncaughtExceptionHandler {

	private final UncaughtExceptionHandler delegate;

	/**
     * Constructs a new SilentExitExceptionHandler with the specified delegate UncaughtExceptionHandler.
     * 
     * @param delegate the delegate UncaughtExceptionHandler to be used
     */
    SilentExitExceptionHandler(UncaughtExceptionHandler delegate) {
		this.delegate = delegate;
	}

	/**
     * Handles uncaught exceptions in a thread.
     * 
     * @param thread    the thread in which the exception occurred
     * @param exception the uncaught exception
     */
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

	/**
     * Checks if the Java Virtual Machine (JVM) is exiting.
     * 
     * @param exceptionThread the thread that caused the exception
     * @return true if the JVM is exiting, false otherwise
     */
    private boolean isJvmExiting(Thread exceptionThread) {
		for (Thread thread : getAllThreads()) {
			if (thread != exceptionThread && thread.isAlive() && !thread.isDaemon()) {
				return false;
			}
		}
		return true;
	}

	/**
     * Returns an array of all currently active threads in the system.
     * 
     * @return an array of Thread objects representing all currently active threads
     */
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

	/**
     * Returns the root thread group of the current thread.
     * 
     * @return the root thread group
     */
    private ThreadGroup getRootThreadGroup() {
		ThreadGroup candidate = Thread.currentThread().getThreadGroup();
		while (candidate.getParent() != null) {
			candidate = candidate.getParent();
		}
		return candidate;
	}

	/**
     * This method is used to prevent a non-zero exit code from being returned.
     * It calls the System.exit() method with a parameter of 0, which terminates the Java Virtual Machine.
     * This ensures that the program exits with a successful status code.
     */
    protected void preventNonZeroExitCode() {
		System.exit(0);
	}

	/**
     * Sets up the provided thread with a custom uncaught exception handler.
     * If the current uncaught exception handler is not an instance of SilentExitExceptionHandler,
     * it wraps it with a SilentExitExceptionHandler and sets it as the new uncaught exception handler for the thread.
     * 
     * @param thread the thread to set up with the custom uncaught exception handler
     */
    static void setup(Thread thread) {
		UncaughtExceptionHandler handler = thread.getUncaughtExceptionHandler();
		if (!(handler instanceof SilentExitExceptionHandler)) {
			handler = new SilentExitExceptionHandler(handler);
			thread.setUncaughtExceptionHandler(handler);
		}
	}

	/**
     * Throws a {@link SilentExitException} to exit the current thread.
     */
    static void exitCurrentThread() {
		throw new SilentExitException();
	}

	/**
     * SilentExitException class.
     */
    private static final class SilentExitException extends RuntimeException {

	}

}
