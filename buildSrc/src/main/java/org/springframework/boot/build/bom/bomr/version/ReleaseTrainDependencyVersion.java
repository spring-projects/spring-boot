/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * A {@link DependencyVersion} for a release train such as Spring Data.
 *
 * @author Andy Wilkinson
 */
final class ReleaseTrainDependencyVersion implements DependencyVersion {

	private static final Pattern VERSION_PATTERN = Pattern
		.compile("([A-Z][a-z]+)-((BUILD-SNAPSHOT)|([A-Z-]+)([0-9]*))");

	private final String releaseTrain;

	private final String type;

	private final int version;

	private final String original;

	/**
     * Creates a new ReleaseTrainDependencyVersion object with the specified parameters.
     * 
     * @param releaseTrain the release train name
     * @param type the type of dependency
     * @param version the version number
     * @param original the original version
     */
    private ReleaseTrainDependencyVersion(String releaseTrain, String type, int version, String original) {
		this.releaseTrain = releaseTrain;
		this.type = type;
		this.version = version;
		this.original = original;
	}

	/**
     * Compares this ReleaseTrainDependencyVersion object with the specified object for order.
     * 
     * @param other the object to be compared
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object
     * @throws IllegalArgumentException if the specified object is not an instance of ReleaseTrainDependencyVersion
     */
    @Override
	public int compareTo(DependencyVersion other) {
		if (!(other instanceof ReleaseTrainDependencyVersion otherReleaseTrain)) {
			return -1;
		}
		int comparison = this.releaseTrain.compareTo(otherReleaseTrain.releaseTrain);
		if (comparison != 0) {
			return comparison;
		}
		comparison = this.type.compareTo(otherReleaseTrain.type);
		if (comparison != 0) {
			return comparison;
		}
		return Integer.compare(this.version, otherReleaseTrain.version);
	}

	/**
     * Checks if the given dependency version is an upgrade.
     * 
     * @param candidate The dependency version to check.
     * @param movingToSnapshots Indicates whether the upgrade is moving to snapshots.
     * @return {@code true} if the given dependency version is an upgrade, {@code false} otherwise.
     */
    @Override
	public boolean isUpgrade(DependencyVersion candidate, boolean movingToSnapshots) {
		if (candidate instanceof ReleaseTrainDependencyVersion candidateReleaseTrain) {
			return isUpgrade(candidateReleaseTrain, movingToSnapshots);
		}
		return true;
	}

	/**
     * Determines if the given ReleaseTrainDependencyVersion is an upgrade compared to the current instance.
     * 
     * @param candidate The ReleaseTrainDependencyVersion to compare with.
     * @param movingToSnapshots Indicates if the upgrade is moving to snapshot versions.
     * @return true if the given ReleaseTrainDependencyVersion is an upgrade, false otherwise.
     */
    private boolean isUpgrade(ReleaseTrainDependencyVersion candidate, boolean movingToSnapshots) {
		int comparison = this.releaseTrain.compareTo(candidate.releaseTrain);
		if (comparison != 0) {
			return comparison < 0;
		}
		if (movingToSnapshots && !isSnapshot() && candidate.isSnapshot()) {
			return true;
		}
		comparison = this.type.compareTo(candidate.type);
		if (comparison != 0) {
			return comparison < 0;
		}
		return Integer.compare(this.version, candidate.version) < 0;
	}

	/**
     * Checks if the version is a snapshot.
     * 
     * @return true if the version is a snapshot, false otherwise
     */
    private boolean isSnapshot() {
		return "BUILD-SNAPSHOT".equals(this.type);
	}

	/**
     * Checks if the current dependency version is a snapshot for the given candidate version.
     * 
     * @param candidate The dependency version to check against.
     * @return True if the current version is a snapshot for the candidate version, false otherwise.
     */
    @Override
	public boolean isSnapshotFor(DependencyVersion candidate) {
		if (!isSnapshot() || !(candidate instanceof ReleaseTrainDependencyVersion candidateReleaseTrain)) {
			return false;
		}
		return this.releaseTrain.equals(candidateReleaseTrain.releaseTrain);
	}

	/**
     * Checks if the major version of this ReleaseTrainDependencyVersion is the same as the major version of the specified DependencyVersion.
     * 
     * @param other the DependencyVersion to compare with
     * @return true if the major version is the same, false otherwise
     */
    @Override
	public boolean isSameMajor(DependencyVersion other) {
		return isSameReleaseTrain(other);
	}

	/**
     * Checks if the given DependencyVersion object has the same minor version as this ReleaseTrainDependencyVersion object.
     * 
     * @param other the DependencyVersion object to compare with
     * @return true if the given DependencyVersion object has the same minor version, false otherwise
     */
    @Override
	public boolean isSameMinor(DependencyVersion other) {
		return isSameReleaseTrain(other);
	}

	/**
     * Checks if the given DependencyVersion is from the same release train as this ReleaseTrainDependencyVersion.
     * 
     * @param other the DependencyVersion to compare with
     * @return true if the given DependencyVersion is from the same release train, false otherwise
     */
    private boolean isSameReleaseTrain(DependencyVersion other) {
		if (other instanceof CalendarVersionDependencyVersion) {
			return false;
		}
		if (other instanceof ReleaseTrainDependencyVersion otherReleaseTrain) {
			return otherReleaseTrain.releaseTrain.equals(this.releaseTrain);
		}
		return true;
	}

	/**
     * Compares this ReleaseTrainDependencyVersion object with the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return true if the specified object is equal to this ReleaseTrainDependencyVersion object, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ReleaseTrainDependencyVersion other = (ReleaseTrainDependencyVersion) obj;
		return this.original.equals(other.original);
	}

	/**
     * Returns the hash code value for this ReleaseTrainDependencyVersion object.
     * 
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		return this.original.hashCode();
	}

	/**
     * Returns the original string representation of the object.
     *
     * @return the original string representation of the object
     */
    @Override
	public String toString() {
		return this.original;
	}

	/**
     * Parses the given input string to create a ReleaseTrainDependencyVersion object.
     * 
     * @param input the input string to be parsed
     * @return a ReleaseTrainDependencyVersion object if the input string matches the version pattern, otherwise null
     */
    static ReleaseTrainDependencyVersion parse(String input) {
		Matcher matcher = VERSION_PATTERN.matcher(input);
		if (!matcher.matches()) {
			return null;
		}
		return new ReleaseTrainDependencyVersion(matcher.group(1),
				StringUtils.hasLength(matcher.group(3)) ? matcher.group(3) : matcher.group(4),
				(StringUtils.hasLength(matcher.group(5))) ? Integer.parseInt(matcher.group(5)) : 0, input);
	}

}
