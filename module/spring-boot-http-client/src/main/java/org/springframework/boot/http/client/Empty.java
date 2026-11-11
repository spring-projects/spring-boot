/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.util.function.Consumer;

/**
 * Helper for empty functional interfaces.
 *
 * @author Phillip Webb
 */
final class Empty {

	private static final Consumer<?> EMPTY_CUSTOMIZER = (t) -> {
	};

	private Empty() {
	}

	@SuppressWarnings("unchecked")
	static <T> Consumer<T> consumer() {
		return (Consumer<T>) EMPTY_CUSTOMIZER;
	}

}
