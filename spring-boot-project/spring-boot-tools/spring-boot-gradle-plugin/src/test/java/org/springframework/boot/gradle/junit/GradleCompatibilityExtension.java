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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.AnnotationUtils;

import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuildExtension;
import org.springframework.boot.testsupport.gradle.testkit.GradleVersions;
import org.springframework.util.StringUtils;

/**
 * {@link Extension} that runs {@link TestTemplate templated tests} against multiple
 * versions of Gradle. Test classes using the extension must have a non-private and
 * non-final {@link GradleBuild} field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 */
final class GradleCompatibilityExtension implements TestTemplateInvocationContextProvider {

	private static final List<String> GRADLE_VERSIONS = GradleVersions.allCompatible();

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		Stream<String> gradleVersions = GRADLE_VERSIONS.stream();
		GradleCompatibility gradleCompatibility = AnnotationUtils
				.findAnnotation(context.getRequiredTestClass(), GradleCompatibility.class).get();
		if (StringUtils.hasText(gradleCompatibility.versionsLessThan())) {
			GradleVersion upperExclusive = GradleVersion.version(gradleCompatibility.versionsLessThan());
			gradleVersions = gradleVersions
					.filter((version) -> GradleVersion.version(version).compareTo(upperExclusive) < 0);
		}
		return gradleVersions.flatMap((version) -> {
			List<TestTemplateInvocationContext> invocationContexts = new ArrayList<>();
			invocationContexts.add(new GradleVersionTestTemplateInvocationContext(version, false));
			boolean configurationCache = gradleCompatibility.configurationCache();
			if (configurationCache) {
				invocationContexts.add(new GradleVersionTestTemplateInvocationContext(version, true));
			}
			return invocationContexts.stream();
		});
	}

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	private static final class GradleVersionTestTemplateInvocationContext implements TestTemplateInvocationContext {

		private final String gradleVersion;

		private final boolean configurationCache;

		GradleVersionTestTemplateInvocationContext(String gradleVersion, boolean configurationCache) {
			this.gradleVersion = gradleVersion;
			this.configurationCache = configurationCache;
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return "Gradle " + this.gradleVersion + ((this.configurationCache) ? " --configuration-cache" : "");
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			GradleBuild gradleBuild = new GradleBuild().gradleVersion(this.gradleVersion);
			if (this.configurationCache) {
				gradleBuild.configurationCache();
			}
			return Arrays.asList(new GradleBuildFieldSetter(gradleBuild), new GradleBuildExtension());
		}

	}

}
