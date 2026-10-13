/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.junit;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Helper class to build Gradle {@link Project Projects} for test fixtures. Wraps
 * functionality of Gradle's own {@link ProjectBuilder}.
 *
 * @author Christoph Dreis
 */
public final class GradleProjectBuilder {

	private @Nullable File projectDir;

	private @Nullable String name;

	private GradleProjectBuilder() {
	}

	public static GradleProjectBuilder builder() {
		return new GradleProjectBuilder();
	}

	public GradleProjectBuilder withProjectDir(File dir) {
		this.projectDir = dir;
		return this;
	}

	public GradleProjectBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public Project build() {
		Assert.notNull(this.projectDir, "ProjectDir must not be null");
		ProjectBuilder builder = ProjectBuilder.builder();
		builder.withProjectDir(this.projectDir);
		File userHome = new File(this.projectDir, "userHome");
		builder.withGradleUserHomeDir(userHome);
		if (StringUtils.hasText(this.name)) {
			builder.withName(this.name);
		}
		return builder.build();
	}

}
