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

import java.util.Objects;
import java.util.Optional;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import org.springframework.util.StringUtils;

/**
 * A {@link DependencyVersion} backed by an {@link ArtifactVersion}.
 *
 * @author Andy Wilkinson
 */
class ArtifactVersionDependencyVersion extends AbstractDependencyVersion {

	private final ArtifactVersion artifactVersion;

	/**
     * Constructs a new instance of the ArtifactVersionDependencyVersion class with the specified artifact version.
     * 
     * @param artifactVersion the artifact version to be used for constructing the dependency version
     */
    protected ArtifactVersionDependencyVersion(ArtifactVersion artifactVersion) {
		super(new ComparableVersion(toNormalizedString(artifactVersion)));
		this.artifactVersion = artifactVersion;
	}

	/**
     * Converts the given ArtifactVersion to a normalized string representation.
     * 
     * @param artifactVersion the ArtifactVersion to be converted
     * @return the normalized string representation of the ArtifactVersion
     */
    private static String toNormalizedString(ArtifactVersion artifactVersion) {
		String versionString = artifactVersion.toString();
		if (versionString.endsWith(".RELEASE")) {
			return versionString.substring(0, versionString.length() - 8);
		}
		if (versionString.endsWith(".BUILD-SNAPSHOT")) {
			return versionString.substring(0, versionString.length() - 15) + "-SNAPSHOT";
		}
		return versionString;
	}

	/**
     * Constructs a new instance of the ArtifactVersionDependencyVersion class with the specified artifact version and comparable version.
     * 
     * @param artifactVersion the artifact version associated with this dependency version
     * @param comparableVersion the comparable version used for comparison and sorting
     */
    protected ArtifactVersionDependencyVersion(ArtifactVersion artifactVersion, ComparableVersion comparableVersion) {
		super(comparableVersion);
		this.artifactVersion = artifactVersion;
	}

	/**
     * Checks if the given DependencyVersion is of the same major version as this ArtifactVersionDependencyVersion.
     * 
     * @param other The DependencyVersion to compare with.
     * @return true if the major version of the given DependencyVersion is the same as this ArtifactVersionDependencyVersion, false otherwise.
     */
    @Override
	public boolean isSameMajor(DependencyVersion other) {
		if (other instanceof ReleaseTrainDependencyVersion) {
			return false;
		}
		return extractArtifactVersionDependencyVersion(other).map(this::isSameMajor).orElse(true);
	}

	/**
     * Checks if the major version of this ArtifactVersionDependencyVersion object is the same as the major version of the specified object.
     * 
     * @param other the other ArtifactVersionDependencyVersion object to compare with
     * @return true if the major versions are the same, false otherwise
     */
    private boolean isSameMajor(ArtifactVersionDependencyVersion other) {
		return this.artifactVersion.getMajorVersion() == other.artifactVersion.getMajorVersion();
	}

	/**
     * Checks if the given DependencyVersion is of the same minor version as this ArtifactVersionDependencyVersion.
     * 
     * @param other The DependencyVersion to compare with.
     * @return true if the given DependencyVersion is of the same minor version, false otherwise.
     */
    @Override
	public boolean isSameMinor(DependencyVersion other) {
		if (other instanceof ReleaseTrainDependencyVersion) {
			return false;
		}
		return extractArtifactVersionDependencyVersion(other).map(this::isSameMinor).orElse(true);
	}

	/**
     * Checks if the minor version of this ArtifactVersionDependencyVersion object is the same as the minor version of the specified object.
     * 
     * @param other the other ArtifactVersionDependencyVersion object to compare with
     * @return true if the minor versions are the same, false otherwise
     */
    private boolean isSameMinor(ArtifactVersionDependencyVersion other) {
		return isSameMajor(other) && this.artifactVersion.getMinorVersion() == other.artifactVersion.getMinorVersion();
	}

	/**
     * Determines if the given dependency version is an upgrade from the current version.
     * 
     * @param candidate The dependency version to check.
     * @param movingToSnapshots Indicates if the upgrade is moving to snapshots.
     * @return True if the given version is an upgrade, false otherwise.
     */
    @Override
	public boolean isUpgrade(DependencyVersion candidate, boolean movingToSnapshots) {
		if (!(candidate instanceof ArtifactVersionDependencyVersion)) {
			return false;
		}
		ArtifactVersion other = ((ArtifactVersionDependencyVersion) candidate).artifactVersion;
		if (this.artifactVersion.equals(other)) {
			return false;
		}
		if (sameMajorMinorIncremental(other)) {
			if (!StringUtils.hasLength(this.artifactVersion.getQualifier())
					|| "RELEASE".equals(this.artifactVersion.getQualifier())) {
				return false;
			}
			if (isSnapshot()) {
				return true;
			}
			else if (((ArtifactVersionDependencyVersion) candidate).isSnapshot()) {
				return movingToSnapshots;
			}
		}
		return super.isUpgrade(candidate, movingToSnapshots);
	}

