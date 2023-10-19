/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers.lifecycle;

import java.util.Collection;

import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Testcontainers startup strategies. The strategy to use can be configured in the Spring
 * {@link Environment} with a {@value #PROPERTY} property.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public enum TestcontainersStartup {

	/**
	 * Startup containers sequentially.
	 */
	SEQUENTIAL {

		@Override
		void start(Collection<? extends Startable> startables) {
			startables.forEach(Startable::start);
		}

	},

	/**
	 * Startup containers in parallel.
	 */
	PARALLEL {

		@Override
		void start(Collection<? extends Startable> startables) {
			Startables.deepStart(startables).join();
		}

	};

	/**
	 * The {@link Environment} property used to change the {@link TestcontainersStartup}
	 * strategy.
	 */
	public static final String PROPERTY = "spring.testcontainers.beans.startup";

	abstract void start(Collection<? extends Startable> startables);

	static TestcontainersStartup get(ConfigurableEnvironment environment) {
		return get((environment != null) ? environment.getProperty(PROPERTY) : null);
	}

	private static TestcontainersStartup get(String value) {
		if (value == null) {
			return SEQUENTIAL;
		}
		String canonicalName = getCanonicalName(value);
		for (TestcontainersStartup candidate : values()) {
			if (candidate.name().equalsIgnoreCase(canonicalName)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("Unknown '%s' property value '%s'".formatted(PROPERTY, value));
	}

	private static String getCanonicalName(String name) {
		StringBuilder canonicalName = new StringBuilder(name.length());
		name.chars()
			.filter(Character::isLetterOrDigit)
			.map(Character::toLowerCase)
			.forEach((c) -> canonicalName.append((char) c));
		return canonicalName.toString();
	}

}
