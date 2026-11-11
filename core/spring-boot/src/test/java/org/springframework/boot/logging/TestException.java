/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

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
		Exception exception = creatorThread.exception;
		assertThat(exception).isNotNull();
		return exception;
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

		@Nullable Exception exception;

		@Override
		public void run() {
			this.exception = createTestException();
		}

	}

}
