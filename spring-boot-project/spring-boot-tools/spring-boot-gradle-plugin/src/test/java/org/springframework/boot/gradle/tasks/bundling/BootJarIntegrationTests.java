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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.InvalidRunnerConfigurationException;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.loader.tools.JarModeLibrary;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Paddy Drury
 */
class BootJarIntegrationTests extends AbstractBootArchiveIntegrationTests {

	BootJarIntegrationTests() {
		super("bootJar", "BOOT-INF/lib/", "BOOT-INF/classes/");
	}

	@TestTemplate
	void upToDateWhenBuiltTwiceWithLayers() throws InvalidRunnerConfigurationException, UnexpectedBuildFailure {
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@TestTemplate
	void notUpToDateWhenBuiltWithoutLayersAndThenWithLayers()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure {
		assertThat(this.gradleBuild.build("bootJar").task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void notUpToDateWhenBuiltWithLayersAndToolsAndThenWithLayersAndWithoutTools()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure {
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-Playered=true", "-PexcludeTools=true", "bootJar").task(":bootJar")
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void implicitLayers() throws IOException {
		writeMainClass();
		writeResource();
		assertThat(this.gradleBuild.build("bootJar").task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Map<String, List<String>> indexedLayers;
		String layerToolsJar = "BOOT-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName();
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getEntry(layerToolsJar)).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/commons-lang3-3.9.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/spring-core-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/spring-jcl-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/library-1.0-SNAPSHOT.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/example/Main.class")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/static/file.txt")).isNotNull();
			indexedLayers = readLayerIndex(jarFile);
		}
		List<String> layerNames = Arrays.asList("dependencies", "spring-boot-loader", "snapshot-dependencies",
				"application");
		assertThat(indexedLayers.keySet()).containsExactlyElementsOf(layerNames);
		Set<String> expectedDependencies = new TreeSet<>();
		expectedDependencies.add("BOOT-INF/lib/commons-lang3-3.9.jar");
		expectedDependencies.add("BOOT-INF/lib/spring-core-5.2.5.RELEASE.jar");
		expectedDependencies.add("BOOT-INF/lib/spring-jcl-5.2.5.RELEASE.jar");
		Set<String> expectedSnapshotDependencies = new TreeSet<>();
		expectedSnapshotDependencies.add("BOOT-INF/lib/library-1.0-SNAPSHOT.jar");
		(layerToolsJar.contains("SNAPSHOT") ? expectedSnapshotDependencies : expectedDependencies).add(layerToolsJar);
		assertThat(indexedLayers.get("dependencies")).containsExactlyElementsOf(expectedDependencies);
		assertThat(indexedLayers.get("spring-boot-loader")).containsExactly("org/");
		assertThat(indexedLayers.get("snapshot-dependencies")).containsExactlyElementsOf(expectedSnapshotDependencies);
		assertThat(indexedLayers.get("application")).containsExactly("BOOT-INF/classes/", "BOOT-INF/classpath.idx",
				"BOOT-INF/layers.idx", "META-INF/");
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
		BuildResult build = this.gradleBuild.build("bootJar");
		assertThat(build.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Map<String, List<String>> indexedLayers;
		String layerToolsJar = "BOOT-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName();
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getEntry(layerToolsJar)).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/commons-lang3-3.9.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/spring-core-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/spring-jcl-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/library-1.0-SNAPSHOT.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/example/Main.class")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/static/file.txt")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers.idx")).isNotNull();
			indexedLayers = readLayerIndex(jarFile);
		}
		List<String> layerNames = Arrays.asList("dependencies", "commons-dependencies", "snapshot-dependencies",
				"static", "app");
		assertThat(indexedLayers.keySet()).containsExactlyElementsOf(layerNames);
		Set<String> expectedDependencies = new TreeSet<>();
		expectedDependencies.add("BOOT-INF/lib/spring-core-5.2.5.RELEASE.jar");
		expectedDependencies.add("BOOT-INF/lib/spring-jcl-5.2.5.RELEASE.jar");
		List<String> expectedSnapshotDependencies = new ArrayList<>();
		expectedSnapshotDependencies.add("BOOT-INF/lib/library-1.0-SNAPSHOT.jar");
		(layerToolsJar.contains("SNAPSHOT") ? expectedSnapshotDependencies : expectedDependencies).add(layerToolsJar);
		assertThat(indexedLayers.get("dependencies")).containsExactlyElementsOf(expectedDependencies);
		assertThat(indexedLayers.get("commons-dependencies")).containsExactly("BOOT-INF/lib/commons-lang3-3.9.jar");
		assertThat(indexedLayers.get("snapshot-dependencies")).containsExactlyElementsOf(expectedSnapshotDependencies);
		assertThat(indexedLayers.get("static")).containsExactly("BOOT-INF/classes/static/");
		List<String> appLayer = new ArrayList<>(indexedLayers.get("app"));
		Set<String> nonLoaderEntries = new TreeSet<>();
		nonLoaderEntries.add("BOOT-INF/classes/example/");
		nonLoaderEntries.add("BOOT-INF/classpath.idx");
		nonLoaderEntries.add("BOOT-INF/layers.idx");
		nonLoaderEntries.add("META-INF/");
		assertThat(appLayer).containsSubsequence(nonLoaderEntries);
		appLayer.removeAll(nonLoaderEntries);
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
		BuildResult build = this.gradleBuild.build("bootJar");
		assertThat(build.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		Map<String, List<String>> indexedLayers;
		String layerToolsJar = "BOOT-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName();
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getEntry(layerToolsJar)).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/alpha-1.2.3.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/bravo-1.2.3.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/commons-lang3-3.9.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/spring-core-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/spring-jcl-5.2.5.RELEASE.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/lib/library-1.0-SNAPSHOT.jar")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/example/Main.class")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/static/file.txt")).isNotNull();
			assertThat(jarFile.getEntry("BOOT-INF/layers.idx")).isNotNull();
			indexedLayers = readLayerIndex(jarFile);
		}
		List<String> layerNames = Arrays.asList("dependencies", "commons-dependencies", "snapshot-dependencies",
				"subproject-dependencies", "static", "app");
		assertThat(indexedLayers.keySet()).containsExactlyElementsOf(layerNames);
		Set<String> expectedSubprojectDependencies = new TreeSet<>();
		expectedSubprojectDependencies.add("BOOT-INF/lib/alpha-1.2.3.jar");
		expectedSubprojectDependencies.add("BOOT-INF/lib/bravo-1.2.3.jar");
		Set<String> expectedDependencies = new TreeSet<>();
		expectedDependencies.add("BOOT-INF/lib/spring-core-5.2.5.RELEASE.jar");
		expectedDependencies.add("BOOT-INF/lib/spring-jcl-5.2.5.RELEASE.jar");
		List<String> expectedSnapshotDependencies = new ArrayList<>();
		expectedSnapshotDependencies.add("BOOT-INF/lib/library-1.0-SNAPSHOT.jar");
		(layerToolsJar.contains("SNAPSHOT") ? expectedSnapshotDependencies : expectedDependencies).add(layerToolsJar);
		assertThat(indexedLayers.get("subproject-dependencies"))
				.containsExactlyElementsOf(expectedSubprojectDependencies);
		assertThat(indexedLayers.get("dependencies")).containsExactlyElementsOf(expectedDependencies);
		assertThat(indexedLayers.get("commons-dependencies")).containsExactly("BOOT-INF/lib/commons-lang3-3.9.jar");
		assertThat(indexedLayers.get("snapshot-dependencies")).containsExactlyElementsOf(expectedSnapshotDependencies);
		assertThat(indexedLayers.get("static")).containsExactly("BOOT-INF/classes/static/");
		List<String> appLayer = new ArrayList<>(indexedLayers.get("app"));
		Set<String> nonLoaderEntries = new TreeSet<>();
		nonLoaderEntries.add("BOOT-INF/classes/example/");
		nonLoaderEntries.add("BOOT-INF/classpath.idx");
		nonLoaderEntries.add("BOOT-INF/layers.idx");
		nonLoaderEntries.add("META-INF/");
		assertThat(appLayer).containsSubsequence(nonLoaderEntries);
		appLayer.removeAll(nonLoaderEntries);
		assertThat(appLayer).containsExactly("org/");
		BuildResult listLayers = this.gradleBuild.build("listLayers");
		assertThat(listLayers.task(":listLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		String listLayersOutput = listLayers.getOutput();
		assertThat(new BufferedReader(new StringReader(listLayersOutput)).lines()).containsSequence(layerNames);
		BuildResult extractLayers = this.gradleBuild.build("extractLayers");
		assertThat(extractLayers.task(":extractLayers").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertExtractedLayers(layerNames, indexedLayers);
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

	private void writeSettingsGradle() {
		try (PrintWriter writer = new PrintWriter(
				new FileWriter(new File(this.gradleBuild.getProjectDir(), "settings.gradle")))) {
			writer.println("include 'alpha', 'bravo'");
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
		ZipEntry indexEntry = jarFile.getEntry("BOOT-INF/layers.idx");
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

}
