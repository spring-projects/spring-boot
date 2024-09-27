/*
 * Copyright 2024-2024 the original author or authors.
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

package org.springframework.boot.build.properties;

import org.gradle.api.Project;

/**
 * Properties that can influence the build.
 *
 * @param buildType the build type
 * @param gitHub GitHub details
 * @author Phillip Webb
 */
public record BuildProperties(BuildType buildType, GitHub gitHub) {

	private static final String PROPERTY_NAME = BuildProperties.class.getName();

	/**
	 * Get the {@link BuildProperties} for the given {@link Project}.
	 * @param project the source project
	 * @return the build properties
	 */
	public static BuildProperties get(Project project) {
		BuildProperties buildProperties = (BuildProperties) project.findProperty(PROPERTY_NAME);
		if (buildProperties == null) {
			buildProperties = load(project);
			project.getExtensions().getExtraProperties().set(PROPERTY_NAME, buildProperties);
		}
		return buildProperties;
	}

	private static BuildProperties load(Project project) {
		BuildType buildType = buildType(project.findProperty("spring.build-type"));
		return switch (buildType) {
			case OPEN_SOURCE -> new BuildProperties(buildType, GitHub.OPEN_SOURCE);
			case COMMERCIAL -> new BuildProperties(buildType, GitHub.COMMERCIAL);
		};
	}

	private static BuildType buildType(Object value) {
		if (value == null || "oss".equals(value.toString())) {
			return BuildType.OPEN_SOURCE;
		}
		if ("commercial".equals(value.toString())) {
			return BuildType.COMMERCIAL;
		}
		throw new IllegalStateException("Unknown build type property '" + value + "'");
	}

	/**
	 * GitHub properties.
	 *
	 * @param organization the GitHub organization
	 * @param repository the GitHub repository
	 */
	public record GitHub(String organization, String repository) {

		static final GitHub OPEN_SOURCE = new GitHub("spring-projects", "spring-boot");

		static final GitHub COMMERCIAL = new GitHub("spring-projects", "spring-boot-commercial");

	}

}
