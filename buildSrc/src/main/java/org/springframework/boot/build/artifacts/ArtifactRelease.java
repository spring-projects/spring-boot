/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.artifacts;

import java.util.Locale;

import org.gradle.api.Project;

/**
 * Information about artifacts produced by a build.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public final class ArtifactRelease {

	private static final String SPRING_SNAPSHOT_REPO = "https://repo.spring.io/snapshot";

	private static final String MAVEN_REPO = "https://repo.maven.apache.org/maven2";

	private final Type type;

	private ArtifactRelease(Type type) {
		this.type = type;
	}

	public String getType() {
		return this.type.toString().toLowerCase(Locale.ROOT);
	}

	public String getDownloadRepo() {
		return (this.type == Type.SNAPSHOT) ? SPRING_SNAPSHOT_REPO : MAVEN_REPO;
	}

	public boolean isRelease() {
		return this.type == Type.RELEASE;
	}

	public static ArtifactRelease forProject(Project project) {
		return forVersion(project.getVersion().toString());
	}

	public static ArtifactRelease forVersion(String version) {
		return new ArtifactRelease(Type.forVersion(version));
	}

	enum Type {

		SNAPSHOT, MILESTONE, RELEASE;

		static Type forVersion(String version) {
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

}
