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

import org.springframework.boot.gradle.testkit.Dsl;
import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.boot.gradle.testkit.GradleBuildExtension;

/**
 * {@link Extension} that runs {@link TestTemplate templated tests} against the Groovy and
 * Kotlin DSLs. Test classes using the extension most have a non-private non-final
 * {@link GradleBuild} field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 */
public class GradleMultiDslExtension implements TestTemplateInvocationContextProvider {

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		return Stream.of(Dsl.values()).map(DslTestTemplateInvocationContext::new);
	}

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	private static final class DslTestTemplateInvocationContext implements TestTemplateInvocationContext {

		private final Dsl dsl;

		DslTestTemplateInvocationContext(Dsl dsl) {
			this.dsl = dsl;
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			GradleBuild gradleBuild = new GradleBuild(this.dsl);
			JavaVersion javaVersion = JavaVersion.current();
			if (javaVersion.isCompatibleWith(JavaVersion.VERSION_17)) {
				gradleBuild.gradleVersion("7.2");
			}
			else if (javaVersion.isCompatibleWith(JavaVersion.VERSION_16)) {
				gradleBuild.gradleVersion("7.0.2");
			}
			return Arrays.asList(new GradleBuildFieldSetter(gradleBuild), new GradleBuildExtension());
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return this.dsl.getName();
		}

	}

}
