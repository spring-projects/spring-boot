/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * Encapsulates information about the artifact coordinates of a library.
 *
 * @author Scott Frederick
 */
class DefaultLibraryCoordinates implements LibraryCoordinates {

	private final @Nullable String groupId;

	private final @Nullable String artifactId;

	private final @Nullable String version;

	/**
	 * Create a new instance from discrete elements.
	 * @param groupId the group ID
	 * @param artifactId the artifact ID
	 * @param version the version
	 */
	DefaultLibraryCoordinates(@Nullable String groupId, @Nullable String artifactId, @Nullable String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	/**
	 * Return the group ID of the coordinates.
	 * @return the group ID
	 */
	@Override
	public @Nullable String getGroupId() {
		return this.groupId;
	}

	/**
	 * Return the artifact ID of the coordinates.
	 * @return the artifact ID
	 */
	@Override
	public @Nullable String getArtifactId() {
		return this.artifactId;
	}

	/**
	 * Return the version of the coordinates.
	 * @return the version
	 */
	@Override
	public @Nullable String getVersion() {
		return this.version;
	}

	/**
	 * Return the coordinates in the form {@code groupId:artifactId:version}.
	 */
	@Override
	public String toString() {
		return LibraryCoordinates.toStandardNotationString(this);
	}

}
