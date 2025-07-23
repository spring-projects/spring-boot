/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates a test exception without too much stacktrace.
 *
 * @author Phillip Webb
 */
public final class TestException {

	private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile("\\.java\\:\\d+\\)");

	private TestException() {
	}

	public static Exception create() {
		CreatorThread creatorThread = new CreatorThread();
		creatorThread.start();
		try {
			creatorThread.join();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return creatorThread.exception;
	}

	private static Exception createTestException() {
		Throwable root = new RuntimeException("root");
		Throwable cause = createCause(root);
		Exception exception = createException(cause);
		exception.addSuppressed(new RuntimeException("suppressed"));
		return exception;
	}

	private static Throwable createCause(Throwable root) {
		return new RuntimeException("cause", root);
	}

	private static Exception createException(Throwable cause) {
		return actualCreateException(cause);
	}

	private static Exception actualCreateException(Throwable cause) {
		return new RuntimeException("exception", cause);
	}

	public static String withoutLineNumbers(String stackTrace) {
		Matcher matcher = LINE_NUMBER_PATTERN.matcher(stackTrace);
		return matcher.replaceAll(".java:NN)");
	}

	private static final class CreatorThread extends Thread {

		Exception exception;

		@Override
		public void run() {
			this.exception = createTestException();
		}

	}

}
