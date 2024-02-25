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

package org.springframework.boot.buildpack.platform.build;

import org.springframework.util.Assert;

/**
 * Identifying information about the tooling that created a builder.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public class Creator {

	private final String version;

	/**
     * Constructor for the Creator class.
     * 
     * @param version the version of the software being created
     */
    Creator(String version) {
		this.version = version;
	}

	/**
	 * Return the name of the builder creator.
	 * @return the name
	 */
	public String getName() {
		return "Spring Boot";
	}

	/**
	 * Return the version of the builder creator.
	 * @return the version
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Create a new {@code Creator} using the provided version.
	 * @param version the creator version
	 * @return a new creator instance
	 */
	public static Creator withVersion(String version) {
		Assert.notNull(version, "Version must not be null");
		return new Creator(version);
	}

	/**
     * Returns a string representation of the Creator object.
     * The string includes the name and version of the Creator.
     *
     * @return a string representation of the Creator object
     */
    @Override
	public String toString() {
		return getName() + " version " + getVersion();
	}

}