	/**
     * Checks if the major, minor, and incremental versions of this ArtifactVersionDependencyVersion object are the same as the provided ArtifactVersion object.
     * 
     * @param other the ArtifactVersion object to compare with
     * @return true if the major, minor, and incremental versions are the same, false otherwise
     */
    private boolean sameMajorMinorIncremental(ArtifactVersion other) {
		return this.artifactVersion.getMajorVersion() == other.getMajorVersion()
				&& this.artifactVersion.getMinorVersion() == other.getMinorVersion()
				&& this.artifactVersion.getIncrementalVersion() == other.getIncrementalVersion();
	}

	/**
     * Checks if the artifact version is a snapshot version.
     * 
     * @return true if the artifact version is a snapshot version, false otherwise
     */
    private boolean isSnapshot() {
		return "SNAPSHOT".equals(this.artifactVersion.getQualifier())
				|| "BUILD".equals(this.artifactVersion.getQualifier());
	}

	/**
     * Checks if the given DependencyVersion is a snapshot version for this ArtifactVersionDependencyVersion.
     * 
     * @param candidate The DependencyVersion to check.
     * @return True if the given DependencyVersion is a snapshot version for this ArtifactVersionDependencyVersion, false otherwise.
     */
    @Override
	public boolean isSnapshotFor(DependencyVersion candidate) {
		if (!isSnapshot() || !(candidate instanceof ArtifactVersionDependencyVersion)) {
			return false;
		}
		return sameMajorMinorIncremental(((ArtifactVersionDependencyVersion) candidate).artifactVersion);
	}

	/**
     * Compares this DependencyVersion object with the specified DependencyVersion object for order.
     * 
     * @param other the DependencyVersion object to be compared
     * @return a negative integer, zero, or a positive integer as this DependencyVersion object is less than, equal to, or greater than the specified DependencyVersion object
     */
    @Override
	public int compareTo(DependencyVersion other) {
		if (other instanceof ArtifactVersionDependencyVersion otherArtifactDependencyVersion) {
			ArtifactVersion otherArtifactVersion = otherArtifactDependencyVersion.artifactVersion;
			if ((!Objects.equals(this.artifactVersion.getQualifier(), otherArtifactVersion.getQualifier()))
					&& "snapshot".equalsIgnoreCase(otherArtifactVersion.getQualifier())
					&& otherArtifactVersion.getMajorVersion() == this.artifactVersion.getMajorVersion()
					&& otherArtifactVersion.getMinorVersion() == this.artifactVersion.getMinorVersion()
					&& otherArtifactVersion.getIncrementalVersion() == this.artifactVersion.getIncrementalVersion()) {
				return 1;
			}
		}
		return super.compareTo(other);
	}

	/**
     * Returns a string representation of the ArtifactVersionDependencyVersion object.
     * 
     * @return the string representation of the ArtifactVersionDependencyVersion object
     */
    @Override
	public String toString() {
		return this.artifactVersion.toString();
	}

	/**
     * Extracts the ArtifactVersionDependencyVersion from the given DependencyVersion.
     * 
     * @param other the DependencyVersion to extract from
     * @return an Optional containing the extracted ArtifactVersionDependencyVersion, or an empty Optional if the given DependencyVersion is not an instance of ArtifactVersionDependencyVersion
     */
    protected Optional<ArtifactVersionDependencyVersion> extractArtifactVersionDependencyVersion(
			DependencyVersion other) {
		ArtifactVersionDependencyVersion artifactVersion = null;
		if (other instanceof ArtifactVersionDependencyVersion otherVersion) {
			artifactVersion = otherVersion;
		}
		return Optional.ofNullable(artifactVersion);
	}

	/**
     * Parses a given version string and returns an instance of ArtifactVersionDependencyVersion.
     * 
     * @param version the version string to be parsed
     * @return an instance of ArtifactVersionDependencyVersion if the version string is valid, otherwise null
     */
    static ArtifactVersionDependencyVersion parse(String version) {
		ArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
		if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier().equals(version)) {
			return null;
		}
		return new ArtifactVersionDependencyVersion(artifactVersion);
	}

}
