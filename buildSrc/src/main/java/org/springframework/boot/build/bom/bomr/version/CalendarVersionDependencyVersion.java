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

import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * A specialization of {@link ArtifactVersionDependencyVersion} for calendar versions.
 * Calendar versions are always considered to be newer than
 * {@link ReleaseTrainDependencyVersion release train versions}.
 *
 * @author Andy Wilkinson
 */
class CalendarVersionDependencyVersion extends ArtifactVersionDependencyVersion {

	private static final Pattern CALENDAR_VERSION_PATTERN = Pattern.compile("\\d{4}\\.\\d+\\.\\d+(-.+)?");

	/**
	 * Constructs a new instance of the CalendarVersionDependencyVersion class with the
	 * specified artifact version.
	 * @param artifactVersion the artifact version to be used for the calendar version
	 * dependency
	 */
	protected CalendarVersionDependencyVersion(ArtifactVersion artifactVersion) {
		super(artifactVersion);
	}

	/**
	 * Constructs a new CalendarVersionDependencyVersion with the specified artifact
	 * version and comparable version.
	 * @param artifactVersion the artifact version of the dependency
	 * @param comparableVersion the comparable version of the dependency
	 */
	protected CalendarVersionDependencyVersion(ArtifactVersion artifactVersion, ComparableVersion comparableVersion) {
		super(artifactVersion, comparableVersion);
	}

	/**
	 * Parses the given version string and returns a CalendarVersionDependencyVersion
	 * object.
	 * @param version the version string to parse
	 * @return a CalendarVersionDependencyVersion object if the version string is valid,
	 * null otherwise
	 */
	static CalendarVersionDependencyVersion parse(String version) {
		if (!CALENDAR_VERSION_PATTERN.matcher(version).matches()) {
			return null;
		}
		ArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
		if (artifactVersion.getQualifier() != null && artifactVersion.getQualifier().equals(version)) {
			return null;
		}
		return new CalendarVersionDependencyVersion(artifactVersion);
	}

}
