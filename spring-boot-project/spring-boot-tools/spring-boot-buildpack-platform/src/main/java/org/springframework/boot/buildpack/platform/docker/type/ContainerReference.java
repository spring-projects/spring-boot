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

package org.springframework.boot.buildpack.platform.docker.type;

import org.springframework.util.Assert;

/**
 * A reference to a Docker container.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public final class ContainerReference {

	private final String value;

	private ContainerReference(String value) {
		Assert.hasText(value, "Value must not be empty");
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ContainerReference other = (ContainerReference) obj;
		return this.value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Factory method to create a {@link ContainerReference} with a specific value.
	 * @param value the container reference value
	 * @return a new container reference instance
	 */
	public static ContainerReference of(String value) {
		return new ContainerReference(value);
	}

}
