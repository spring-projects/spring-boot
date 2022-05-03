/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.gradle.junit;

import java.io.File;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.internal.nativeintegration.services.NativeServices;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testfixtures.internal.ProjectBuilderImpl;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Helper class to build Gradle {@link Project Projects} for test fixtures. Wraps
 * functionality of Gradle's own {@link ProjectBuilder} in order to work around an issue
 * on JDK 17 and 18.
 *
 * @author Christoph Dreis
 * @see <a href="https://github.com/gradle/gradle/issues/16857">Gradle Support JDK 17</a>
 */
public final class GradleProjectBuilder {

	private File projectDir;

	private String name;

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
		if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
			NativeServices.initialize(userHome);
			try {
				ProjectBuilderImpl.getGlobalServices();
			}
			catch (Throwable ignore) {
			}
		}
		return builder.build();
	}

}
