/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr.version;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Base class for {@link DependencyVersion} implementations.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractDependencyVersion implements DependencyVersion {

	private final ComparableVersion comparableVersion;

	protected AbstractDependencyVersion(ComparableVersion comparableVersion) {
		this.comparableVersion = comparableVersion;
	}

	@Override
	public int compareTo(DependencyVersion other) {
		ComparableVersion otherComparable = (other instanceof AbstractDependencyVersion otherVersion)
				? otherVersion.comparableVersion : new ComparableVersion(other.toString());
		return this.comparableVersion.compareTo(otherComparable);
	}

	@Override
	public boolean isUpgrade(DependencyVersion candidate, boolean movingToSnapshots) {
		ComparableVersion comparableCandidate = (candidate instanceof AbstractDependencyVersion abstractDependencyVersion)
				? abstractDependencyVersion.comparableVersion : new ComparableVersion(candidate.toString());
		return comparableCandidate.compareTo(this.comparableVersion) > 0;
	}

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
		AbstractDependencyVersion other = (AbstractDependencyVersion) obj;
		return this.comparableVersion.equals(other.comparableVersion);
	}

	@Override
	public int hashCode() {
		return this.comparableVersion.hashCode();
	}

	@Override
	public String toString() {
		return this.comparableVersion.toString();
	}

}
