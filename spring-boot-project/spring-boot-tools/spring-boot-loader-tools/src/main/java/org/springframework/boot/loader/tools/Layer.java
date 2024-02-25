/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.util.regex.Pattern;

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
		Assert.hasText(name, "Name must not be empty");
		Assert.isTrue(PATTERN.matcher(name).matches(), () -> "Malformed layer name '" + name + "'");
		Assert.isTrue(!name.equalsIgnoreCase("ext") && !name.toLowerCase().startsWith("springboot"),
				() -> "Layer name '" + name + "' is reserved");
		this.name = name;
	}

	/**
	 * Compares this Layer object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.name.equals(((Layer) obj).name);
	}

	/**
	 * Returns the hash code value for the Layer object.
	 * @return the hash code value for the Layer object
	 */
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	/**
	 * Returns the string representation of the Layer object.
	 * @return the name of the Layer object
	 */
	@Override
	public String toString() {
		return this.name;
	}

}
