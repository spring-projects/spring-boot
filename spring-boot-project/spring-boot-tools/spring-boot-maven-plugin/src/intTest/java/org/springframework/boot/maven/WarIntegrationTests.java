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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.loader.tools.FileUtils;
import org.springframework.util.FileSystemUtils;

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
	void whenWarIsRepackagedWithOutputTimestampConfiguredThenWarIsReproducible(MavenBuild mavenBuild)
			throws InterruptedException {
		String firstHash = buildWarWithOutputTimestamp(mavenBuild);
		Thread.sleep(1500);
		String secondHash = buildWarWithOutputTimestamp(mavenBuild);
		assertThat(firstHash).isEqualTo(secondHash);
	}

	private String buildWarWithOutputTimestamp(MavenBuild mavenBuild) {
		AtomicReference<String> warHash = new AtomicReference<>();
		mavenBuild.project("war-output-timestamp").execute((project) -> {
			File repackaged = new File(project, "target/war-output-timestamp-0.0.1.BUILD-SNAPSHOT.war");
			assertThat(repackaged).isFile();
			assertThat(repackaged.lastModified()).isEqualTo(1584352800000L);
			try (JarFile jar = new JarFile(repackaged)) {
				List<String> unreproducibleEntries = jar.stream()
						.filter((entry) -> entry.getLastModifiedTime().toMillis() != 1584352800000L)
						.map((entry) -> entry.getName() + ": " + entry.getLastModifiedTime())
						.collect(Collectors.toList());
				assertThat(unreproducibleEntries).isEmpty();
				warHash.set(FileUtils.sha1Hash(repackaged));
				FileSystemUtils.deleteRecursively(project);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
		return warHash.get();
	}

}
