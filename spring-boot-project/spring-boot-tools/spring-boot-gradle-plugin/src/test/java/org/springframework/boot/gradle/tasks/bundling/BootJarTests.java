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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
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
	void whenJarIsLayeredThenBootInfContainsOnlyLayersAndIndexFiles() throws IOException {
		List<String> entryNames = getEntryNames(createLayeredJar());
		assertThat(entryNames.stream().filter((name) -> name.startsWith("BOOT-INF/"))
				.filter((name) -> !name.startsWith("BOOT-INF/layers/"))).contains("BOOT-INF/layers.idx",
						"BOOT-INF/classpath.idx");
	}

	@Test
	void whenJarIsLayeredThenManifestContainsEntryForLayersIndexInPlaceOfClassesAndLib() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classes")).isEqualTo(null);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Lib")).isEqualTo(null);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Layers-Index"))
					.isEqualTo("BOOT-INF/layers.idx");
		}
	}

	@Test
	void whenJarIsLayeredThenLayersIndexIsPresentAndListsLayersInOrder() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(entryLines(jarFile, "BOOT-INF/layers.idx")).containsExactly("dependencies", "spring-boot-loader",
					"snapshot-dependencies", "application");
		}
	}

	@Test
	void whenJarIsLayeredThenContentsAreMovedToLayerDirectories() throws IOException {
		List<String> entryNames = getEntryNames(createLayeredJar());
		assertThat(entryNames)
				.containsSubsequence("BOOT-INF/layers/dependencies/lib/first-library.jar",
						"BOOT-INF/layers/dependencies/lib/second-library.jar")
				.contains("BOOT-INF/layers/snapshot-dependencies/lib/third-library-SNAPSHOT.jar")
				.containsSubsequence("BOOT-INF/layers/application/classes/com/example/Application.class",
						"BOOT-INF/layers/application/classes/application.properties")
				.contains("BOOT-INF/layers/application/classes/static/test.css");
	}

	@Test
	void whenJarIsLayeredWithCustomStrategiesThenContentsAreMovedToLayerDirectories() throws IOException {
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
			layered.layerOrder("my-deps", "my-internal-deps", "my-snapshot-deps", "resources", "application");
		});
		List<String> entryNames = getEntryNames(jar);
		assertThat(entryNames)
				.containsSubsequence("BOOT-INF/layers/my-internal-deps/lib/first-library.jar",
						"BOOT-INF/layers/my-internal-deps/lib/second-library.jar")
				.contains("BOOT-INF/layers/my-snapshot-deps/lib/third-library-SNAPSHOT.jar")
				.containsSubsequence("BOOT-INF/layers/application/classes/com/example/Application.class",
						"BOOT-INF/layers/application/classes/application.properties")
				.contains("BOOT-INF/layers/resources/classes/static/test.css");
	}

	@Test
	void whenJarIsLayeredJarsInLibAreStored() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(jarFile.getEntry("BOOT-INF/layers/dependencies/lib/first-library.jar").getMethod())
					.isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/layers/dependencies/lib/second-library.jar").getMethod())
					.isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/layers/snapshot-dependencies/lib/third-library-SNAPSHOT.jar")
					.getMethod()).isEqualTo(ZipEntry.STORED);
		}
	}

	@Test
	void whenJarIsLayeredClasspathIndexPointsToLayeredLibs() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(entryLines(jarFile, "BOOT-INF/classpath.idx")).containsExactly("first-library.jar",
					"second-library.jar", "third-library-SNAPSHOT.jar");
		}
	}

	@Test
	void whenJarIsLayeredThenLayerToolsAreAddedToTheJar() throws IOException {
		List<String> entryNames = getEntryNames(createLayeredJar());
		assertThat(entryNames).contains(jarModeLayerTools());
	}

	private String jarModeLayerTools() {
		JarModeLibrary library = JarModeLibrary.LAYER_TOOLS;
		String version = library.getCoordinates().getVersion();
		String layer = (version == null || !version.contains("SNAPSHOT")) ? "dependencies" : "snapshot-dependencies";
		return "BOOT-INF/layers/" + layer + "/lib/" + library.getName();
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
			assertThat(entryLines(jarFile, "BOOT-INF/classpath.idx")).containsExactly("first-library.jar",
					"second-library.jar", "third-library-SNAPSHOT.jar");
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
				jarFile("third-library-SNAPSHOT.jar"));
		Set<ResolvedArtifactResult> artifacts = new LinkedHashSet<>();
		artifacts.add(mockLibraryArtifact("first-library.jar", "com.example", "first-library", "1.0.0"));
		artifacts.add(mockLibraryArtifact("second-library.jar", "com.example", "second-library", "1.0.0"));
		artifacts.add(
				mockLibraryArtifact("third-library-SNAPSHOT.jar", "com.example", "third-library", "1.0.0.SNAPSHOT"));
		ArtifactCollection resolvedDependencies = mock(ArtifactCollection.class);
		given(resolvedDependencies.getArtifacts()).willReturn(artifacts);
		ResolvableDependencies resolvableDependencies = mock(ResolvableDependencies.class);
		given(resolvableDependencies.getArtifacts()).willReturn(resolvedDependencies);
		Configuration configuration = mock(Configuration.class);
		given(configuration.isCanBeResolved()).willReturn(true);
		given(configuration.getIncoming()).willReturn(resolvableDependencies);
		bootJar.setConfiguration(Collections.singleton(configuration));
	}

	private ResolvedArtifactResult mockLibraryArtifact(String fileName, String group, String module, String version) {
		ModuleComponentIdentifier identifier = mock(ModuleComponentIdentifier.class);
		given(identifier.getGroup()).willReturn(group);
		given(identifier.getModule()).willReturn(module);
		given(identifier.getVersion()).willReturn(version);
		ComponentArtifactIdentifier libraryArtifactId = mock(ComponentArtifactIdentifier.class);
		given(libraryArtifactId.getComponentIdentifier()).willReturn(identifier);
		ResolvedArtifactResult libraryArtifact = mock(ResolvedArtifactResult.class);
		File file = new File(this.temp, fileName).getAbsoluteFile();
		System.out.println(file);
		given(libraryArtifact.getFile()).willReturn(file);
		given(libraryArtifact.getId()).willReturn(libraryArtifactId);
		return libraryArtifact;
	}

	private List<String> entryLines(JarFile jarFile, String entryName) throws IOException {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(jarFile.getInputStream(jarFile.getEntry(entryName))))) {
			return reader.lines().collect(Collectors.toList());
		}
	}

	@Override
	protected void executeTask() {
		getTask().copy();
	}

	public static class TestBootJar extends BootJar {

		private Iterable<Configuration> configurations = Collections.emptySet();

		@Override
		protected Iterable<Configuration> getConfigurations() {
			return this.configurations;
		}

		void setConfiguration(Iterable<Configuration> configurations) {
			this.configurations = configurations;
		}

	}

}
