/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.util.Locale;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A named layer used to separate the jar when creating a Docker image.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.3.0
 * @see Layers
 */
public class Layer {

	private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9-]+$");

	private final String name;

	/**
	 * Create a new {@link Layer} instance with the specified name.
	 * @param name the name of the layer.
	 */
	public Layer(String name) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.isTrue(PATTERN.matcher(name).matches(), () -> "Malformed layer name '" + name + "'");
		Assert.isTrue(!name.equalsIgnoreCase("ext") && !name.toLowerCase(Locale.ROOT).startsWith("springboot"),
				() -> "Layer name '" + name + "' is reserved");
		this.name = name;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.name.equals(((Layer) obj).name);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public String toString() {
		return this.name;
	}

}
