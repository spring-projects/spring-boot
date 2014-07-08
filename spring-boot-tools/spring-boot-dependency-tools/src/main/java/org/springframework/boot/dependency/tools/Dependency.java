/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.dependency.tools;

import java.util.Collections;
import java.util.List;

/**
 * A single dependency.
 *
 * @author Phillip Webb
 * @see Dependencies
 */
public final class Dependency {

	private final String groupId;

	private final String artifactId;

	private final String version;

	private final List<Exclusion> exclusions;

	/**
	 * Create a new {@link Dependency} instance.
	 * @param groupId the group ID
	 * @param artifactId the artifact ID
	 * @param version the version
	 */
	public Dependency(String groupId, String artifactId, String version) {
		this(groupId, artifactId, version, Collections.<Exclusion> emptyList());
	}

	/**
	 * Create a new {@link Dependency} instance.
	 * @param groupId the group ID
	 * @param artifactId the artifact ID
	 * @param version the version
	 * @param exclusions the exclusions
	 */
	public Dependency(String groupId, String artifactId, String version,
			List<Exclusion> exclusions) {
		Assert.notNull(groupId, "GroupId must not be null");
		Assert.notNull(artifactId, "ArtifactId must not be null");
		Assert.notNull(version, "Version must not be null");
		Assert.notNull(exclusions, "Exclusions must not be null");
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.exclusions = Collections.unmodifiableList(exclusions);
	}

	/**
	 * Return the dependency group id.
	 */
	public String getGroupId() {
		return this.groupId;
	}

	/**
	 * Return the dependency artifact id.
	 */
	public String getArtifactId() {
		return this.artifactId;
	}

	/**
	 * Return the dependency version.
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Return the dependency exclusions.
	 */
	public List<Exclusion> getExclusions() {
		return this.exclusions;
	}

	@Override
	public String toString() {
		return this.groupId + ":" + this.artifactId + ":" + this.version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.groupId.hashCode();
		result = prime * result + this.artifactId.hashCode();
		result = prime * result + this.version.hashCode();
		result = prime * result + this.exclusions.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() == obj.getClass()) {
			Dependency other = (Dependency) obj;
			boolean result = true;
			result &= this.groupId.equals(other.groupId);
			result &= this.artifactId.equals(other.artifactId);
			result &= this.version.equals(other.version);
			result &= this.exclusions.equals(other.exclusions);
			return result;
		}
		return false;
	}

	/**
	 * A dependency exclusion.
	 */
	public static final class Exclusion {

		private final String groupId;

		private final String artifactId;

		Exclusion(String groupId, String artifactId) {
			Assert.notNull(groupId, "GroupId must not be null");
			Assert.notNull(groupId, "ArtifactId must not be null");
			this.groupId = groupId;
			this.artifactId = artifactId;
		}

		/**
		 * Return the exclusion artifact id.
		 */
		public String getArtifactId() {
			return this.artifactId;
		}

		/**
		 * Return the exclusion group id.
		 */
		public String getGroupId() {
			return this.groupId;
		}

		@Override
		public String toString() {
			return this.groupId + ":" + this.artifactId;
		}

		@Override
		public int hashCode() {
			return this.groupId.hashCode() * 31 + this.artifactId.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() == obj.getClass()) {
				Exclusion other = (Exclusion) obj;
				boolean result = true;
				result &= this.groupId.equals(other.groupId);
				result &= this.artifactId.equals(other.artifactId);
				return result;
			}
			return false;
		}

	}

}
