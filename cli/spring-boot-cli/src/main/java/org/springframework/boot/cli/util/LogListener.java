/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.util;

import org.jspecify.annotations.Nullable;

/**
 * Listener that can be attached to the {@link Log} to capture calls.
 *
 * @author Phillip Webb
 */
interface LogListener {

	void info(@Nullable String message);

	void infoPrint(@Nullable String message);

	void error(@Nullable String message);

	void error(Exception ex);

}
