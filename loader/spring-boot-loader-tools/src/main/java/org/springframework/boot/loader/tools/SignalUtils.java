/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import sun.misc.Signal;

/**
 * Utilities for working with signal handling.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
public final class SignalUtils {

	private static final Signal SIG_INT = new Signal("INT");

	private SignalUtils() {
	}

	/**
	 * Handle {@literal INT} signals by calling the specified {@link Runnable}.
	 * @param runnable the runnable to call on SIGINT.
	 */
	public static void attachSignalHandler(Runnable runnable) {
		Signal.handle(SIG_INT, (signal) -> runnable.run());
	}

}
