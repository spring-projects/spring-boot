/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Objects;

import org.springframework.util.Assert;

/**
 * Volume bindings to apply when creating a container.
 *
 * @author Scott Frederick
 * @since 2.5.0
 */
public final class Binding {

	private final String value;

	private Binding(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Binding)) {
			return false;
		}
		Binding binding = (Binding) obj;
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
	 * Create a {@link Binding} with the specified value containing a host source,
	 * container destination, and options.
	 * @param value the volume binding value
	 * @return a new {@link Binding} instance
	 */
	public static Binding of(String value) {
		Assert.notNull(value, "Value must not be null");
		return new Binding(value);
	}

	/**
	 * Create a {@link Binding} from the specified source and destination.
	 * @param sourceVolume the volume binding host source
	 * @param destination the volume binding container destination
	 * @return a new {@link Binding} instance
	 */
	public static Binding from(VolumeName sourceVolume, String destination) {
		Assert.notNull(sourceVolume, "SourceVolume must not be null");
		return from(sourceVolume.toString(), destination);
	}

	/**
	 * Create a {@link Binding} from the specified source and destination.
	 * @param source the volume binding host source
	 * @param destination the volume binding container destination
	 * @return a new {@link Binding} instance
	 */
	public static Binding from(String source, String destination) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(destination, "Destination must not be null");
		return new Binding(source + ":" + destination);
	}

}
