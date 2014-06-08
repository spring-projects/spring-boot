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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract base implementation for {@link Dependencies}.
 * 
 * @author Phillip Webb
 * @since 1.1.0
 */
abstract class AbstractDependencies implements Dependencies {

	private final Map<ArtifactAndGroupId, Dependency> byArtifactAndGroupId;

	private final Map<String, Dependency> byArtifactId;

	public AbstractDependencies() {
		this.byArtifactAndGroupId = new LinkedHashMap<ArtifactAndGroupId, Dependency>();
		this.byArtifactId = new LinkedHashMap<String, Dependency>();
	}

	@Override
	public Dependency find(String groupId, String artifactId) {
		return this.byArtifactAndGroupId.get(new ArtifactAndGroupId(groupId, artifactId));
	}

	@Override
	public Dependency find(String artifactId) {
		return this.byArtifactId.get(artifactId);
	}

	@Override
	public Iterator<Dependency> iterator() {
		return this.byArtifactAndGroupId.values().iterator();
	}

	protected void add(ArtifactAndGroupId artifactAndGroupId, Dependency dependency) {
		this.byArtifactAndGroupId.put(artifactAndGroupId, dependency);
		this.byArtifactId.put(dependency.getArtifactId(), dependency);
	}

	/**
	 * Simple holder for an artifact+group ID.
	 */
	protected static class ArtifactAndGroupId {

		private final String groupId;

		private final String artifactId;

		public ArtifactAndGroupId(Dependency dependency) {
			this(dependency.getGroupId(), dependency.getArtifactId());
		}

		public ArtifactAndGroupId(String groupId, String artifactId) {
			Assert.notNull(groupId, "GroupId must not be null");
			Assert.notNull(artifactId, "ArtifactId must not be null");
			this.groupId = groupId;
			this.artifactId = artifactId;
		}

		public Dependency newDependency(String version) {
			return new Dependency(this.groupId, this.artifactId, version);
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
				ArtifactAndGroupId other = (ArtifactAndGroupId) obj;
				boolean result = true;
				result &= this.groupId.equals(other.groupId);
				result &= this.artifactId.equals(other.artifactId);
				return result;
			}
			return false;
		}

	}

}
