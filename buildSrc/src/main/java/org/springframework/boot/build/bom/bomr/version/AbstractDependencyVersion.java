/*
 * Copyright 2012-2022 the original author or authors.
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
		if (!this.comparableVersion.equals(other.comparableVersion)) {
			return false;
		}
		return true;
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
