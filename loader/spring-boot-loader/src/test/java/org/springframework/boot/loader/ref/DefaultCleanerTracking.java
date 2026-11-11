/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.ref;

import java.lang.ref.Cleaner.Cleanable;
import java.util.function.BiConsumer;

/**
 * Utility that allows tests to set a tracker on {@link DefaultCleaner}.
 *
 * @author Phillip Webb
 */
public final class DefaultCleanerTracking {

	private DefaultCleanerTracking() {
	}

	public static void set(BiConsumer<Object, Cleanable> tracker) {
		DefaultCleaner.tracker = tracker;
	}

}
