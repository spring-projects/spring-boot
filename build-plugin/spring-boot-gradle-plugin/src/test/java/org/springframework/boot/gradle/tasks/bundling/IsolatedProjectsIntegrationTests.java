/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.tasks.bundling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for archive coordinates and Gradle's isolated projects.
 *
 * @author Greg Taube
 */
@GradleCompatibility
class IsolatedProjectsIntegrationTests {

	@SuppressWarnings("NullAway.Init")
	GradleBuild gradleBuild;

	@TestTemplate
	void bootJarUsesResolvedProjectCoordinates() throws IOException {
		assertArchiveCoordinates("bootJar", "BOOT-INF/", false);
	}

	@TestTemplate
	void bootWarUsesResolvedProjectCoordinates() throws IOException {
		assertArchiveCoordinates("bootWar", "WEB-INF/", false);
	}

	@TestTemplate
	void bootJarSupportsIsolatedProjectsAndConfigurationCacheReuse() throws IOException {
		assertArchiveCoordinates("bootJar", "BOOT-INF/", true);
	}

	@TestTemplate
	void bootWarSupportsIsolatedProjectsAndConfigurationCacheReuse() throws IOException {
		assertArchiveCoordinates("bootWar", "WEB-INF/", true);
	}

	@TestTemplate
	void projectCoordinateChangesInvalidateArchiveWithoutChangingLibraryContents() throws IOException {
		createBuild();
		build("bootJar", true, false);
		Path library = projectPath("library/build/libs/renamed-main.jar");
		byte[] originalLibrary = Files.readAllBytes(library);
		assertThat(layers("bootJar", "BOOT-INF/")).containsSequence("actual", "BOOT-INF/lib/renamed-main.jar");
		writeFile("library/version.txt", "3.2-SNAPSHOT");
		BuildResult changed = build("bootJar", true, false);
		assertThat(Objects.requireNonNull(changed.task(":app:bootJar")).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(Files.readAllBytes(library)).isEqualTo(originalLibrary);
		assertThat(layers("bootJar", "BOOT-INF/")).doesNotContainSequence("actual", "BOOT-INF/lib/renamed-main.jar")
			.containsSequence("projects", "BOOT-INF/lib/renamed-main.jar");
	}

	@TestTemplate
	void artifactOnlyRegistrationPreservesCustomArchiveCoordinates() throws IOException {
		createBuild();
		customArchive(false);
		build("customArchive", false, false);
		assertThat(layers("customArchive", "BOOT-INF/")).containsSequence("actual", "BOOT-INF/lib/renamed-main.jar");
	}

	@TestTemplate
	void graphRegistrationSupportsArtifactViewsAndConfigurationCacheReuse() throws IOException {
		createBuild();
		customArchive(true);
		build("customArchive", true, true);
		BuildResult reused = build("customArchive", true, true);
		assertThat(reused.getOutput()).contains("Reusing configuration cache");
		assertThat(Objects.requireNonNull(reused.task(":app:customArchive")).getOutcome())
			.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(layers("customArchive", "BOOT-INF/")).containsSequence("actual", "BOOT-INF/lib/");
		try (JarFile jar = new JarFile(projectPath("app/build/libs/customArchive.jar").toFile())) {
			assertThat(jar.getJarEntry("BOOT-INF/lib/renamed-main.jar")).isNotNull();
			assertThat(jar.getJarEntry("BOOT-INF/lib/commons-lang3-3.9.jar")).isNull();
		}
	}

	@TestTemplate
	void bootBuildImageTaskCanBeConfiguredWithIsolatedProjects() throws IOException {
		createBuild();
		this.gradleBuild.configurationCache();
		BuildResult result = this.gradleBuild.build(":app:bootBuildImage", "--dry-run",
				this.gradleBuild.gradleVersionIsAtLeast("9.7") ? "--isolated-projects"
						: "-Dorg.gradle.unsafe.isolated-projects=true");
		assertThat(result.getOutput()).contains(":app:bootBuildImage SKIPPED");
	}

	@TestTemplate
	void bootJarUsesProjectReachabilityMetadataWithLayersDisabled() throws IOException {
		createBuild();
		writeFile("app/src/main/resources/META-INF/native-image/other/library/2.0/reachability-metadata.properties",
				"override=true");
		Files.writeString(projectPath("app/build.gradle"),
				"tasks.withType(org.springframework.boot.gradle.tasks.bundling.BootArchive).configureEach { layered.enabled = false }\n",
				StandardOpenOption.APPEND);
		build("bootJar", true, false);
		try (JarFile jar = new JarFile(projectPath("app/build/libs/bootJar.jar").toFile())) {
			assertThat(jar.getJarEntry("BOOT-INF/layers.idx")).isNull();
			String args = new String(
					jar.getInputStream(jar.getJarEntry("META-INF/native-image/argfile")).readAllBytes(),
					StandardCharsets.UTF_8);
			assertThat(args).contains("--exclude-config", "\\Qincluded-lib.jar\\E");
		}
	}

	@TestTemplate
	void toolingModelCanRealizeArchiveTasksWithIsolatedProjects() throws IOException {
		createBuild();
		this.gradleBuild.configurationCache();
		GradleRunner runner = this.gradleBuild.prepareRunner("help");
		// Tooling API requests do not use TestKit's plugin classpath injection.
		String classpath = runner.getPluginClasspath()
			.stream()
			.map((file) -> "'" + file.getAbsolutePath().replace("\\", "\\\\").replace("'", "\\'") + "'")
			.collect(Collectors.joining(", "));
		writeFile("build.gradle", "buildscript { dependencies { classpath files(" + classpath + ") } }\n");
		try (ProjectConnection connection = GradleConnector.newConnector()
			.forProjectDirectory(this.gradleBuild.getProjectDir())
			.useGradleVersion(this.gradleBuild.getGradleVersion())
			.connect()) {
			GradleProject model = connection.model(GradleProject.class)
				.withArguments(this.gradleBuild.gradleVersionIsAtLeast("9.7") ? "--isolated-projects"
						: "-Dorg.gradle.unsafe.isolated-projects=true")
				.get();
			assertThat(model.getChildren()).filteredOn((project) -> project.getName().equals("app"))
				.singleElement()
				.satisfies((app) -> assertThat(app.getTasks()).extracting((task) -> task.getName())
					.contains("bootJar", "bootWar", "bootBuildImage"));
		}
	}

	@TestTemplate
	void graphRegistrationSupportsKotlinDslAndConfigurationCacheReuse() throws IOException {
		createBuild();
		Files.delete(projectPath("app/build.gradle"));
		writeFile("app/build.gradle.kts",
				"""
						import org.springframework.boot.gradle.tasks.bundling.BootJar

						plugins { java; id("org.springframework.boot") }
						dependencies {
						    implementation(project(":library")) { capabilities { requireCapability("unrelated:feature") } }
						}
						tasks.register<BootJar>("customArchive") {
						    mainClass.set("example.Main")
						    targetJavaVersion.set(JavaVersion.current())
						    archiveFileName.set("customArchive.jar")
						    includeTools.set(false)
						    val runtime = configurations["runtimeClasspath"]
						    classpath(sourceSets["main"].output, runtime)
						    resolvedArtifacts(runtime.incoming.artifacts.resolvedArtifacts, runtime.incoming.resolutionResult.rootComponent)
						    layered {
						        dependencies {
						            intoLayer("library") { include("actual.group:library:3.1-SNAPSHOT") }
						            intoLayer("dependencies")
						        }
						        application { intoLayer("application") }
						        layerOrder.set(listOf("library", "dependencies", "application"))
						    }
						}
						""");
		build("customArchive", true, true);
		BuildResult reused = build("customArchive", true, true);
		assertThat(reused.getOutput()).contains("Reusing configuration cache");
		assertThat(Objects.requireNonNull(reused.task(":app:customArchive")).getOutcome())
			.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(layers("customArchive", "BOOT-INF/")).containsSequence("library", "BOOT-INF/lib/");
	}

	private void assertArchiveCoordinates(String task, String prefix, boolean isolated) throws IOException {
		createBuild();
		build(task, isolated, true);
		List<String> original = layers(task, prefix);
		assertThat(original).containsSequence("actual", prefix + "lib/renamed-main.jar")
			.containsSequence("included", prefix + "lib/included-lib.jar")
			.containsSequence("projects", prefix + "lib/bridge-1.0.jar")
			.containsSequence("external", prefix + "lib/commons-lang3-3.9.jar");
		BuildResult reused = build(task, isolated, true);
		assertThat(reused.getOutput()).contains("Reusing configuration cache");
		assertThat(Objects.requireNonNull(reused.task(":app:" + task)).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(layers(task, prefix)).isEqualTo(original);
	}

	private BuildResult build(String task, boolean isolated, boolean rerun) {
		this.gradleBuild.configurationCache();
		List<String> arguments = new ArrayList<>(List.of(":app:" + task));
		if (isolated) {
			arguments.add(this.gradleBuild.gradleVersionIsAtLeast("9.7") ? "--isolated-projects"
					: "-Dorg.gradle.unsafe.isolated-projects=true");
		}
		if (rerun) {
			arguments.add("--rerun-tasks");
		}
		return this.gradleBuild.build(arguments.toArray(String[]::new));
	}

	private List<String> layers(String task, String prefix) throws IOException {
		try (JarFile jar = new JarFile(projectPath("app/build/libs/" + task + ".jar").toFile())) {
			String index = new String(jar.getInputStream(jar.getJarEntry(prefix + "layers.idx")).readAllBytes(),
					StandardCharsets.UTF_8);
			List<String> entries = new ArrayList<>();
			String layer = "";
			for (String line : index.lines().toList()) {
				if (line.startsWith("- ")) {
					layer = line.substring(3, line.length() - 2);
				}
				else if (line.startsWith("  - ")) {
					entries.add(layer);
					entries.add(line.substring(5, line.length() - 1));
				}
			}
			return entries;
		}
	}

	private void createBuild() throws IOException {
		writeFile("settings.gradle",
				"rootProject.name = 'archives'\ninclude 'app', 'library'\nincludeBuild 'included'\n");
		writeFile("app/build.gradle", """
				plugins {
				    id 'java'
				    id 'war'
				    id 'org.springframework.boot'
				}
				repositories { mavenCentral() }
				dependencies {
				    implementation(project(':library')) { capabilities { requireCapability('unrelated:feature') } }
				    implementation 'included:bridge:1.0'
				    implementation 'org.apache.commons:commons-lang3:3.9'
				}
				tasks.withType(org.springframework.boot.gradle.tasks.bundling.BootArchive).configureEach {
				    mainClass = 'example.Main'
				    targetJavaVersion = JavaVersion.current()
				    includeTools = false
				    archiveFileName = name + '.jar'
				    layered {
				        dependencies {
				            intoLayer('actual') { include 'actual.group:library:3.1-SNAPSHOT' }
				            intoLayer('included') { include 'other:library:2.0' }
				            intoLayer('projects') { includeProjectDependencies() }
				            intoLayer('external')
				        }
				        application { intoLayer('application') }
				        layerOrder = ['actual', 'included', 'projects', 'external', 'application']
				    }
				}
				""");
		writeFile("app/src/main/java/example/Main.java",
				"package example; public class Main { public static void main(String[] args) {} }");
		writeFile("library/build.gradle",
				"""
						plugins { id 'java-library'; id 'maven-publish' }
						group = 'actual.group'
						version = 'initial'
						afterEvaluate { version = providers.fileContents(layout.projectDirectory.file('version.txt')).asText.get().trim() }
						tasks.named('jar') { archiveFileName = 'renamed-main.jar' }
						configurations.runtimeElements.outgoing.capability('unrelated:feature:9.0')
						publishing {
						    publications {
						        custom(MavenPublication) {
						            from components.java
						            groupId = 'published.group'
						            artifactId = 'published-name'
						            version = '4.0'
						        }
						    }
						}
						""");
		writeFile("library/version.txt", "3.1-SNAPSHOT");
		writeFile("library/src/main/resources/library.txt", "main library");
		writeFile("included/settings.gradle", "rootProject.name = 'included'\ninclude 'bridge', 'library'\n");
		writeFile("included/bridge/build.gradle", """
				plugins { id 'java-library' }
				group = 'included'
				version = '1.0'
				dependencies { api project(':library') }
				""");
		writeFile("included/library/build.gradle", """
				plugins { id 'java-library' }
				group = 'other'
				version = '2.0'
				tasks.named('jar') { archiveFileName = 'included-lib.jar' }
				""");
		writeFile("included/library/src/main/resources/library.txt", "included library");
	}

	private void customArchive(boolean graph) throws IOException {
		String registration = graph ? """
				def incoming = configurations.customRuntime.incoming
				def artifacts = incoming.artifactView {
				    componentFilter { it instanceof org.gradle.api.artifacts.component.ProjectComponentIdentifier }
				}.artifacts
				classpath = sourceSets.main.output + artifacts.artifactFiles
				resolvedArtifacts(artifacts.resolvedArtifacts, incoming.resolutionResult.rootComponent)
				""" : """
				classpath = sourceSets.main.output + configurations.customRuntime
				resolvedArtifacts(configurations.customRuntime.incoming.artifacts.resolvedArtifacts)
				""";
		Files.writeString(projectPath("app/build.gradle"),
				"""
						configurations { customRuntime { canBeConsumed = false } }
						configurations.customRuntime.attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
						dependencies {
						    customRuntime(project(':library')) { capabilities { requireCapability('unrelated:feature') } }
						    customRuntime 'org.apache.commons:commons-lang3:3.9'
						}
						tasks.register('customArchive', org.springframework.boot.gradle.tasks.bundling.BootJar) {
						"""
						+ registration + "}\n",
				StandardOpenOption.APPEND);
	}

	private void writeFile(String relativePath, String content) throws IOException {
		Path file = projectPath(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

	private Path projectPath(String path) {
		return this.gradleBuild.getProjectDir().toPath().resolve(path);
	}

}
