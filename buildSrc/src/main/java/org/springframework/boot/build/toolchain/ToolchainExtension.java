/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.toolchain;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import org.springframework.boot.build.SystemRequirementsExtension;

/**
 * DSL extension for {@link ToolchainPlugin}.
 *
 * @author Christoph Dreis
 */
public abstract class ToolchainExtension {

	private final JavaLanguageVersion javaVersion;

	public ToolchainExtension(Project project) {
		String toolchainVersion = (String) project.findProperty("toolchainVersion");
		this.javaVersion = (toolchainVersion != null) ? JavaLanguageVersion.of(toolchainVersion) : null;
		SystemRequirementsExtension systemRequirements = project.getExtensions()
			.getByType(SystemRequirementsExtension.class);
		getMinimumCompatibleJavaVersion()
			.convention(project.provider(() -> JavaLanguageVersion.of(systemRequirements.getJava().getVersion())));
	}

	public abstract Property<JavaLanguageVersion> getMinimumCompatibleJavaVersion();

	public abstract Property<JavaLanguageVersion> getMaximumCompatibleJavaVersion();

	public abstract ListProperty<String> getTestJvmArgs();

	JavaLanguageVersion getJavaVersion() {
		return this.javaVersion;
	}

}
