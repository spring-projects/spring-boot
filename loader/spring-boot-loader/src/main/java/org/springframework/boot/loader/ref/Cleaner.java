/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.ref;

import java.lang.ref.Cleaner.Cleanable;

/**
 * Wrapper for {@link java.lang.ref.Cleaner} providing registration support.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public interface Cleaner {

	/**
	 * Provides access to the default clean instance which delegates to
	 * {@link java.lang.ref.Cleaner}.
	 */
	Cleaner instance = DefaultCleaner.instance;

	/**
	 * Registers an object and the clean action to run when the object becomes phantom
	 * reachable.
	 * @param obj the object to monitor
	 * @param action the cleanup action to run
	 * @return a {@link Cleanable} instance
	 * @see java.lang.ref.Cleaner#register(Object, Runnable)
	 */
	Cleanable register(Object obj, Runnable action);

}
