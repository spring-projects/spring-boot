/*
 * Copyright 2012-present the original author or authors.
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.loader.tools.JarModeLibrary;
import org.springframework.boot.testsupport.FileUtils;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's jar support.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
@ExtendWith(MavenBuildExtension.class)
class JarIntegrationTests extends AbstractArchiveIntegrationTests {

	@Override
	protected String getLayersIndexLocation() {
		return "BOOT-INF/layers.idx";
	}

	@TestTemplate
	void whenJarIsRepackagedInPlaceOnlyRepackagedJarIsInstalled(MavenBuild mavenBuild) {
		mavenBuild.project("jar").goals("install").execute((project) -> {
			File original = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar.original");
			assertThat(original).isFile();
			File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(launchScript(repackaged)).isEmpty();
			assertThat(jar(repackaged)).manifest((manifest) -> {
				manifest.hasMainClass("org.springframework.boot.loader.launch.JarLauncher");
				manifest.hasStartClass("some.random.Main");
				manifest.hasAttribute("Not-Used", "Foo");
			})
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/commons-logging")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jakarta.servlet-api-6")
				.hasEntryWithName("BOOT-INF/classes/org/test/SampleApplication.class")
				.hasEntryWithName("org/springframework/boot/loader/launch/JarLauncher.class");
			assertThat(buildLog(project))
				.contains("Replacing main artifact " + repackaged + " with repackaged archive,")
				.contains("The original artifact has been renamed to " + original)
				.contains("Installing " + repackaged + " to")
				.doesNotContain("Installing " + original + " to");
		});
	}

	@TestTemplate
	void whenAttachIsDisabledOnlyTheOriginalJarIsInstalled(MavenBuild mavenBuild) {
		mavenBuild.project("jar-attach-disabled").goals("install").execute((project) -> {
			File original = new File(project, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar.original");
			assertThat(original).isFile();
			File main = new File(project, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(main).isFile();
			assertThat(buildLog(project)).contains("Updating main artifact " + main + " to " + original)
				.contains("Installing " + original + " to")
				.doesNotContain("Installing " + main + " to");
		});
	}

	@TestTemplate
	void whenAClassifierIsConfiguredTheRepackagedJarHasAClassifierAndBothItAndTheOriginalAreInstalled(
			MavenBuild mavenBuild) {
		mavenBuild.project("jar-classifier-main").goals("install").execute((project) -> {
			assertThat(new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar.original"))
				.doesNotExist();
			File main = new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(main).isFile();
			File repackaged = new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT-test.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project))
				.contains("Attaching repackaged archive " + repackaged + " with classifier test")
				.doesNotContain("Creating repackaged archive " + repackaged + " with classifier test")
				.contains("Installing " + main + " to")
				.contains("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenBothJarsHaveTheSameClassifierRepackagingIsDoneInPlaceAndOnlyRepackagedJarIsInstalled(
			MavenBuild mavenBuild) {
		mavenBuild.project("jar-classifier-source").goals("install").execute((project) -> {
			File original = new File(project, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar.original");
			assertThat(original).isFile();
			File repackaged = new File(project, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project))
				.contains("Replacing artifact with classifier test " + repackaged + " with repackaged archive,")
				.contains("The original artifact has been renamed to " + original)
				.doesNotContain("Installing " + original + " to")
				.contains("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenBothJarsHaveTheSameClassifierAndAttachIsDisabledOnlyTheOriginalJarIsInstalled(MavenBuild mavenBuild) {
		mavenBuild.project("jar-classifier-source-attach-disabled").goals("install").execute((project) -> {
			File original = new File(project,
					"target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar.original");
			assertThat(original).isFile();
			File repackaged = new File(project,
					"target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project))
				.doesNotContain("Attaching repackaged archive " + repackaged + " with classifier test")
				.contains("Updating artifact with classifier test " + repackaged + " to " + original)
				.contains("Installing " + original + " to")
				.doesNotContain("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenAClassifierAndAnOutputDirectoryAreConfiguredTheRepackagedJarHasAClassifierAndIsWrittenToTheOutputDirectory(
			MavenBuild mavenBuild) {
		mavenBuild.project("jar-create-dir").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/foo/jar-create-dir-0.0.1.BUILD-SNAPSHOT-foo.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project)).contains("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenAnOutputDirectoryIsConfiguredTheRepackagedJarIsWrittenToIt(MavenBuild mavenBuild) {
		mavenBuild.project("jar-custom-dir").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/foo/jar-custom-dir-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project)).contains("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenACustomLaunchScriptIsConfiguredItAppearsInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-custom-launcher").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(launchScript(repackaged)).contains("Hello world");
		});
	}

	@TestTemplate
	void whenAnEntryIsExcludedItDoesNotAppearInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-exclude-entry").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-exclude-entry-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/commons-logging")
				.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/servlet-api-");
		});
	}

	@TestTemplate
	void whenAnEntryIsOptionalByDefaultDoesNotAppearInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-optional-default").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-optional-default-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
				.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/log4j-api-");
		});
	}

	@TestTemplate
	void whenAnEntryIsOptionalAndOptionalsIncludedAppearsInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-optional-include").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-optional-include-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/log4j-api-");
		});
	}

	@TestTemplate
	void whenAnEntryIsOptionalAndOptionalsExcludedDoesNotAppearInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-optional-exclude").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-optional-exclude-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
				.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/log4j-api-");
		});
	}

	@TestTemplate
	void whenAnEntryIsExcludedWithPropertyItDoesNotAppearInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar")
			.systemProperty("spring-boot.excludes", "jakarta.servlet:jakarta.servlet-api")
			.goals("install")
			.execute((project) -> {
				File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/commons-logging")
					.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/jakarta.servlet-api-");
			});
	}

	@TestTemplate
	void whenAnEntryIsIncludedOnlyIncludedEntriesAppearInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-include-entry").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-include-entry-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jakarta.servlet-api-")
				.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
				.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
				.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/commons-logging");
		});
	}

	@TestTemplate
	void whenAnIncludeIsSpecifiedAsAPropertyOnlyIncludedEntriesAppearInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar")
			.systemProperty("spring-boot.includes", "jakarta.servlet:jakarta.servlet-api")
			.goals("install")
			.execute((project) -> {
				File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/jakarta.servlet-api-")
					.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
					.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
					.doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/commons-logging");
			});
	}

	@TestTemplate
	void whenAGroupIsExcludedNoEntriesInThatGroupAppearInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-exclude-group").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-exclude-group-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/commons-logging")
				.doesNotHaveEntryWithName("BOOT-INF/lib/log4j-api-");
		});
	}

	@TestTemplate
	void whenAJarIsExecutableItBeginsWithTheDefaultLaunchScript(MavenBuild mavenBuild) {
		mavenBuild.project("jar-executable").execute((project) -> {
			File repackaged = new File(project, "target/jar-executable-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(launchScript(repackaged)).contains("Spring Boot Startup Script")
				.contains("MyFullyExecutableJarName")
				.contains("MyFullyExecutableJarDesc");
		});
	}

	@TestTemplate
	void whenAJarIsBuiltWithLibrariesWithConflictingNamesTheyAreMadeUniqueUsingTheirGroupIds(MavenBuild mavenBuild) {
		mavenBuild.project("jar-lib-name-conflict").execute((project) -> {
			File repackaged = new File(project, "test-project/target/test-project-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithName("BOOT-INF/lib/org.springframework.boot.maven.it-acme-lib-0.0.1.BUILD-SNAPSHOT.jar")
				.hasEntryWithName(
						"BOOT-INF/lib/org.springframework.boot.maven.it.another-acme-lib-0.0.1.BUILD-SNAPSHOT.jar");
		});
	}

	@TestTemplate
	void whenAProjectUsesPomPackagingRepackagingIsSkipped(MavenBuild mavenBuild) {
		mavenBuild.project("jar-pom").execute((project) -> {
			File target = new File(project, "target");
			assertThat(target.listFiles()).containsExactly(new File(target, "build.log"));
		});
	}

	@TestTemplate
	void whenRepackagingIsSkippedTheJarIsNotRepackaged(MavenBuild mavenBuild) {
		mavenBuild.project("jar-skip").execute((project) -> {
			File main = new File(project, "target/jar-skip-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).doesNotHaveEntryWithNameStartingWith("org/springframework/boot");
			assertThat(new File(project, "target/jar-skip-0.0.1.BUILD-SNAPSHOT.jar.original")).doesNotExist();

		});
	}

	@TestTemplate
	void whenADependencyHasSystemScopeAndInclusionOfSystemScopeDependenciesIsEnabledItIsIncludedInTheRepackagedJar(
			MavenBuild mavenBuild) {
		mavenBuild.project("jar-system-scope").execute((project) -> {
			File main = new File(project, "target/jar-system-scope-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).hasEntryWithName("BOOT-INF/lib/sample-1.0.0.jar");

		});
	}

	@TestTemplate
	void whenADependencyHasSystemScopeItIsNotIncludedInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-system-scope-default").execute((project) -> {
			File main = new File(project, "target/jar-system-scope-default-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).doesNotHaveEntryWithName("BOOT-INF/lib/sample-1.0.0.jar");

		});
	}

	@TestTemplate
	void whenADependencyHasTestScopeItIsNotIncludedInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-test-scope").execute((project) -> {
			File main = new File(project, "target/jar-test-scope-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/log4j")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-");
		});
	}

	@TestTemplate
	void whenAProjectUsesKotlinItsModuleMetadataIsRepackagedIntoBootInfClasses(MavenBuild mavenBuild) {
		mavenBuild.project("jar-with-kotlin-module").execute((project) -> {
			File main = new File(project, "target/jar-with-kotlin-module-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).hasEntryWithName("BOOT-INF/classes/META-INF/jar-with-kotlin-module.kotlin_module");
		});
	}

	@TestTemplate
	void whenAProjectIsBuiltWithALayoutPropertyTheSpecifiedLayoutIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("jar-with-layout-property")
			.goals("package", "-Dspring-boot.repackage.layout=ZIP")
			.execute((project) -> {
				File main = new File(project, "target/jar-with-layout-property-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(jar(main)).manifest(
						(manifest) -> manifest.hasMainClass("org.springframework.boot.loader.launch.PropertiesLauncher")
							.hasStartClass("org.test.SampleApplication"));
				assertThat(buildLog(project)).contains("Layout: ZIP");
			});
	}

	@TestTemplate
	void whenALayoutIsConfiguredTheSpecifiedLayoutIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("jar-with-zip-layout").execute((project) -> {
			File main = new File(project, "target/jar-with-zip-layout-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).manifest(
					(manifest) -> manifest.hasMainClass("org.springframework.boot.loader.launch.PropertiesLauncher")
						.hasStartClass("org.test.SampleApplication"));
			assertThat(buildLog(project)).contains("Layout: ZIP");
		});
	}

	@TestTemplate
	void whenRequiresUnpackConfigurationIsProvidedItIsReflectedInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-with-unpack").execute((project) -> {
			File main = new File(project, "target/jar-with-unpack-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).hasUnpackEntryWithNameStartingWith("BOOT-INF/lib/spring-core-")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context-");
		});
	}

	@TestTemplate
	void whenJarIsRepackagedWithACustomLayoutTheJarUsesTheLayout(MavenBuild mavenBuild) {
		mavenBuild.project("jar-custom-layout").execute((project) -> {
			assertThat(jar(new File(project, "custom/target/custom-0.0.1.BUILD-SNAPSHOT.jar")))
				.hasEntryWithName("custom");
			assertThat(jar(new File(project, "default/target/default-0.0.1.BUILD-SNAPSHOT.jar")))
				.hasEntryWithName("sample");
		});
	}

	@TestTemplate
	void repackagedJarContainsTheLayersIndexByDefault(MavenBuild mavenBuild) {
		mavenBuild.project("jar-layered").execute((project) -> {
			File repackaged = new File(project, "jar/target/jar-layered-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jar-release")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jar-snapshot")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/" + JarModeLibrary.TOOLS.getCoordinates().getArtifactId());
			try (JarFile jarFile = new JarFile(repackaged)) {
				Map<String, List<String>> layerIndex = readLayerIndex(jarFile);
				assertThat(layerIndex.keySet()).containsExactly("dependencies", "spring-boot-loader",
						"snapshot-dependencies", "application");
				assertThat(layerIndex.get("application")).contains("BOOT-INF/lib/jar-release-0.0.1.RELEASE.jar",
						"BOOT-INF/lib/jar-snapshot-0.0.1.BUILD-SNAPSHOT.jar");
				assertThat(layerIndex.get("dependencies"))
					.anyMatch((dependency) -> dependency.startsWith("BOOT-INF/lib/log4j-api-"));
			}
			catch (IOException ex) {
				// Ignore
			}
		});
	}

	@TestTemplate
	void whenJarIsRepackagedWithTheLayersDisabledDoesNotContainLayersIndex(MavenBuild mavenBuild) {
		mavenBuild.project("jar-layered-disabled").execute((project) -> {
			File repackaged = new File(project, "jar/target/jar-layered-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jar-release")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jar-snapshot")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/" + JarModeLibrary.TOOLS.getCoordinates().getArtifactId())
				.doesNotHaveEntryWithName("BOOT-INF/layers.idx");
		});
	}

	@TestTemplate
	void whenJarIsRepackagedWithToolsExclude(MavenBuild mavenBuild) {
		mavenBuild.project("jar-no-tools").execute((project) -> {
			File repackaged = new File(project, "jar/target/jar-no-tools-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jar-release")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jar-snapshot")
				.doesNotHaveEntryWithNameStartingWith(
						"BOOT-INF/lib/" + JarModeLibrary.TOOLS.getCoordinates().getArtifactId());
		});
	}

	@TestTemplate
	void whenJarIsRepackagedWithTheCustomLayers(MavenBuild mavenBuild) {
		mavenBuild.project("jar-layered-custom").execute((project) -> {
			File repackaged = new File(project, "jar/target/jar-layered-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jar-release")
				.hasEntryWithNameStartingWith("BOOT-INF/lib/jar-snapshot");
			try (JarFile jarFile = new JarFile(repackaged)) {
				Map<String, List<String>> layerIndex = readLayerIndex(jarFile);
				assertThat(layerIndex.keySet()).containsExactly("my-dependencies-name", "snapshot-dependencies",
						"configuration", "application");
				assertThat(layerIndex.get("application"))
					.contains("BOOT-INF/lib/jar-release-0.0.1.RELEASE.jar",
							"BOOT-INF/lib/jar-snapshot-0.0.1.BUILD-SNAPSHOT.jar",
							"BOOT-INF/lib/jar-classifier-0.0.1-bravo.jar")
					.doesNotContain("BOOT-INF/lib/jar-classifier-0.0.1-alpha.jar");
			}
		});
	}

	@TestTemplate
	void repackagedJarContainsClasspathIndex(MavenBuild mavenBuild) {
		mavenBuild.project("jar").execute((project) -> {
			File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged))
				.manifest((manifest) -> manifest.hasAttribute("Spring-Boot-Classpath-Index", "BOOT-INF/classpath.idx"));
			assertThat(jar(repackaged)).hasEntryWithName("BOOT-INF/classpath.idx");
			try (JarFile jarFile = new JarFile(repackaged)) {
				List<String> index = readClasspathIndex(jarFile, "BOOT-INF/classpath.idx");
				assertThat(index).allMatch((entry) -> entry.startsWith("BOOT-INF/lib/"));
			}
		});
	}

	@TestTemplate
	void whenJarIsRepackagedWithOutputTimestampConfiguredThenJarIsReproducible(MavenBuild mavenBuild)
			throws InterruptedException {
		String firstHash = buildJarWithOutputTimestamp(mavenBuild);
		Thread.sleep(1500);
		String secondHash = buildJarWithOutputTimestamp(mavenBuild);
		assertThat(firstHash).isEqualTo(secondHash);
	}

	private String buildJarWithOutputTimestamp(MavenBuild mavenBuild) {
		AtomicReference<String> jarHash = new AtomicReference<>();
		mavenBuild.project("jar-output-timestamp").execute((project) -> {
			File repackaged = new File(project, "target/jar-output-timestamp-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(repackaged).isFile();
			long expectedModified = 1584352800000L;
			long offsetExpectedModified = expectedModified - TimeZone.getDefault().getOffset(expectedModified);
			assertThat(repackaged.lastModified()).isEqualTo(expectedModified);
			try (JarFile jar = new JarFile(repackaged)) {
				List<String> unreproducibleEntries = jar.stream()
					.filter((entry) -> entry.getLastModifiedTime().toMillis() != offsetExpectedModified)
					.map((entry) -> entry.getName() + ": " + entry.getLastModifiedTime())
					.toList();
				assertThat(unreproducibleEntries).isEmpty();
				jarHash.set(FileUtils.sha1Hash(repackaged));
				FileSystemUtils.deleteRecursively(project);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
		return jarHash.get();
	}

	@TestTemplate
	void whenJarIsRepackagedWithOutputTimestampConfiguredThenLibrariesAreSorted(MavenBuild mavenBuild) {
		mavenBuild.project("jar-output-timestamp").execute((project) -> {
			File repackaged = new File(project, "target/jar-output-timestamp-0.0.1.BUILD-SNAPSHOT.jar");
			List<String> sortedLibs = Arrays.asList("BOOT-INF/lib/commons-logging", "BOOT-INF/lib/jakarta.servlet-api",
					"BOOT-INF/lib/jspecify", "BOOT-INF/lib/micrometer-commons", "BOOT-INF/lib/micrometer-observation",
					"BOOT-INF/lib/spring-aop", "BOOT-INF/lib/spring-beans",
					"BOOT-INF/lib/" + JarModeLibrary.TOOLS.getCoordinates().getArtifactId(),
					"BOOT-INF/lib/spring-context", "BOOT-INF/lib/spring-core", "BOOT-INF/lib/spring-expression");
			assertThat(jar(repackaged)).entryNamesInPath("BOOT-INF/lib/")
				.zipSatisfy(sortedLibs,
						(String jarLib, String expectedLib) -> assertThat(jarLib).startsWith(expectedLib));
		});
	}

	@TestTemplate
	void whenSigned(MavenBuild mavenBuild) {
		mavenBuild.project("jar-signed").execute((project) -> {
			File repackaged = new File(project, "target/jar-signed-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithName("META-INF/BOOT.SF");
		});
	}

}
