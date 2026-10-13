/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure.condition;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/**
 * Enumeration of supported semantic conventions.
 *
 * @author Andy Wilkinson
 * @since 4.2.0
 */
public enum SemanticConventions {

	/**
	 * Micrometer semantic conventions.
	 */
	MICROMETER(true),

	/**
	 * OpenTelemetry semantic conventions.
	 */
	OPEN_TELEMETRY;

	private final boolean matchIfMissing;

	SemanticConventions() {
		this(false);
	}

	SemanticConventions(boolean matchIfMissing) {
		this.matchIfMissing = matchIfMissing;
	}

	boolean isActive(Environment environment) {
		BindResult<String> result = Binder.get(environment, null)
			.bind("management.observations.conventions", String.class);
		if (!result.isBound()) {
			return this.matchIfMissing;
		}
		return name().equalsIgnoreCase(result.get()) || getCanonicalName().equalsIgnoreCase(result.get());
	}

	private String getCanonicalName() {
		String name = name();
		StringBuilder canonicalName = new StringBuilder(name.length());
		name.chars()
			.filter(Character::isLetterOrDigit)
			.map(Character::toLowerCase)
			.forEach((c) -> canonicalName.append((char) c));
		return canonicalName.toString();
	}

}
