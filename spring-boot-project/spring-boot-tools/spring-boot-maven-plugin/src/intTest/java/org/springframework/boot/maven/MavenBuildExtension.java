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

package org.springframework.boot.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * An {@link Extension} for templated tests that use {@link MavenBuild}. Each templated
 * test is run against multiple versions of Maven.
 *
 * @author Andy Wilkinson
 */
class MavenBuildExtension implements TestTemplateInvocationContextProvider {

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		try {
			return Files.list(Paths.get("build/maven-binaries")).map(MavenVersionTestTemplateInvocationContext::new);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static final class MavenVersionTestTemplateInvocationContext implements TestTemplateInvocationContext {

		private final Path mavenHome;

		private MavenVersionTestTemplateInvocationContext(Path mavenHome) {
			this.mavenHome = mavenHome;
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return this.mavenHome.getFileName().toString();
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return Arrays.asList(new MavenBuildParameterResolver(this.mavenHome));
		}

	}

	private static final class MavenBuildParameterResolver implements ParameterResolver {

		private final Path mavenHome;

		private MavenBuildParameterResolver(Path mavenHome) {
			this.mavenHome = mavenHome;
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return parameterContext.getParameter().getType().equals(MavenBuild.class);
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return new MavenBuild(this.mavenHome.toFile());
		}

	}

}
