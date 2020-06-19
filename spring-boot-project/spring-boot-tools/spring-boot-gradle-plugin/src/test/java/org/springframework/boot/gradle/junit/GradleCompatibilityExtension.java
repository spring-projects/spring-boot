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

package org.springframework.boot.gradle.junit;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.api.JavaVersion;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.boot.gradle.testkit.GradleBuildExtension;

/**
 * {@link Extension} that runs {@link TestTemplate templated tests} against multiple
 * versions of Gradle. Test classes using the extension must have a non-private and
 * non-final {@link GradleBuild} field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 */
public final class GradleCompatibilityExtension implements TestTemplateInvocationContextProvider {

	private static final List<String> GRADLE_VERSIONS;

	static {
		JavaVersion javaVersion = JavaVersion.current();
		if (javaVersion.isCompatibleWith(JavaVersion.VERSION_14)
				|| javaVersion.isCompatibleWith(JavaVersion.VERSION_13)) {
			GRADLE_VERSIONS = Arrays.asList("6.3", "default");
		}
		else {
			GRADLE_VERSIONS = Arrays.asList("5.6.4", "6.3", "default");
		}
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		return GRADLE_VERSIONS.stream().map(GradleVersionTestTemplateInvocationContext::new);
	}

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	private static final class GradleVersionTestTemplateInvocationContext implements TestTemplateInvocationContext {

		private final String gradleVersion;

		GradleVersionTestTemplateInvocationContext(String gradleVersion) {
			this.gradleVersion = gradleVersion;
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return "Gradle " + this.gradleVersion;
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			GradleBuild gradleBuild = new GradleBuild();
			if (!this.gradleVersion.equals("default")) {
				gradleBuild.gradleVersion(this.gradleVersion);
			}
			return Arrays.asList(new GradleBuildFieldSetter(gradleBuild), new GradleBuildExtension());
		}

	}

}
