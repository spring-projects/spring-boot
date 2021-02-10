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

import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

/**
 * DSL extension for {@link ToolchainPlugin}.
 *
 * @author Christoph Dreis
 */
public class ToolchainExtension {

	private final Project project;

	private int maximumCompatibleJavaVersion;

	public ToolchainExtension(Project project) {
		this.project = project;
	}

	public void setMaximumCompatibleJavaVersion(int maximumVersion) {
		this.maximumCompatibleJavaVersion = maximumVersion;
	}

	public Optional<JavaLanguageVersion> getToolchainVersion() {
		String toolchainVersion = (String) this.project.findProperty("toolchainVersion");
		if (toolchainVersion == null) {
			return Optional.empty();
		}
		int version = Integer.parseInt(toolchainVersion);
		return getJavaLanguageVersion(version);
	}

	public boolean isJavaVersionSupported() {
		Optional<JavaLanguageVersion> maximumVersion = getJavaLanguageVersion(this.maximumCompatibleJavaVersion);
		if (!maximumVersion.isPresent()) {
			return true;
		}
		Optional<JavaLanguageVersion> toolchainVersion = getToolchainVersion();
		return toolchainVersion.isPresent() && maximumVersion.get().canCompileOrRun(toolchainVersion.get());
	}

	private Optional<JavaLanguageVersion> getJavaLanguageVersion(int version) {
		if (version >= 8) {
			return Optional.of(JavaLanguageVersion.of(version));
		}
		return Optional.empty();
	}

}
