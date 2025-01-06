/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.testcontainers.containers.Container;
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
			startables.forEach(TestcontainersStartup::start);
		}

	},

	/**
	 * Startup containers in parallel.
	 */
	PARALLEL {

		@Override
		void start(Collection<? extends Startable> startables) {
			SingleStartables singleStartables = new SingleStartables();
			Startables.deepStart(startables.stream().map(singleStartables::getOrCreate)).join();
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

	/**
	 * Start the given {@link Startable} unless is's detected as already running.
	 * @param startable the startable to start
	 * @since 3.4.1
	 */
	public static void start(Startable startable) {
		if (!isRunning(startable)) {
			startable.start();
		}
	}

	private static boolean isRunning(Startable startable) {
		try {
			return (startable instanceof Container<?> container) && container.isRunning();
		}
		catch (Throwable ex) {
			return false;

		}
	}

	/**
	 * Tracks and adapts {@link Startable} instances to use
	 * {@link TestcontainersStartup#start(Startable)} so containers are only started once
	 * even when calling {@link Startables#deepStart(java.util.stream.Stream)}.
	 */
	private static final class SingleStartables {

		private final Map<Startable, SingleStartable> adapters = new HashMap<>();

		SingleStartable getOrCreate(Startable startable) {
			return this.adapters.computeIfAbsent(startable, this::create);
		}

		private SingleStartable create(Startable startable) {
			return new SingleStartable(this, startable);
		}

		record SingleStartable(SingleStartables singleStartables, Startable startable) implements Startable {

			@Override
			public Set<Startable> getDependencies() {
				Set<Startable> dependencies = this.startable.getDependencies();
				if (dependencies.isEmpty()) {
					return dependencies;
				}
				return dependencies.stream()
					.map(this.singleStartables::getOrCreate)
					.collect(Collectors.toCollection(LinkedHashSet::new));
			}

			@Override
			public void start() {
				TestcontainersStartup.start(this.startable);
			}

			@Override
			public void stop() {
				this.startable.stop();
			}

		}

	}

}
