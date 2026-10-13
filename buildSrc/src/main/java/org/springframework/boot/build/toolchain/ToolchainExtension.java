/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.toolchain;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

/**
 * DSL extension for {@link ToolchainPlugin}.
 *
 * @author Christoph Dreis
 */
public class ToolchainExtension {

	private final Property<JavaLanguageVersion> maximumCompatibleJavaVersion;

	private final ListProperty<String> testJvmArgs;

	private final JavaLanguageVersion javaVersion;

	public ToolchainExtension(Project project) {
		this.maximumCompatibleJavaVersion = project.getObjects().property(JavaLanguageVersion.class);
		this.testJvmArgs = project.getObjects().listProperty(String.class);
		String toolchainVersion = (String) project.findProperty("toolchainVersion");
		this.javaVersion = (toolchainVersion != null) ? JavaLanguageVersion.of(toolchainVersion) : null;
	}

	public Property<JavaLanguageVersion> getMaximumCompatibleJavaVersion() {
		return this.maximumCompatibleJavaVersion;
	}

	public ListProperty<String> getTestJvmArgs() {
		return this.testJvmArgs;
	}

	JavaLanguageVersion getJavaVersion() {
		return this.javaVersion;
	}

}
