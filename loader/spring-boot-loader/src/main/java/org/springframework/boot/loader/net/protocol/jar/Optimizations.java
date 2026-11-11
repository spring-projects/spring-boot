/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

/**
 * {@link ThreadLocal} state for {@link Handler} optimizations.
 *
 * @author Phillip Webb
 */
final class Optimizations {

	private static final ThreadLocal<Boolean> status = new ThreadLocal<>();

	private Optimizations() {
	}

	static void enable(boolean readContents) {
		status.set(readContents);
	}

	static void disable() {
		status.remove();
	}

	static boolean isEnabled() {
		return status.get() != null;
	}

	static boolean isEnabled(boolean readContents) {
		return Boolean.valueOf(readContents).equals(status.get());
	}

}
