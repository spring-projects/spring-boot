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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.boot.loader.tools.FileUtils;
import org.springframework.boot.loader.tools.JarModeLibrary;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BootJar} and {@link BootWar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
abstract class AbstractBootArchiveIntegrationTests {

	private final String taskName;

	private final String libPath;

	private final String classesPath;

	private final String indexPath;

	GradleBuild gradleBuild;

	protected AbstractBootArchiveIntegrationTests(String taskName, String libPath, String classesPath,
			String indexPath) {
		this.taskName = taskName;
		this.libPath = libPath;
		this.classesPath = classesPath;
		this.indexPath = indexPath;
	}

	@TestTemplate
	void basicBuild() {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@Deprecated
	@TestTemplate
	void basicBuildUsingDeprecatedMainClassName() {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void reproducibleArchive() throws IOException, InterruptedException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		File jar = new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0];
		String firstHash = FileUtils.sha1Hash(jar);
		Thread.sleep(1500);
		assertThat(this.gradleBuild.build("clean", this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		String secondHash = FileUtils.sha1Hash(jar);
		assertThat(firstHash).isEqualTo(secondHash);
	}

	@TestTemplate
	void upToDateWhenBuiltTwice() {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@TestTemplate
	void upToDateWhenBuiltTwiceWithLaunchScriptIncluded() {
		assertThat(this.gradleBuild.build("-PincludeLaunchScript=true", this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-PincludeLaunchScript=true", this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@TestTemplate
	void notUpToDateWhenLaunchScriptWasNotIncludedAndThenIsIncluded() {
		assertThat(this.gradleBuild.scriptProperty("launchScript", "").build(this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.scriptProperty("launchScript", "launchScript()").build(this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void notUpToDateWhenLaunchScriptWasIncludedAndThenIsNotIncluded() {
		assertThat(this.gradleBuild.scriptProperty("launchScript", "launchScript()").build(this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.scriptProperty("launchScript", "").build(this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void notUpToDateWhenLaunchScriptPropertyChanges() {
		assertThat(this.gradleBuild.scriptProperty("launchScriptProperty", "alpha").build(this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.scriptProperty("launchScriptProperty", "bravo").build(this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void applicationPluginMainClassNameIsUsed() throws IOException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.CustomMain");
		}
	}

	@TestTemplate
	void springBootExtensionMainClassNameIsUsed() throws IOException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.CustomMain");
		}
	}

	@TestTemplate
	void duplicatesAreHandledGracefully() {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void developmentOnlyDependenciesAreNotIncludedInTheArchiveByDefault() throws IOException {
		File srcMainResources = new File(this.gradleBuild.getProjectDir(), "src/main/resources");
		srcMainResources.mkdirs();
		new File(srcMainResources, "resource").createNewFile();
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			Stream<String> libEntryNames = jarFile.stream().filter((entry) -> !entry.isDirectory())
					.map(JarEntry::getName).filter((name) -> name.startsWith(this.libPath));
			assertThat(libEntryNames).containsExactly(this.libPath + "commons-io-2.6.jar");
			Stream<String> classesEntryNames = jarFile.stream().filter((entry) -> !entry.isDirectory())
					.map(JarEntry::getName).filter((name) -> name.startsWith(this.classesPath));
			assertThat(classesEntryNames).containsExactly(this.classesPath + "resource");
		}
	}

	@TestTemplate
	void developmentOnlyDependenciesCanBeIncludedInTheArchive() throws IOException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			Stream<String> libEntryNames = jarFile.stream().filter((entry) -> !entry.isDirectory())
					.map(JarEntry::getName).filter((name) -> name.startsWith(this.libPath));
			assertThat(libEntryNames).containsExactly(this.libPath + "commons-io-2.6.jar",
					this.libPath + "commons-lang3-3.9.jar");
		}
	}

	@TestTemplate
	void jarTypeFilteringIsApplied() throws IOException {
		File flatDirRepository = new File(this.gradleBuild.getProjectDir(), "repository");
		createDependenciesStarterJar(new File(flatDirRepository, "starter.jar"));
		createStandardJar(new File(flatDirRepository, "standard.jar"));
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			Stream<String> libEntryNames = jarFile.stream().filter((entry) -> !entry.isDirectory())
					.map(JarEntry::getName).filter((name) -> name.startsWith(this.libPath));
			assertThat(libEntryNames).containsExactly(this.libPath + "standard.jar");
		}
	}

	@TestTemplate
	void startClassIsSetByResolvingTheMainClass() throws IOException {
		copyMainClassApplication();
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			Attributes mainAttributes = jarFile.getManifest().getMainAttributes();
			assertThat(mainAttributes.getValue("Start-Class"))
					.isEqualTo("com.example." + this.taskName.toLowerCase(Locale.ENGLISH) + ".main.CustomMainClass");
		}
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@TestTemplate
	void upToDateWhenBuiltWithDefaultLayeredAndThenWithExplicitLayered() {
		assertThat(this.gradleBuild.scriptProperty("layered", "").build("" + this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.scriptProperty("layered", "layered {}").build("" + this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@TestTemplate
	void notUpToDateWhenBuiltWithoutLayersAndThenWithLayers() {
		assertThat(this.gradleBuild.scriptProperty("layerEnablement", "enabled = false").build(this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.scriptProperty("layerEnablement", "enabled = true").build(this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void notUpToDateWhenBuiltWithLayerToolsAndThenWithoutLayerTools() {
		assertThat(this.gradleBuild.scriptProperty("layerTools", "").build(this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.scriptProperty("layerTools", "includeLayerTools = false").build(this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void layersWithCustomSourceSet() {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void implicitLayers() throws IOException {
		writeMainClass();
		writeResource();
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		Map<String, List<String>> indexedLayers;
		String layerToolsJar = this.libPath + JarModeLibrary.LAYER_TOOLS.getName();
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getEntry(layerToolsJar)).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "commons-lang3-3.9.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "spring-core-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "spring-jcl-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "library-1.0-SNAPSHOT.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.classesPath + "example/Main.class")).isNotNull();
			assertThat(jarFile.getEntry(this.classesPath + "static/file.txt")).isNotNull();
			indexedLayers = readLayerIndex(jarFile);
		}
		List<String> layerNames = Arrays.asList("dependencies", "spring-boot-loader", "snapshot-dependencies",
				"application");
		assertThat(indexedLayers.keySet()).containsExactlyElementsOf(layerNames);
		Set<String> expectedDependencies = new TreeSet<>();
		expectedDependencies.add(this.libPath + "commons-lang3-3.9.jar");
		expectedDependencies.add(this.libPath + "spring-core-5.2.5.RELEASE.jar");
		expectedDependencies.add(this.libPath + "spring-jcl-5.2.5.RELEASE.jar");
		expectedDependencies.add(this.libPath + "jul-to-slf4j-1.7.28.jar");
		expectedDependencies.add(this.libPath + "log4j-api-2.12.1.jar");
		expectedDependencies.add(this.libPath + "log4j-to-slf4j-2.12.1.jar");
		expectedDependencies.add(this.libPath + "logback-classic-1.2.3.jar");
		expectedDependencies.add(this.libPath + "logback-core-1.2.3.jar");
		expectedDependencies.add(this.libPath + "slf4j-api-1.7.28.jar");
		expectedDependencies.add(this.libPath + "spring-boot-starter-logging-2.2.0.RELEASE.jar");
		Set<String> expectedSnapshotDependencies = new TreeSet<>();
		expectedSnapshotDependencies.add(this.libPath + "library-1.0-SNAPSHOT.jar");
		(layerToolsJar.contains("SNAPSHOT") ? expectedSnapshotDependencies : expectedDependencies).add(layerToolsJar);
		assertThat(indexedLayers.get("dependencies")).containsExactlyElementsOf(expectedDependencies);
		assertThat(indexedLayers.get("spring-boot-loader")).containsExactly("org/");
		assertThat(indexedLayers.get("snapshot-dependencies")).containsExactlyElementsOf(expectedSnapshotDependencies);
		assertThat(indexedLayers.get("application"))
				.containsExactly(getExpectedApplicationLayerContents(this.classesPath));
		BuildResult listLayers = this.gradleBuild.build("listLayers");
		assertThat(listLayers.task(":listLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		String listLayersOutput = listLayers.getOutput();
		assertThat(new BufferedReader(new StringReader(listLayersOutput)).lines()).containsSequence(layerNames);
		BuildResult extractLayers = this.gradleBuild.build("extractLayers");
		assertThat(extractLayers.task(":extractLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertExtractedLayers(layerNames, indexedLayers);
	}

	abstract String[] getExpectedApplicationLayerContents(String... additionalFiles);

	@TestTemplate
	void multiModuleImplicitLayers() throws IOException {
		writeSettingsGradle();
		writeMainClass();
		writeResource();
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		Map<String, List<String>> indexedLayers;
		String layerToolsJar = this.libPath + JarModeLibrary.LAYER_TOOLS.getName();
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getEntry(layerToolsJar)).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "alpha-1.2.3.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "bravo-1.2.3.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "charlie-1.2.3.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "commons-lang3-3.9.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "spring-core-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "spring-jcl-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "library-1.0-SNAPSHOT.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.classesPath + "example/Main.class")).isNotNull();
			assertThat(jarFile.getEntry(this.classesPath + "static/file.txt")).isNotNull();
			indexedLayers = readLayerIndex(jarFile);
		}
		List<String> layerNames = Arrays.asList("dependencies", "spring-boot-loader", "snapshot-dependencies",
				"application");
		assertThat(indexedLayers.keySet()).containsExactlyElementsOf(layerNames);
		Set<String> expectedDependencies = new TreeSet<>();
		expectedDependencies.add(this.libPath + "commons-lang3-3.9.jar");
		expectedDependencies.add(this.libPath + "spring-core-5.2.5.RELEASE.jar");
		expectedDependencies.add(this.libPath + "spring-jcl-5.2.5.RELEASE.jar");
		Set<String> expectedSnapshotDependencies = new TreeSet<>();
		expectedSnapshotDependencies.add(this.libPath + "library-1.0-SNAPSHOT.jar");
		(layerToolsJar.contains("SNAPSHOT") ? expectedSnapshotDependencies : expectedDependencies).add(layerToolsJar);
		assertThat(indexedLayers.get("dependencies")).containsExactlyElementsOf(expectedDependencies);
		assertThat(indexedLayers.get("spring-boot-loader")).containsExactly("org/");
		assertThat(indexedLayers.get("snapshot-dependencies")).containsExactlyElementsOf(expectedSnapshotDependencies);
		assertThat(indexedLayers.get("application"))
				.containsExactly(getExpectedApplicationLayerContents(this.classesPath, this.libPath + "alpha-1.2.3.jar",
						this.libPath + "bravo-1.2.3.jar", this.libPath + "charlie-1.2.3.jar"));
		BuildResult listLayers = this.gradleBuild.build("listLayers");
		assertThat(listLayers.task(":listLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		String listLayersOutput = listLayers.getOutput();
		assertThat(new BufferedReader(new StringReader(listLayersOutput)).lines()).containsSequence(layerNames);
		BuildResult extractLayers = this.gradleBuild.build("extractLayers");
		assertThat(extractLayers.task(":extractLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertExtractedLayers(layerNames, indexedLayers);
	}

	@TestTemplate
	void customLayers() throws IOException {
		writeMainClass();
		writeResource();
		BuildResult build = this.gradleBuild.build(this.taskName);
		assertThat(build.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Map<String, List<String>> indexedLayers;
		String layerToolsJar = this.libPath + JarModeLibrary.LAYER_TOOLS.getName();
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getEntry(layerToolsJar)).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "commons-lang3-3.9.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "spring-core-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "spring-jcl-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "library-1.0-SNAPSHOT.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.classesPath + "example/Main.class")).isNotNull();
			assertThat(jarFile.getEntry(this.classesPath + "static/file.txt")).isNotNull();
			assertThat(jarFile.getEntry(this.indexPath + "layers.idx")).isNotNull();
			indexedLayers = readLayerIndex(jarFile);
		}
		List<String> layerNames = Arrays.asList("dependencies", "commons-dependencies", "snapshot-dependencies",
				"static", "app");
		assertThat(indexedLayers.keySet()).containsExactlyElementsOf(layerNames);
		Set<String> expectedDependencies = new TreeSet<>();
		expectedDependencies.add(this.libPath + "spring-core-5.2.5.RELEASE.jar");
		expectedDependencies.add(this.libPath + "spring-jcl-5.2.5.RELEASE.jar");
		List<String> expectedSnapshotDependencies = new ArrayList<>();
		expectedSnapshotDependencies.add(this.libPath + "library-1.0-SNAPSHOT.jar");
		(layerToolsJar.contains("SNAPSHOT") ? expectedSnapshotDependencies : expectedDependencies).add(layerToolsJar);
		assertThat(indexedLayers.get("dependencies")).containsExactlyElementsOf(expectedDependencies);
		assertThat(indexedLayers.get("commons-dependencies")).containsExactly(this.libPath + "commons-lang3-3.9.jar");
		assertThat(indexedLayers.get("snapshot-dependencies")).containsExactlyElementsOf(expectedSnapshotDependencies);
		assertThat(indexedLayers.get("static")).containsExactly(this.classesPath + "static/");
		List<String> appLayer = new ArrayList<>(indexedLayers.get("app"));
		String[] appLayerContents = getExpectedApplicationLayerContents(this.classesPath + "example/");
		assertThat(appLayer).containsSubsequence(appLayerContents);
		appLayer.removeAll(Arrays.asList(appLayerContents));
		assertThat(appLayer).containsExactly("org/");
		BuildResult listLayers = this.gradleBuild.build("listLayers");
		assertThat(listLayers.task(":listLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		String listLayersOutput = listLayers.getOutput();
		assertThat(new BufferedReader(new StringReader(listLayersOutput)).lines()).containsSequence(layerNames);
		BuildResult extractLayers = this.gradleBuild.build("extractLayers");
		assertThat(extractLayers.task(":extractLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertExtractedLayers(layerNames, indexedLayers);
	}

	@TestTemplate
	void multiModuleCustomLayers() throws IOException {
		writeSettingsGradle();
		writeMainClass();
		writeResource();
		BuildResult build = this.gradleBuild.build(this.taskName);
		assertThat(build.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Map<String, List<String>> indexedLayers;
		String layerToolsJar = this.libPath + JarModeLibrary.LAYER_TOOLS.getName();
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getEntry(layerToolsJar)).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "alpha-1.2.3.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "bravo-1.2.3.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "charlie-1.2.3.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "commons-lang3-3.9.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "spring-core-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "spring-jcl-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "library-1.0-SNAPSHOT.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.classesPath + "example/Main.class")).isNotNull();
			assertThat(jarFile.getEntry(this.classesPath + "static/file.txt")).isNotNull();
			assertThat(jarFile.getEntry(this.indexPath + "layers.idx")).isNotNull();
			indexedLayers = readLayerIndex(jarFile);
		}
		List<String> layerNames = Arrays.asList("dependencies", "commons-dependencies", "snapshot-dependencies",
				"subproject-dependencies", "static", "app");
		assertThat(indexedLayers.keySet()).containsExactlyElementsOf(layerNames);
		Set<String> expectedSubprojectDependencies = new TreeSet<>();
		expectedSubprojectDependencies.add(this.libPath + "alpha-1.2.3.jar");
		expectedSubprojectDependencies.add(this.libPath + "bravo-1.2.3.jar");
		expectedSubprojectDependencies.add(this.libPath + "charlie-1.2.3.jar");
		Set<String> expectedDependencies = new TreeSet<>();
		expectedDependencies.add(this.libPath + "spring-core-5.2.5.RELEASE.jar");
		expectedDependencies.add(this.libPath + "spring-jcl-5.2.5.RELEASE.jar");
		List<String> expectedSnapshotDependencies = new ArrayList<>();
		expectedSnapshotDependencies.add(this.libPath + "library-1.0-SNAPSHOT.jar");
		(layerToolsJar.contains("SNAPSHOT") ? expectedSnapshotDependencies : expectedDependencies).add(layerToolsJar);
		assertThat(indexedLayers.get("subproject-dependencies"))
				.containsExactlyElementsOf(expectedSubprojectDependencies);
		assertThat(indexedLayers.get("dependencies")).containsExactlyElementsOf(expectedDependencies);
		assertThat(indexedLayers.get("commons-dependencies")).containsExactly(this.libPath + "commons-lang3-3.9.jar");
		assertThat(indexedLayers.get("snapshot-dependencies")).containsExactlyElementsOf(expectedSnapshotDependencies);
		assertThat(indexedLayers.get("static")).containsExactly(this.classesPath + "static/");
		List<String> appLayer = new ArrayList<>(indexedLayers.get("app"));
		String[] appLayerContents = getExpectedApplicationLayerContents(this.classesPath + "example/");
		assertThat(appLayer).containsSubsequence(appLayerContents);
		appLayer.removeAll(Arrays.asList(appLayerContents));
		assertThat(appLayer).containsExactly("org/");
		BuildResult listLayers = this.gradleBuild.build("listLayers");
		assertThat(listLayers.task(":listLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		String listLayersOutput = listLayers.getOutput();
		assertThat(new BufferedReader(new StringReader(listLayersOutput)).lines()).containsSequence(layerNames);
		BuildResult extractLayers = this.gradleBuild.build("extractLayers");
		assertThat(extractLayers.task(":extractLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertExtractedLayers(layerNames, indexedLayers);
	}

	private void copyMainClassApplication() throws IOException {
		copyApplication("main");
	}

	protected void copyApplication(String name) throws IOException {
		File output = new File(this.gradleBuild.getProjectDir(),
				"src/main/java/com/example/" + this.taskName.toLowerCase() + "/" + name);
		output.mkdirs();
		FileSystemUtils.copyRecursively(
				new File("src/test/java/com/example/" + this.taskName.toLowerCase(Locale.ENGLISH) + "/" + name),
				output);
	}

	private void createStandardJar(File location) throws IOException {
		createJar(location, (attributes) -> {
		});
	}

	private void createDependenciesStarterJar(File location) throws IOException {
		createJar(location, (attributes) -> attributes.putValue("Spring-Boot-Jar-Type", "dependencies-starter"));
	}

	private void createJar(File location, Consumer<Attributes> attributesConfigurer) throws IOException {
		location.getParentFile().mkdirs();
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		attributesConfigurer.accept(attributes);
		new JarOutputStream(new FileOutputStream(location), manifest).close();
	}

	private void writeSettingsGradle() {
		try (PrintWriter writer = new PrintWriter(
				new FileWriter(new File(this.gradleBuild.getProjectDir(), "settings.gradle")))) {
			writer.println("include 'alpha', 'bravo', 'charlie'");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void writeMainClass() {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/example");
		examplePackage.mkdirs();
		File main = new File(examplePackage, "Main.java");
		try (PrintWriter writer = new PrintWriter(new FileWriter(main))) {
			writer.println("package example;");
			writer.println();
			writer.println("import java.io.IOException;");
			writer.println();
			writer.println("public class Main {");
			writer.println();
			writer.println("    public static void main(String[] args) {");
			writer.println("    }");
			writer.println();
			writer.println("}");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void writeResource() {
		try {
			Path path = this.gradleBuild.getProjectDir().toPath()
					.resolve(Paths.get("src", "main", "resources", "static", "file.txt"));
			Files.createDirectories(path.getParent());
			Files.createFile(path);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Map<String, List<String>> readLayerIndex(JarFile jarFile) throws IOException {
		Map<String, List<String>> index = new LinkedHashMap<>();
		ZipEntry indexEntry = jarFile.getEntry(this.indexPath + "layers.idx");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(indexEntry)))) {
			String line = reader.readLine();
			String layer = null;
			while (line != null) {
				if (line.startsWith("- ")) {
					layer = line.substring(3, line.length() - 2);
				}
				else if (line.startsWith("  - ")) {
					index.computeIfAbsent(layer, (key) -> new ArrayList<>()).add(line.substring(5, line.length() - 1));
				}
				line = reader.readLine();
			}
			return index;
		}
	}

	private Map<String, List<String>> readExtractedLayers(File root, List<String> layerNames) throws IOException {
		Map<String, List<String>> extractedLayers = new LinkedHashMap<>();
		for (String layerName : layerNames) {
			File layer = new File(root, layerName);
			assertThat(layer).isDirectory();
			extractedLayers.put(layerName,
					Files.walk(layer.toPath()).filter((path) -> path.toFile().isFile()).map(layer.toPath()::relativize)
							.map(Path::toString).map(StringUtils::cleanPath).collect(Collectors.toList()));
		}
		return extractedLayers;
	}

	private void assertExtractedLayers(List<String> layerNames, Map<String, List<String>> indexedLayers)
			throws IOException {
		Map<String, List<String>> extractedLayers = readExtractedLayers(this.gradleBuild.getProjectDir(), layerNames);
		assertThat(extractedLayers.keySet()).isEqualTo(indexedLayers.keySet());
		extractedLayers.forEach((name, contents) -> {
			List<String> index = indexedLayers.get(name);
			List<String> unexpected = new ArrayList<>();
			for (String file : contents) {
				if (!isInIndex(index, file)) {
					unexpected.add(name);
				}
			}
			assertThat(unexpected).isEmpty();
		});
	}

	private boolean isInIndex(List<String> index, String file) {
		for (String candidate : index) {
			if (file.equals(candidate) || candidate.endsWith("/") && file.startsWith(candidate)) {
				return true;
			}
		}
		return false;
	}

}
