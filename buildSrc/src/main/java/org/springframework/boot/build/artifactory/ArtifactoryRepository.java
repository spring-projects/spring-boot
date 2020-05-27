/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.build.artifactory;

import org.gradle.api.Project;

/**
 * An Artifactory repository to which a build of Spring Boot can be published.
 *
 * @author Andy Wilkinson
 */
public final class ArtifactoryRepository {

	private final String name;

	private ArtifactoryRepository(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public static ArtifactoryRepository forProject(Project project) {
		return new ArtifactoryRepository(determineArtifactoryRepo(project));
	}

	private static String determineArtifactoryRepo(Project project) {
		String version = project.getVersion().toString();
		String type = version.substring(version.lastIndexOf('.') + 1);
		if (type.equals("RELEASE")) {
			return "release";
		}
		if (type.startsWith("M") || type.startsWith("RC")) {
			return "milestone";
		}
		return "snapshot";
	}

}
