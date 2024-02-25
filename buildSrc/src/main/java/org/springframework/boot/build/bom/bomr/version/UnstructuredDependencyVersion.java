/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.build.bom.bomr.version;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * A {@link DependencyVersion} with no structure such that version comparisons are not
 * possible.
 *
 * @author Andy Wilkinson
 */
final class UnstructuredDependencyVersion extends AbstractDependencyVersion implements DependencyVersion {

	private final String version;

	/**
     * Constructs a new UnstructuredDependencyVersion object with the given version.
     * 
     * @param version the version string to be used for constructing the object
     */
    private UnstructuredDependencyVersion(String version) {
		super(new ComparableVersion(version));
		this.version = version;
	}

	/**
     * Checks if the major version of this dependency is the same as the major version of the specified dependency.
     * 
     * @param other the dependency version to compare with
     * @return true if the major versions are the same, false otherwise
     */
    @Override
	public boolean isSameMajor(DependencyVersion other) {
		return true;
	}

	/**
     * Checks if the given DependencyVersion object has the same minor version as this UnstructuredDependencyVersion object.
     * 
     * @param other the DependencyVersion object to compare with
     * @return true if the minor version of the given object is the same as this object's minor version, false otherwise
     */
    @Override
	public boolean isSameMinor(DependencyVersion other) {
		return true;
	}

	/**
     * Returns the string representation of the version.
     *
     * @return the version as a string
     */
    @Override
	public String toString() {
		return this.version;
	}

	/**
     * Determines if the given DependencyVersion is a snapshot for this UnstructuredDependencyVersion.
     * 
     * @param candidate the DependencyVersion to check
     * @return true if the given DependencyVersion is a snapshot for this UnstructuredDependencyVersion, false otherwise
     */
    @Override
	public boolean isSnapshotFor(DependencyVersion candidate) {
		return false;
	}

	/**
     * Parses the given version string and returns an instance of UnstructuredDependencyVersion.
     * 
     * @param version the version string to be parsed
     * @return an instance of UnstructuredDependencyVersion representing the parsed version
     */
    static UnstructuredDependencyVersion parse(String version) {
		return new UnstructuredDependencyVersion(version);
	}

}
