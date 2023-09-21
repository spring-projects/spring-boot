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

	private UnstructuredDependencyVersion(String version) {
		super(new ComparableVersion(version));
		this.version = version;
	}

	@Override
	public boolean isSameMajor(DependencyVersion other) {
		return true;
	}

	@Override
	public boolean isSameMinor(DependencyVersion other) {
		return true;
	}

	@Override
	public String toString() {
		return this.version;
	}

	@Override
	public boolean isSnapshotFor(DependencyVersion candidate) {
		return false;
	}

	static UnstructuredDependencyVersion parse(String version) {
		return new UnstructuredDependencyVersion(version);
	}

}
