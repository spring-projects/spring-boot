/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.structured;

import org.jspecify.annotations.Nullable;

/**
 * Test access to {@link ContextPairs}.
 *
 * @author Phillip Webb
 */
public final class TestContextPairs {

	private TestContextPairs() {
	}

	public static ContextPairs include() {
		return of(true, null);
	}

	public static ContextPairs of(boolean include, @Nullable String prefix) {
		return new ContextPairs(include, prefix);
	}

}
