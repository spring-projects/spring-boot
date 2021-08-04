/*
 * Copyright 2012-2021 the original author or authors.
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
