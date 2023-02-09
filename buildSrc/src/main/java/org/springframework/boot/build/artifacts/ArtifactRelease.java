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

package org.springframework.boot.build.artifacts;

import org.gradle.api.Project;

/**
 * Information about artifacts produced by a build.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public final class ArtifactRelease {

	private static final String SNAPSHOT = "snapshot";

	private static final String MILESTONE = "milestone";

	private static final String RELEASE = "release";

	private static final String SPRING_REPO = "https://repo.spring.io/%s";

	private static final String MAVEN_REPO = "https://repo.maven.apache.org/maven2";

	private final String type;

	private ArtifactRelease(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	public String getDownloadRepo() {
		return (this.isRelease()) ? MAVEN_REPO : String.format(SPRING_REPO, this.getType());
	}

	public boolean isRelease() {
		return RELEASE.equals(this.type);
	}

	public static ArtifactRelease forProject(Project project) {
		return new ArtifactRelease(determineReleaseType(project));
	}

	private static String determineReleaseType(Project project) {
		String version = project.getVersion().toString();
		int modifierIndex = version.lastIndexOf('-');
		if (modifierIndex == -1) {
			return RELEASE;
		}
		String type = version.substring(modifierIndex + 1);
		if (type.startsWith("M") || type.startsWith("RC")) {
			return MILESTONE;
		}
		return SNAPSHOT;
	}

}
