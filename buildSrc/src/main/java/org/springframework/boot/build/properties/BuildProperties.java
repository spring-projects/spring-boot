/*
 * Copyright 2012-present the original author or authors.
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
