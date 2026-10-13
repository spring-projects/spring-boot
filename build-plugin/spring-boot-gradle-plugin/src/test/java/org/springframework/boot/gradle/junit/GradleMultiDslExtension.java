/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.junit;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import org.springframework.boot.gradle.testkit.PluginClasspathGradleBuild;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.boot.testsupport.gradle.testkit.Dsl;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuildExtension;
import org.springframework.boot.testsupport.gradle.testkit.GradleVersions;

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
		BuildOutput buildOutput = new BuildOutput(context.getRequiredTestClass());
		return Stream.of(Dsl.values()).map((dsl) -> new DslTestTemplateInvocationContext(buildOutput, dsl));
	}

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	private static final class DslTestTemplateInvocationContext implements TestTemplateInvocationContext {

		private final BuildOutput buildOutput;

		private final Dsl dsl;

		DslTestTemplateInvocationContext(BuildOutput buildOutput, Dsl dsl) {
			this.buildOutput = buildOutput;
			this.dsl = dsl;
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			PluginClasspathGradleBuild gradleBuild = new PluginClasspathGradleBuild(this.buildOutput, this.dsl);
			if (this.dsl == Dsl.KOTLIN) {
				gradleBuild.kotlin();
			}
			gradleBuild.gradleVersion(GradleVersions.minimumCompatible());
			return Arrays.asList(new GradleBuildFieldSetter(gradleBuild), new GradleBuildExtension());
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return this.dsl.getName();
		}

	}

}
