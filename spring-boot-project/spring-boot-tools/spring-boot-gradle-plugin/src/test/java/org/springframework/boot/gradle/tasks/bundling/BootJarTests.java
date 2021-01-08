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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.junit.jupiter.api.Test;

import org.springframework.boot.gradle.tasks.bundling.BootJarTests.TestBootJar;
import org.springframework.boot.loader.tools.JarModeLibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Paddy Drury
 */
class BootJarTests extends AbstractBootArchiveTests<TestBootJar> {

	BootJarTests() {
		super(TestBootJar.class, "org.springframework.boot.loader.JarLauncher", "BOOT-INF/lib/", "BOOT-INF/classes/");
	}

	@Test
	void contentCanBeAddedToBootInfUsingCopySpecFromGetter() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Application");
		bootJar.getBootInf().into("test").from(new File("build.gradle").getAbsolutePath());
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Test
	void contentCanBeAddedToBootInfUsingCopySpecAction() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Application");
		bootJar.bootInf((copySpec) -> copySpec.into("test").from(new File("build.gradle").getAbsolutePath()));
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Test
	void whenJarIsLayeredThenManifestContainsEntryForLayersIndexInPlaceOfClassesAndLib() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classes"))
					.isEqualTo("BOOT-INF/classes/");
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Lib"))
					.isEqualTo("BOOT-INF/lib/");
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classpath-Index"))
					.isEqualTo("BOOT-INF/classpath.idx");
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Layers-Index"))
					.isEqualTo("BOOT-INF/layers.idx");
		}
	}

	@Test
	void whenJarIsLayeredThenLayersIndexIsPresentAndCorrect() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			List<String> entryNames = getEntryNames(jarFile);
			assertThat(entryNames).contains("BOOT-INF/lib/first-library.jar", "BOOT-INF/lib/second-library.jar",
					"BOOT-INF/lib/third-library-SNAPSHOT.jar", "BOOT-INF/lib/first-project-library.jar",
					"BOOT-INF/lib/second-project-library-SNAPSHOT.jar",
					"BOOT-INF/classes/com/example/Application.class", "BOOT-INF/classes/application.properties",
					"BOOT-INF/classes/static/test.css");
			List<String> index = entryLines(jarFile, "BOOT-INF/layers.idx");
			assertThat(getLayerNames(index)).containsExactly("dependencies", "spring-boot-loader",
					"snapshot-dependencies", "application");
			String layerToolsJar = "BOOT-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName();
			List<String> expected = new ArrayList<>();
			expected.add("- \"dependencies\":");
			expected.add("  - \"BOOT-INF/lib/first-library.jar\"");
			expected.add("  - \"BOOT-INF/lib/first-project-library.jar\"");
			expected.add("  - \"BOOT-INF/lib/second-library.jar\"");
			if (!layerToolsJar.contains("SNAPSHOT")) {
				expected.add("  - \"" + layerToolsJar + "\"");
			}
			expected.add("- \"spring-boot-loader\":");
			expected.add("  - \"org/\"");
			expected.add("- \"snapshot-dependencies\":");
			expected.add("  - \"BOOT-INF/lib/second-project-library-SNAPSHOT.jar\"");
			if (layerToolsJar.contains("SNAPSHOT")) {
				expected.add("  - \"" + layerToolsJar + "\"");
			}
			expected.add("  - \"BOOT-INF/lib/third-library-SNAPSHOT.jar\"");
			expected.add("- \"application\":");
			expected.add("  - \"BOOT-INF/classes/\"");
			expected.add("  - \"BOOT-INF/classpath.idx\"");
			expected.add("  - \"BOOT-INF/layers.idx\"");
			expected.add("  - \"META-INF/\"");
			assertThat(index).containsExactlyElementsOf(expected);
		}
	}

	@Test
	void whenJarIsLayeredWithCustomStrategiesThenLayersIndexIsPresentAndCorrect() throws IOException {
		File jar = createLayeredJar((layered) -> {
			layered.application((application) -> {
				application.intoLayer("resources", (spec) -> spec.include("static/**"));
				application.intoLayer("application");
			});
			layered.dependencies((dependencies) -> {
				dependencies.intoLayer("my-snapshot-deps", (spec) -> spec.include("com.example:*:*.SNAPSHOT"));
				dependencies.intoLayer("my-internal-deps", (spec) -> spec.include("com.example:*:*"));
				dependencies.intoLayer("my-deps");
			});
			layered.setLayerOrder("my-deps", "my-internal-deps", "my-snapshot-deps", "resources", "application");
		});
		try (JarFile jarFile = new JarFile(jar)) {
			List<String> entryNames = getEntryNames(jar);
			assertThat(entryNames).contains("BOOT-INF/lib/first-library.jar", "BOOT-INF/lib/second-library.jar",
					"BOOT-INF/lib/third-library-SNAPSHOT.jar", "BOOT-INF/lib/first-project-library.jar",
					"BOOT-INF/lib/second-project-library-SNAPSHOT.jar",
					"BOOT-INF/classes/com/example/Application.class", "BOOT-INF/classes/application.properties",
					"BOOT-INF/classes/static/test.css");
			List<String> index = entryLines(jarFile, "BOOT-INF/layers.idx");
			assertThat(getLayerNames(index)).containsExactly("my-deps", "my-internal-deps", "my-snapshot-deps",
					"resources", "application");
			String layerToolsJar = "BOOT-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName();
			List<String> expected = new ArrayList<>();
			expected.add("- \"my-deps\":");
			expected.add("  - \"" + layerToolsJar + "\"");
			expected.add("- \"my-internal-deps\":");
			expected.add("  - \"BOOT-INF/lib/first-library.jar\"");
			expected.add("  - \"BOOT-INF/lib/first-project-library.jar\"");
			expected.add("  - \"BOOT-INF/lib/second-library.jar\"");
			expected.add("- \"my-snapshot-deps\":");
			expected.add("  - \"BOOT-INF/lib/second-project-library-SNAPSHOT.jar\"");
			expected.add("  - \"BOOT-INF/lib/third-library-SNAPSHOT.jar\"");
			expected.add("- \"resources\":");
			expected.add("  - \"BOOT-INF/classes/static/\"");
			expected.add("- \"application\":");
			expected.add("  - \"BOOT-INF/classes/application.properties\"");
			expected.add("  - \"BOOT-INF/classes/com/\"");
			expected.add("  - \"BOOT-INF/classpath.idx\"");
			expected.add("  - \"BOOT-INF/layers.idx\"");
			expected.add("  - \"META-INF/\"");
			expected.add("  - \"org/\"");
			assertThat(index).containsExactlyElementsOf(expected);
		}
	}

	@Test
	void jarsInLibAreStored() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(jarFile.getEntry("BOOT-INF/lib/first-library.jar").getMethod()).isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/lib/second-library.jar").getMethod()).isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/lib/third-library-SNAPSHOT.jar").getMethod())
					.isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/lib/first-project-library.jar").getMethod())
					.isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/lib/second-project-library-SNAPSHOT.jar").getMethod())
					.isEqualTo(ZipEntry.STORED);
		}
	}

	@Test
	void whenJarIsLayeredClasspathIndexPointsToLayeredLibs() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(entryLines(jarFile, "BOOT-INF/classpath.idx")).containsExactly(
					"- \"BOOT-INF/lib/first-library.jar\"", "- \"BOOT-INF/lib/second-library.jar\"",
					"- \"BOOT-INF/lib/third-library-SNAPSHOT.jar\"", "- \"BOOT-INF/lib/first-project-library.jar\"",
					"- \"BOOT-INF/lib/second-project-library-SNAPSHOT.jar\"");
		}
	}

	@Test
	void whenJarIsLayeredThenLayerToolsAreAddedToTheJar() throws IOException {
		List<String> entryNames = getEntryNames(createLayeredJar());
		assertThat(entryNames).contains("BOOT-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName());
	}

	@Test
	void whenJarIsLayeredAndIncludeLayerToolsIsFalseThenLayerToolsAreNotAddedToTheJar() throws IOException {
		List<String> entryNames = getEntryNames(
				createLayeredJar((configuration) -> configuration.setIncludeLayerTools(false)));
		assertThat(entryNames).doesNotContain("BOOT-INF/layers/dependencies/lib/spring-boot-jarmode-layertools.jar");
	}

	@Test
	void classpathIndexPointsToBootInfLibs() throws IOException {
		try (JarFile jarFile = new JarFile(createPopulatedJar())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classpath-Index"))
					.isEqualTo("BOOT-INF/classpath.idx");
			assertThat(entryLines(jarFile, "BOOT-INF/classpath.idx")).containsExactly(
					"- \"BOOT-INF/lib/first-library.jar\"", "- \"BOOT-INF/lib/second-library.jar\"",
					"- \"BOOT-INF/lib/third-library-SNAPSHOT.jar\"", "- \"BOOT-INF/lib/first-project-library.jar\"",
					"- \"BOOT-INF/lib/second-project-library-SNAPSHOT.jar\"");
		}
	}

	private File createPopulatedJar() throws IOException {
		addContent();
		executeTask();
		return getTask().getArchiveFile().get().getAsFile();
	}

	private File createLayeredJar() throws IOException {
		return createLayeredJar(null);
	}

	private File createLayeredJar(Action<LayeredSpec> action) throws IOException {
		if (action != null) {
			getTask().layered(action);
		}
		else {
			getTask().layered();
		}
		addContent();
		executeTask();
		return getTask().getArchiveFile().get().getAsFile();
	}

	private void addContent() throws IOException {
		TestBootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Main");
		File classesJavaMain = new File(this.temp, "classes/java/main");
		File applicationClass = new File(classesJavaMain, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		File resourcesMain = new File(this.temp, "resources/main");
		File applicationProperties = new File(resourcesMain, "application.properties");
		applicationProperties.getParentFile().mkdirs();
		applicationProperties.createNewFile();
		File staticResources = new File(resourcesMain, "static");
		staticResources.mkdir();
		File css = new File(staticResources, "test.css");
		css.createNewFile();
		bootJar.classpath(classesJavaMain, resourcesMain, jarFile("first-library.jar"), jarFile("second-library.jar"),
				jarFile("third-library-SNAPSHOT.jar"), jarFile("first-project-library.jar"),
				jarFile("second-project-library-SNAPSHOT.jar"));
		Set<ResolvedArtifact> artifacts = new LinkedHashSet<>();
		artifacts.add(mockLibraryArtifact("first-library.jar", "com.example", "first-library", "1.0.0"));
		artifacts.add(mockLibraryArtifact("second-library.jar", "com.example", "second-library", "1.0.0"));
		artifacts.add(
				mockLibraryArtifact("third-library-SNAPSHOT.jar", "com.example", "third-library", "1.0.0.SNAPSHOT"));
		artifacts
				.add(mockProjectArtifact("first-project-library.jar", "com.example", "first-project-library", "1.0.0"));
		artifacts.add(mockProjectArtifact("second-project-library-SNAPSHOT.jar", "com.example",
				"second-project-library", "1.0.0.SNAPSHOT"));
		ResolvedConfiguration resolvedConfiguration = mock(ResolvedConfiguration.class);
		given(resolvedConfiguration.getResolvedArtifacts()).willReturn(artifacts);
		Configuration configuration = mock(Configuration.class);
		given(configuration.isCanBeResolved()).willReturn(true);
		given(configuration.getResolvedConfiguration()).willReturn(resolvedConfiguration);
		bootJar.setConfiguration(Collections.singleton(configuration));
	}

	private ResolvedArtifact mockLibraryArtifact(String fileName, String group, String module, String version) {
		ModuleComponentIdentifier moduleComponentIdentifier = mock(ModuleComponentIdentifier.class);
		ComponentArtifactIdentifier libraryArtifactId = mock(ComponentArtifactIdentifier.class);
		given(libraryArtifactId.getComponentIdentifier()).willReturn(moduleComponentIdentifier);
		ResolvedArtifact libraryArtifact = mockArtifact(fileName, group, module, version);
		given(libraryArtifact.getId()).willReturn(libraryArtifactId);
		return libraryArtifact;
	}

	private ResolvedArtifact mockProjectArtifact(String fileName, String group, String module, String version) {
		ProjectComponentIdentifier projectComponentIdentifier = mock(ProjectComponentIdentifier.class);
		ComponentArtifactIdentifier projectArtifactId = mock(ComponentArtifactIdentifier.class);
		given(projectArtifactId.getComponentIdentifier()).willReturn(projectComponentIdentifier);
		ResolvedArtifact projectArtifact = mockArtifact(fileName, group, module, version);
		given(projectArtifact.getId()).willReturn(projectArtifactId);
		return projectArtifact;
	}

	private ResolvedArtifact mockArtifact(String fileName, String group, String module, String version) {
		ModuleVersionIdentifier moduleVersionIdentifier = mock(ModuleVersionIdentifier.class);
		given(moduleVersionIdentifier.getGroup()).willReturn(group);
		given(moduleVersionIdentifier.getName()).willReturn(module);
		given(moduleVersionIdentifier.getVersion()).willReturn(version);
		ResolvedModuleVersion moduleVersion = mock(ResolvedModuleVersion.class);
		given(moduleVersion.getId()).willReturn(moduleVersionIdentifier);
		ResolvedArtifact libraryArtifact = mock(ResolvedArtifact.class);
		File file = new File(this.temp, fileName).getAbsoluteFile();
		System.out.println(file);
		given(libraryArtifact.getFile()).willReturn(file);
		given(libraryArtifact.getModuleVersion()).willReturn(moduleVersion);
		return libraryArtifact;
	}

	private List<String> entryLines(JarFile jarFile, String entryName) throws IOException {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(jarFile.getInputStream(jarFile.getEntry(entryName))))) {
			return reader.lines().collect(Collectors.toList());
		}
	}

	private Set<String> getLayerNames(List<String> index) {
		Set<String> layerNames = new LinkedHashSet<>();
		for (String line : index) {
			if (line.startsWith("- ")) {
				layerNames.add(line.substring(3, line.length() - 2));
			}
		}
		return layerNames;
	}

	@Override
	protected void executeTask() {
		getTask().copy();
	}

	public static class TestBootJar extends BootJar {

		private Iterable<Configuration> configurations = Collections.emptySet();

		@Override
		@SuppressWarnings("deprecation")
		protected Iterable<Configuration> getConfigurations() {
			return this.configurations;
		}

		void setConfiguration(Iterable<Configuration> configurations) {
			this.configurations = configurations;
		}

	}

}
