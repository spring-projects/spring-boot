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

import org.springframework.util.Assert;

/**
 * Encapsulates information about the Maven artifact coordinates of a library.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public final class LibraryCoordinates {

	private final String groupId;

	private final String artifactId;

	private final String version;

	/**
	 * Create a new instance from discrete elements.
	 * @param groupId the group ID
	 * @param artifactId the artifact ID
	 * @param version the version
	 */
	public LibraryCoordinates(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	/**
	 * Create a new instance from a String value in the form
	 * {@code groupId:artifactId:version} where the version is optional.
	 * @param coordinates the coordinates
	 */
	public LibraryCoordinates(String coordinates) {
		String[] elements = coordinates.split(":");
		Assert.isTrue(elements.length >= 2, "Coordinates must contain at least 'groupId:artifactId'");
		this.groupId = elements[0];
		this.artifactId = elements[1];
		this.version = (elements.length > 2) ? elements[2] : null;
	}

	/**
	 * Return the group ID of the coordinates.
	 * @return the group ID
	 */
	public String getGroupId() {
		return this.groupId;
	}

	/**
	 * Return the artifact ID of the coordinates.
	 * @return the artifact ID
	 */
	public String getArtifactId() {
		return this.artifactId;
	}

	/**
	 * Return the version of the coordinates.
	 * @return the version
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Return the coordinates in the form {@code groupId:artifactId:version}.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append((this.groupId != null) ? this.groupId : "");
		builder.append(":");
		builder.append((this.artifactId != null) ? this.artifactId : "");
		builder.append(":");
		builder.append((this.version != null) ? this.version : "");
		return builder.toString();
	}

}
