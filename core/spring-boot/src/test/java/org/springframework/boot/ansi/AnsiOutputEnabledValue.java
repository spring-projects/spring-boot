/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ansi;

import org.springframework.boot.ansi.AnsiOutput.Enabled;

/**
 * Public access to {@link AnsiOutput#getEnabled()} for other tests to use.
 *
 * @author Phillip Webb
 */
public final class AnsiOutputEnabledValue {

	private AnsiOutputEnabledValue() {
	}

	public static Enabled get() {
		return AnsiOutput.getEnabled();
	}

}
