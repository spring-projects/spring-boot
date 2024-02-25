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

	/**
     * Constructs a new ContainerReference with the specified value.
     * 
     * @param value the value of the ContainerReference
     * @throws IllegalArgumentException if the value is empty
     */
    private ContainerReference(String value) {
		Assert.hasText(value, "Value must not be empty");
		this.value = value;
	}

	/**
     * Compares this ContainerReference object to the specified object. The result is true if and only if the argument is not null and is a ContainerReference object that represents the same value as this object.
     * 
     * @param obj the object to compare this ContainerReference against
     * @return true if the given object represents a ContainerReference equivalent to this object, false otherwise
     */
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

	/**
     * Returns the hash code value for this ContainerReference object.
     * 
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		return this.value.hashCode();
	}

	/**
     * Returns a string representation of the ContainerReference object.
     *
     * @return the string representation of the ContainerReference object
     */
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
