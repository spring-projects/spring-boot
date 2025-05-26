/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * Volume bindings to apply when creating a container.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 2.5.0
 */
public final class Binding {

	/**
	 * Sensitive container paths, which lead to problems if used in a binding.
	 */
	private static final Set<String> SENSITIVE_CONTAINER_PATHS = Set.of("/cnb", "/layers", "/workspace", "c:\\cnb",
			"c:\\layers", "c:\\workspace");

	private final String value;

	private Binding(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Binding binding)) {
			return false;
		}
		return Objects.equals(this.value, binding.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.value);
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Whether the binding uses a sensitive container path.
	 * @return whether the binding uses a sensitive container path
	 * @since 3.4.0
	 */
	public boolean usesSensitiveContainerPath() {
		return SENSITIVE_CONTAINER_PATHS.contains(getContainerDestinationPath());
	}

	/**
	 * Returns the container destination path.
	 * @return the container destination path
	 */
	String getContainerDestinationPath() {
		List<String> parts = getParts();
		Assert.state(parts.size() >= 2, () -> "Expected 2 or more parts, but found %d".formatted(parts.size()));
		return parts.get(1);
	}

	private List<String> getParts() {
		// Format is <host>:<container>:[<options>]
		List<String> parts = new ArrayList<>();
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < this.value.length(); i++) {
			char ch = this.value.charAt(i);
			char nextChar = (i + 1 < this.value.length()) ? this.value.charAt(i + 1) : '\0';
			if (ch == ':' && nextChar != '\\') {
				parts.add(buffer.toString());
				buffer.setLength(0);
			}
			else {
				buffer.append(ch);
			}
		}
		parts.add(buffer.toString());
		return parts;
	}

	/**
	 * Create a {@link Binding} with the specified value containing a host source,
	 * container destination, and options.
	 * @param value the volume binding value
	 * @return a new {@link Binding} instance
	 */
	public static Binding of(String value) {
		Assert.notNull(value, "'value' must not be null");
		return new Binding(value);
	}

	/**
	 * Create a {@link Binding} from the specified source and destination.
	 * @param sourceVolume the volume binding host source
	 * @param destination the volume binding container destination
	 * @return a new {@link Binding} instance
	 */
	public static Binding from(VolumeName sourceVolume, String destination) {
		Assert.notNull(sourceVolume, "'sourceVolume' must not be null");
		return from(sourceVolume.toString(), destination);
	}

	/**
	 * Create a {@link Binding} from the specified source and destination.
	 * @param source the volume binding host source
	 * @param destination the volume binding container destination
	 * @return a new {@link Binding} instance
	 */
	public static Binding from(String source, String destination) {
		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(destination, "'destination' must not be null");
		return new Binding(source + ":" + destination);
	}

}
