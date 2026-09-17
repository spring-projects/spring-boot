/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
