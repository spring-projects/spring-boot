/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.init;

import org.jspecify.annotations.Nullable;

/**
 * Provide some basic information about a dependency.
 *
 * @author Stephane Nicoll
 */
final class Dependency {

	private final @Nullable String id;

	private final @Nullable String name;

	private final @Nullable String description;

	Dependency(@Nullable String id, @Nullable String name, @Nullable String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	@Nullable String getId() {
		return this.id;
	}

	@Nullable String getName() {
		return this.name;
	}

	@Nullable String getDescription() {
		return this.description;
	}

}
