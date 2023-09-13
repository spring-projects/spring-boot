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

	protected ArtifactVersionDependencyVersion(ArtifactVersion artifactVersion) {
		super(new ComparableVersion(toNormalizedString(artifactVersion)));
		this.artifactVersion = artifactVersion;
	}

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

	protected ArtifactVersionDependencyVersion(ArtifactVersion artifactVersion, ComparableVersion comparableVersion) {
		super(comparableVersion);
		this.artifactVersion = artifactVersion;
	}

	@Override
	public boolean isSameMajor(DependencyVersion other) {
		if (other instanceof ReleaseTrainDependencyVersion) {
			return false;
		}
		return extractArtifactVersionDependencyVersion(other).map(this::isSameMajor).orElse(true);
	}

	private boolean isSameMajor(ArtifactVersionDependencyVersion other) {
		return this.artifactVersion.getMajorVersion() == other.artifactVersion.getMajorVersion();
	}

	@Override
	public boolean isSameMinor(DependencyVersion other) {
		if (other instanceof ReleaseTrainDependencyVersion) {
			return false;
		}
		return extractArtifactVersionDependencyVersion(other).map(this::isSameMinor).orElse(true);
	}

	private boolean isSameMinor(ArtifactVersionDependencyVersion other) {
		return isSameMajor(other) && this.artifactVersion.getMinorVersion() == other.artifactVersion.getMinorVersion();
	}

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

	private boolean sameMajorMinorIncremental(ArtifactVersion other) {
		return this.artifactVersion.getMajorVersion() == other.getMajorVersion()
				&& this.artifactVersion.getMinorVersion() == other.getMinorVersion()
				&& this.artifactVersion.getIncrementalVersion() == other.getIncrementalVersion();
	}

	private boolean isSnapshot() {
		return "SNAPSHOT".equals(this.artifactVersion.getQualifier())
				|| "BUILD".equals(this.artifactVersion.getQualifier());
	}

	@Override
	public boolean isSnapshotFor(DependencyVersion candidate) {
		if (!isSnapshot() || !(candidate instanceof ArtifactVersionDependencyVersion)) {
			return false;
		}
		return sameMajorMinorIncremental(((ArtifactVersionDependencyVersion) candidate).artifactVersion);
	}

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

	@Override
	public String toString() {
		return this.artifactVersion.toString();
	}

	protected Optional<ArtifactVersionDependencyVersion> extractArtifactVersionDependencyVersion(
			DependencyVersion other) {
		ArtifactVersionDependencyVersion artifactVersion = null;
		if (other instanceof ArtifactVersionDependencyVersion otherVersion) {
			artifactVersion = otherVersion;
		}
		return Optional.ofNullable(artifactVersion);
	}

	static ArtifactVersionDependencyVersion parse(String version) {
		ArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
		if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier().equals(version)) {
			return null;
		}
		return new ArtifactVersionDependencyVersion(artifactVersion);
	}

}
