/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import org.jspecify.annotations.Nullable;

/**
 * Format System properties.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
final class SystemPropertyFormatter {

	private SystemPropertyFormatter() {
	}

	static String format(@Nullable String key, @Nullable String value) {
		if (key == null) {
			return "";
		}
		if (value == null || value.isEmpty()) {
			return String.format("-D%s", key);
		}
		return String.format("-D%s=%s", key, value);
	}

}
