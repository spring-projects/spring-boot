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

package org.springframework.boot.maven;

import java.io.File;
import java.io.FileReader;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's war support.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(MavenBuildExtension.class)
class WarIntegrationTests extends AbstractArchiveIntegrationTests {

	@TestTemplate
	void warRepackaging(MavenBuild mavenBuild) {
		mavenBuild.project("war")
				.execute((project) -> assertThat(jar(new File(project, "target/war-0.0.1.BUILD-SNAPSHOT.war")))
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-context")
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-core")
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-jcl")
						.hasEntryWithNameStartingWith("WEB-INF/lib-provided/jakarta.servlet-api-4")
						.hasEntryWithName("org/springframework/boot/loader/WarLauncher.class")
						.hasEntryWithName("WEB-INF/classes/org/test/SampleApplication.class")
						.hasEntryWithName("index.html")
						.manifest((manifest) -> manifest.hasMainClass("org.springframework.boot.loader.WarLauncher")
								.hasStartClass("org.test.SampleApplication").hasAttribute("Not-Used", "Foo")));
	}

	@TestTemplate
	void jarDependencyWithCustomFinalNameBuiltInSameReactorIsPackagedUsingArtifactIdAndVersion(MavenBuild mavenBuild) {
		mavenBuild.project("war-reactor")
				.execute(((project) -> assertThat(jar(new File(project, "war/target/war-0.0.1.BUILD-SNAPSHOT.war")))
						.hasEntryWithName("WEB-INF/lib/jar-0.0.1.BUILD-SNAPSHOT.jar")
						.doesNotHaveEntryWithName("WEB-INF/lib/jar.jar")));
	}

	@TestTemplate
	void whenRequiresUnpackConfigurationIsProvidedItIsReflectedInTheRepackagedWar(MavenBuild mavenBuild) {
		mavenBuild.project("war-with-unpack").execute(
				(project) -> assertThat(jar(new File(project, "target/war-with-unpack-0.0.1.BUILD-SNAPSHOT.war")))
						.hasUnpackEntryWithNameStartingWith("WEB-INF/lib/spring-core-")
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-context-")
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-jcl-"));
	}

	@TestTemplate
	void whenWarIsRepackagedWithOutputTimestampTheBuildFailsAsItIsNotSupported(MavenBuild mavenBuild)
			throws InterruptedException {
		mavenBuild.project("war-output-timestamp").executeAndFail((project) -> {
			try {
				String log = FileCopyUtils.copyToString(new FileReader(new File(project, "target/build.log")));
				assertThat(log).contains("Reproducible repackaging is not supported with war packaging");
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	@TestTemplate
	void whenADependencyHasSystemScopeAndInclusionOfSystemScopeDependenciesIsEnabledItIsIncludedInTheRepackagedJar(
			MavenBuild mavenBuild) {
		mavenBuild.project("war-system-scope").execute((project) -> {
			File main = new File(project, "target/war-system-scope-0.0.1.BUILD-SNAPSHOT.war");
			assertThat(jar(main)).hasEntryWithName("WEB-INF/lib-provided/sample-1.0.0.jar");
		});
	}

}
