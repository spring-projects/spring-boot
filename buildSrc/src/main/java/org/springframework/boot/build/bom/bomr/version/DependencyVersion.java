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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Version of a dependency.
 *
 * @author Andy Wilkinson
 */
public interface DependencyVersion extends Comparable<DependencyVersion> {

	/**
	 * Returns whether this version has the same major and minor versions as the
	 * {@code other} version.
	 * @param other the version to test
	 * @return {@code true} if this version has the same major and minor, otherwise
	 * {@code false}
	 */
	boolean isSameMinor(DependencyVersion other);

	/**
	 * Returns whether this version has the same major version as the {@code other}
	 * version.
	 * @param other the version to test
	 * @return {@code true} if this version has the same major, otherwise {@code false}
	 */
	boolean isSameMajor(DependencyVersion other);

	/**
	 * Returns whether the given {@code candidate} is an upgrade of this version.
	 * @param candidate the version to consider
	 * @param movingToSnapshots whether the upgrade is to be considered as part of moving
	 * to snaphots
	 * @return {@code true} if the candidate is an upgrade, otherwise false
	 */
	boolean isUpgrade(DependencyVersion candidate, boolean movingToSnapshots);

	/**
	 * Returns whether this version is a snapshot for the given {@code candidate}.
	 * @param candidate the version to consider
	 * @return {@code true} if this version is a snapshot for the candidate, otherwise
	 * false
	 */
	boolean isSnapshotFor(DependencyVersion candidate);

	static DependencyVersion parse(String version) {
		List<Function<String, DependencyVersion>> parsers = Arrays.asList(CalendarVersionDependencyVersion::parse,
				ArtifactVersionDependencyVersion::parse, ReleaseTrainDependencyVersion::parse,
				MultipleComponentsDependencyVersion::parse, CombinedPatchAndQualifierDependencyVersion::parse,
				LeadingZeroesDependencyVersion::parse, UnstructuredDependencyVersion::parse);
		for (Function<String, DependencyVersion> parser : parsers) {
			DependencyVersion result = parser.apply(version);
			if (result != null) {
				return result;
			}
		}
		throw new IllegalArgumentException("Version '" + version + "' could not be parsed");
	}

}
