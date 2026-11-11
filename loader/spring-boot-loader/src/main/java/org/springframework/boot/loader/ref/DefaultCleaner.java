/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.ref;

import java.lang.ref.Cleaner.Cleanable;
import java.util.function.BiConsumer;

/**
 * Default {@link Cleaner} implementation that delegates to {@link java.lang.ref.Cleaner}.
 *
 * @author Phillip Webb
 */
class DefaultCleaner implements Cleaner {

	static final DefaultCleaner instance = new DefaultCleaner();

	static BiConsumer<Object, Cleanable> tracker;

	private final java.lang.ref.Cleaner cleaner = java.lang.ref.Cleaner.create();

	@Override
	public Cleanable register(Object obj, Runnable action) {
		Cleanable cleanable = (action != null) ? this.cleaner.register(obj, action) : null;
		if (tracker != null) {
			tracker.accept(obj, cleanable);
		}
		return cleanable;
	}

}
