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

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A set of buildpack coordinates that uniquely identifies a buildpack.
 *
 * @author Scott Frederick
 * @see <a href=
 * "https://github.com/buildpacks/spec/blob/main/platform.md#ordertoml-toml">Platform
 * Interface Specification</a>
 */
final class BuildpackCoordinates {

	private final String id;

	private final String version;

	/**
     * Constructs a new instance of the BuildpackCoordinates class with the specified ID and version.
     * 
     * @param id the ID of the buildpack coordinates (must not be empty)
     * @param version the version of the buildpack coordinates
     * @throws IllegalArgumentException if the ID is empty
     */
    private BuildpackCoordinates(String id, String version) {
		Assert.hasText(id, "ID must not be empty");
		this.id = id;
		this.version = version;
	}

	/**
     * Returns the ID of the BuildpackCoordinates object.
     *
     * @return the ID of the BuildpackCoordinates object
     */
    String getId() {
		return this.id;
	}

	/**
	 * Return the buildpack ID with all "/" replaced by "_".
	 * @return the ID
	 */
	String getSanitizedId() {
		return this.id.replace("/", "_");
	}

	/**
     * Returns the version of the BuildpackCoordinates.
     *
     * @return the version of the BuildpackCoordinates
     */
    String getVersion() {
		return this.version;
	}

	/**
     * Compares this BuildpackCoordinates object to the specified object for equality.
     * Returns true if the objects are equal, false otherwise.
     * 
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
		BuildpackCoordinates other = (BuildpackCoordinates) obj;
		return this.id.equals(other.id) && ObjectUtils.nullSafeEquals(this.version, other.version);
	}

	/**
     * Returns a hash code value for the object. This method overrides the default implementation of the hashCode() method.
     * The hash code is calculated based on the id and version fields of the BuildpackCoordinates object.
     * 
     * @return the hash code value for the object
     */
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.id.hashCode();
		result = prime * result + ObjectUtils.nullSafeHashCode(this.version);
		return result;
	}

	/**
     * Returns a string representation of the BuildpackCoordinates object.
     * The string representation includes the id and version of the buildpack coordinates.
     * If the version is not empty, it is appended to the id with an "@" symbol.
     * 
     * @return a string representation of the BuildpackCoordinates object
     */
    @Override
	public String toString() {
		return this.id + ((StringUtils.hasText(this.version)) ? "@" + this.version : "");
	}

	/**
	 * Create {@link BuildpackCoordinates} from a <a href=
	 * "https://github.com/buildpacks/spec/blob/main/buildpack.md#buildpacktoml-toml">{@code buildpack.toml}</a>
	 * file.
	 * @param inputStream an input stream containing {@code buildpack.toml} content
	 * @param path the path to the buildpack containing the {@code buildpack.toml} file
	 * @return a new {@link BuildpackCoordinates} instance
	 * @throws IOException on IO error
	 */
	static BuildpackCoordinates fromToml(InputStream inputStream, Path path) throws IOException {
		return fromToml(Toml.parse(inputStream), path);
	}

	/**
     * Parses the TOML file and extracts the buildpack coordinates.
     * 
     * @param toml The TOML parse result.
     * @param path The path to the buildpack descriptor file.
     * @return The buildpack coordinates extracted from the TOML file.
     * @throws IllegalArgumentException if the TOML file is empty or does not contain the required fields.
     * @throws IllegalArgumentException if the TOML file contains both 'stacks' and 'order' fields.
     */
    private static BuildpackCoordinates fromToml(TomlParseResult toml, Path path) {
		Assert.isTrue(!toml.isEmpty(),
				() -> "Buildpack descriptor 'buildpack.toml' is required in buildpack '" + path + "'");
		Assert.hasText(toml.getString("buildpack.id"),
				() -> "Buildpack descriptor must contain ID in buildpack '" + path + "'");
		Assert.hasText(toml.getString("buildpack.version"),
				() -> "Buildpack descriptor must contain version in buildpack '" + path + "'");
		Assert.isTrue(toml.contains("stacks") || toml.contains("order"),
				() -> "Buildpack descriptor must contain either 'stacks' or 'order' in buildpack '" + path + "'");
		Assert.isTrue(!(toml.contains("stacks") && toml.contains("order")),
				() -> "Buildpack descriptor must not contain both 'stacks' and 'order' in buildpack '" + path + "'");
		return new BuildpackCoordinates(toml.getString("buildpack.id"), toml.getString("buildpack.version"));
	}

	/**
	 * Create {@link BuildpackCoordinates} by extracting values from
	 * {@link BuildpackMetadata}.
	 * @param buildpackMetadata the buildpack metadata
	 * @return a new {@link BuildpackCoordinates} instance
	 */
	static BuildpackCoordinates fromBuildpackMetadata(BuildpackMetadata buildpackMetadata) {
		Assert.notNull(buildpackMetadata, "BuildpackMetadata must not be null");
		return new BuildpackCoordinates(buildpackMetadata.getId(), buildpackMetadata.getVersion());
	}

	/**
	 * Create {@link BuildpackCoordinates} from an ID and version.
	 * @param id the buildpack ID
	 * @param version the buildpack version
	 * @return a new {@link BuildpackCoordinates} instance
	 */
	static BuildpackCoordinates of(String id, String version) {
		return new BuildpackCoordinates(id, version);
	}

}
