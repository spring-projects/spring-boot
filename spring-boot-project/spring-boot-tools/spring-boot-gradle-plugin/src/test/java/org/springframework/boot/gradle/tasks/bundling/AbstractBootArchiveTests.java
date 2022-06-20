/*
 * Copyright 2012-2022 the original author or authors.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.gradle.api.Action;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.LenientConfiguration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.internal.file.archive.ZipCopyAction;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Jar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.gradle.junit.GradleProjectBuilder;
import org.springframework.boot.loader.tools.DefaultLaunchScript;
import org.springframework.boot.loader.tools.JarModeLibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * Abstract base class for testing {@link BootArchive} implementations.
 *
 * @param <T> the type of the concrete BootArchive implementation
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
abstract class AbstractBootArchiveTests<T extends Jar & BootArchive> {

	@TempDir
	File temp;

	private final Class<T> taskClass;

	private final String launcherClass;

	private final String libPath;

	private final String classesPath;

	private final String indexPath;

	private Project project;

	private T task;

	protected AbstractBootArchiveTests(Class<T> taskClass, String launcherClass, String libPath, String classesPath,
			String indexPath) {
		this.taskClass = taskClass;
		this.launcherClass = launcherClass;
		this.libPath = libPath;
		this.classesPath = classesPath;
		this.indexPath = indexPath;
	}

	@BeforeEach
	void createTask() {
		File projectDir = new File(this.temp, "project");
		projectDir.mkdirs();
		this.project = GradleProjectBuilder.builder().withProjectDir(projectDir).build();
		this.project.setDescription("Test project for " + this.taskClass.getSimpleName());
		this.task = configure(this.project.getTasks().create("testArchive", this.taskClass));
	}

	@Test
	void basicArchiveCreation() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Main-Class")).isEqualTo(this.launcherClass);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class")).isEqualTo("com.example.Main");
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classes"))
					.isEqualTo(this.classesPath);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Lib")).isEqualTo(this.libPath);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Version")).isNotNull();
		}
	}

	@Test
	void classpathJarsArePackagedBeneathLibPathAndAreStored() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.classpath(jarFile("one.jar"), jarFile("two.jar"));
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "one.jar")).isNotNull().extracting(ZipEntry::getMethod)
					.isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry(this.libPath + "two.jar")).isNotNull().extracting(ZipEntry::getMethod)
					.isEqualTo(ZipEntry.STORED);
		}
	}

	@Test
	void classpathDirectoriesArePackagedBeneathClassesPath() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		File classpathDirectory = new File(this.temp, "classes");
		File applicationClass = new File(classpathDirectory, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		this.task.classpath(classpathDirectory);
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.classesPath + "com/example/Application.class")).isNotNull();
		}
	}

	@Test
	void moduleInfoClassIsPackagedInTheRootOfTheArchive() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		File classpathDirectory = new File(this.temp, "classes");
		File moduleInfoClass = new File(classpathDirectory, "module-info.class");
		moduleInfoClass.getParentFile().mkdirs();
		moduleInfoClass.createNewFile();
		File applicationClass = new File(classpathDirectory, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		this.task.classpath(classpathDirectory);
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.classesPath + "com/example/Application.class")).isNotNull();
			assertThat(jarFile.getEntry("com/example/Application.class")).isNull();
			assertThat(jarFile.getEntry("module-info.class")).isNotNull();
			assertThat(jarFile.getEntry(this.classesPath + "/module-info.class")).isNull();
		}
	}

	@Test
	void classpathCanBeSetUsingAFileCollection() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.classpath(jarFile("one.jar"));
		this.task.setClasspath(this.task.getProject().files(jarFile("two.jar")));
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "one.jar")).isNull();
			assertThat(jarFile.getEntry(this.libPath + "two.jar")).isNotNull();
		}
	}

	@Test
	void classpathCanBeSetUsingAnObject() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.classpath(jarFile("one.jar"));
		this.task.setClasspath(jarFile("two.jar"));
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "one.jar")).isNull();
			assertThat(jarFile.getEntry(this.libPath + "two.jar")).isNotNull();
		}
	}

	@Test
	void filesOnTheClasspathThatAreNotZipFilesAreSkipped() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.classpath(new File("test.pom"));
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "/test.pom")).isNull();
		}
	}

	@Test
	void loaderIsWrittenToTheRootOfTheJarAfterManifest() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("org/springframework/boot/loader/LaunchedURLClassLoader.class")).isNotNull();
			assertThat(jarFile.getEntry("org/springframework/boot/loader/")).isNotNull();
		}
		// gh-16698
		try (ZipInputStream zipInputStream = new ZipInputStream(
				new FileInputStream(this.task.getArchiveFile().get().getAsFile()))) {
			assertThat(zipInputStream.getNextEntry().getName()).isEqualTo("META-INF/");
			assertThat(zipInputStream.getNextEntry().getName()).isEqualTo("META-INF/MANIFEST.MF");
		}
	}

	@Test
	void loaderIsWrittenToTheRootOfTheJarWhenUsingThePropertiesLauncher() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		executeTask();
		this.task.getManifest().getAttributes().put("Main-Class", "org.springframework.boot.loader.PropertiesLauncher");
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("org/springframework/boot/loader/LaunchedURLClassLoader.class")).isNotNull();
			assertThat(jarFile.getEntry("org/springframework/boot/loader/")).isNotNull();
		}
	}

	@Test
	void unpackCommentIsAddedToEntryIdentifiedByAPattern() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.classpath(jarFile("one.jar"), jarFile("two.jar"));
		this.task.requiresUnpack("**/one.jar");
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "one.jar").getComment()).startsWith("UNPACK:");
			assertThat(jarFile.getEntry(this.libPath + "two.jar").getComment()).isNull();
		}
	}

	@Test
	void unpackCommentIsAddedToEntryIdentifiedByASpec() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.classpath(jarFile("one.jar"), jarFile("two.jar"));
		this.task.requiresUnpack((element) -> element.getName().endsWith("two.jar"));
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "two.jar").getComment()).startsWith("UNPACK:");
			assertThat(jarFile.getEntry(this.libPath + "one.jar").getComment()).isNull();
		}
	}

	@Test
	void launchScriptCanBePrepended() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.launchScript();
		executeTask();
		Map<String, String> properties = new HashMap<>();
		properties.put("initInfoProvides", this.task.getArchiveBaseName().get());
		properties.put("initInfoShortDescription", this.project.getDescription());
		properties.put("initInfoDescription", this.project.getDescription());
		File archiveFile = this.task.getArchiveFile().get().getAsFile();
		assertThat(Files.readAllBytes(archiveFile.toPath()))
				.startsWith(new DefaultLaunchScript(null, properties).toByteArray());
		try (ZipFile zipFile = new ZipFile(archiveFile)) {
			assertThat(zipFile.getEntries().hasMoreElements()).isTrue();
		}
		try {
			Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(archiveFile.toPath());
			assertThat(permissions).contains(PosixFilePermission.OWNER_EXECUTE);
		}
		catch (UnsupportedOperationException ex) {
			// Windows, presumably. Continue
		}
	}

	@Test
	void customLaunchScriptCanBePrepended() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		File customScript = new File(this.temp, "custom.script");
		Files.writeString(customScript.toPath(), "custom script", StandardOpenOption.CREATE);
		this.task.launchScript((configuration) -> configuration.setScript(customScript));
		executeTask();
		Path path = this.task.getArchiveFile().get().getAsFile().toPath();
		assertThat(Files.readString(path, StandardCharsets.ISO_8859_1)).startsWith("custom script");
	}

	@Test
	void launchScriptInitInfoPropertiesCanBeCustomized() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.launchScript((configuration) -> {
			configuration.getProperties().put("initInfoProvides", "provides");
			configuration.getProperties().put("initInfoShortDescription", "short description");
			configuration.getProperties().put("initInfoDescription", "description");
		});
		executeTask();
		Path path = this.task.getArchiveFile().get().getAsFile().toPath();
		String content = Files.readString(path, StandardCharsets.ISO_8859_1);
		assertThat(content).containsSequence("Provides:          provides");
		assertThat(content).containsSequence("Short-Description: short description");
		assertThat(content).containsSequence("Description:       description");
	}

	@Test
	void customMainClassInTheManifestIsHonored() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.getManifest().getAttributes().put("Main-Class", "com.example.CustomLauncher");
		executeTask();
		assertThat(this.task.getArchiveFile().get().getAsFile()).exists();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Main-Class"))
					.isEqualTo("com.example.CustomLauncher");
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class")).isEqualTo("com.example.Main");
			assertThat(jarFile.getEntry("org/springframework/boot/loader/LaunchedURLClassLoader.class")).isNull();
		}
	}

	@Test
	void customStartClassInTheManifestIsHonored() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.getManifest().getAttributes().put("Start-Class", "com.example.CustomMain");
		executeTask();
		assertThat(this.task.getArchiveFile().get().getAsFile()).exists();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Main-Class")).isEqualTo(this.launcherClass);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.CustomMain");
		}
	}

	@Test
	void fileTimestampPreservationCanBeDisabled() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.setPreserveFileTimestamps(false);
		executeTask();
		assertThat(this.task.getArchiveFile().get().getAsFile()).exists();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				assertThat(entry.getTime()).isEqualTo(BootZipCopyAction.CONSTANT_TIME_FOR_ZIP_ENTRIES);
			}
		}
	}

	@Test
	void constantTimestampMatchesGradleInternalTimestamp() {
		assertThat(BootZipCopyAction.CONSTANT_TIME_FOR_ZIP_ENTRIES)
				.isEqualTo(ZipCopyAction.CONSTANT_TIME_FOR_ZIP_ENTRIES);
	}

	@Test
	void reproducibleOrderingCanBeEnabled() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.from(newFile("bravo.txt"), newFile("alpha.txt"), newFile("charlie.txt"));
		this.task.setReproducibleFileOrder(true);
		executeTask();
		assertThat(this.task.getArchiveFile().get().getAsFile()).exists();
		List<String> textFiles = new ArrayList<>();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().endsWith(".txt")) {
					textFiles.add(entry.getName());
				}
			}
		}
		assertThat(textFiles).containsExactly("alpha.txt", "bravo.txt", "charlie.txt");
	}

	@Test
	void devtoolsJarIsExcludedByDefault() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.classpath(newFile("spring-boot-devtools-0.1.2.jar"));
		executeTask();
		assertThat(this.task.getArchiveFile().get().getAsFile()).exists();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "spring-boot-devtools-0.1.2.jar")).isNull();
		}
	}

	@Test
	void allEntriesUseUnixPlatformAndUtf8NameEncoding() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		this.task.setMetadataCharset("UTF-8");
		File classpathDirectory = new File(this.temp, "classes");
		File resource = new File(classpathDirectory, "some-resource.xml");
		resource.getParentFile().mkdirs();
		resource.createNewFile();
		this.task.classpath(classpathDirectory);
		executeTask();
		File archivePath = this.task.getArchiveFile().get().getAsFile();
		try (ZipFile zip = new ZipFile(archivePath)) {
			Enumeration<ZipArchiveEntry> entries = zip.getEntries();
			while (entries.hasMoreElements()) {
				ZipArchiveEntry entry = entries.nextElement();
				assertThat(entry.getPlatform()).isEqualTo(ZipArchiveEntry.PLATFORM_UNIX);
				assertThat(entry.getGeneralPurposeBit().usesUTF8ForNames()).isTrue();
			}
		}
	}

	@Test
	void loaderIsWrittenFirstThenApplicationClassesThenLibraries() throws IOException {
		this.task.getMainClass().set("com.example.Main");
		File classpathDirectory = new File(this.temp, "classes");
		File applicationClass = new File(classpathDirectory, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		this.task.classpath(classpathDirectory, jarFile("first-library.jar"), jarFile("second-library.jar"),
				jarFile("third-library.jar"));
		this.task.requiresUnpack("second-library.jar");
		executeTask();
		assertThat(getEntryNames(this.task.getArchiveFile().get().getAsFile())).containsSubsequence(
				"org/springframework/boot/loader/", this.classesPath + "com/example/Application.class",
				this.libPath + "first-library.jar", this.libPath + "second-library.jar",
				this.libPath + "third-library.jar");
	}

	@Test
	void archiveShouldBeLayeredByDefault() throws IOException {
		addContent();
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classes"))
					.isEqualTo(this.classesPath);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Lib")).isEqualTo(this.libPath);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Layers-Index"))
					.isEqualTo(this.indexPath + "layers.idx");
			assertThat(getEntryNames(jarFile)).contains(this.libPath + JarModeLibrary.LAYER_TOOLS.getName());
		}
	}

	@Test
	void jarWhenLayersDisabledShouldNotContainLayersIndex() throws IOException {
		List<String> entryNames = getEntryNames(createLayeredJar((configuration) -> configuration.setEnabled(false)));
		assertThat(entryNames).doesNotContain(this.indexPath + "layers.idx");
	}

	@Test
	void whenJarIsLayeredThenManifestContainsEntryForLayersIndexInPlaceOfClassesAndLib() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classes"))
					.isEqualTo(this.classesPath);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Lib")).isEqualTo(this.libPath);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Layers-Index"))
					.isEqualTo(this.indexPath + "layers.idx");
		}
	}

	@Test
	void whenJarIsLayeredThenLayersIndexIsPresentAndCorrect() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			List<String> entryNames = getEntryNames(jarFile);
			assertThat(entryNames).contains(this.libPath + "first-library.jar", this.libPath + "second-library.jar",
					this.libPath + "third-library-SNAPSHOT.jar", this.libPath + "first-project-library.jar",
					this.libPath + "second-project-library-SNAPSHOT.jar",
					this.classesPath + "com/example/Application.class", this.classesPath + "application.properties",
					this.classesPath + "static/test.css");
			List<String> index = entryLines(jarFile, this.indexPath + "layers.idx");
			assertThat(getLayerNames(index)).containsExactly("dependencies", "spring-boot-loader",
					"snapshot-dependencies", "application");
			String layerToolsJar = this.libPath + JarModeLibrary.LAYER_TOOLS.getName();
			List<String> expected = new ArrayList<>();
			expected.add("- \"dependencies\":");
			expected.add("  - \"" + this.libPath + "first-library.jar\"");
			expected.add("  - \"" + this.libPath + "first-project-library.jar\"");
			expected.add("  - \"" + this.libPath + "second-library.jar\"");
			if (!layerToolsJar.contains("SNAPSHOT")) {
				expected.add("  - \"" + layerToolsJar + "\"");
			}
			expected.add("- \"spring-boot-loader\":");
			expected.add("  - \"org/\"");
			expected.add("- \"snapshot-dependencies\":");
			expected.add("  - \"" + this.libPath + "second-project-library-SNAPSHOT.jar\"");
			if (layerToolsJar.contains("SNAPSHOT")) {
				expected.add("  - \"" + layerToolsJar + "\"");
			}
			expected.add("  - \"" + this.libPath + "third-library-SNAPSHOT.jar\"");
			expected.add("- \"application\":");
			Set<String> applicationContents = new TreeSet<>();
			applicationContents.add("  - \"" + this.classesPath + "\"");
			applicationContents.add("  - \"" + this.indexPath + "classpath.idx\"");
			applicationContents.add("  - \"" + this.indexPath + "layers.idx\"");
			applicationContents.add("  - \"META-INF/\"");
			expected.addAll(applicationContents);
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
			assertThat(entryNames).contains(this.libPath + "first-library.jar", this.libPath + "second-library.jar",
					this.libPath + "third-library-SNAPSHOT.jar", this.libPath + "first-project-library.jar",
					this.libPath + "second-project-library-SNAPSHOT.jar",
					this.classesPath + "com/example/Application.class", this.classesPath + "application.properties",
					this.classesPath + "static/test.css");
			List<String> index = entryLines(jarFile, this.indexPath + "layers.idx");
			assertThat(getLayerNames(index)).containsExactly("my-deps", "my-internal-deps", "my-snapshot-deps",
					"resources", "application");
			String layerToolsJar = this.libPath + JarModeLibrary.LAYER_TOOLS.getName();
			List<String> expected = new ArrayList<>();
			expected.add("- \"my-deps\":");
			expected.add("  - \"" + layerToolsJar + "\"");
			expected.add("- \"my-internal-deps\":");
			expected.add("  - \"" + this.libPath + "first-library.jar\"");
			expected.add("  - \"" + this.libPath + "first-project-library.jar\"");
			expected.add("  - \"" + this.libPath + "second-library.jar\"");
			expected.add("- \"my-snapshot-deps\":");
			expected.add("  - \"" + this.libPath + "second-project-library-SNAPSHOT.jar\"");
			expected.add("  - \"" + this.libPath + "third-library-SNAPSHOT.jar\"");
			expected.add("- \"resources\":");
			expected.add("  - \"" + this.classesPath + "static/\"");
			expected.add("- \"application\":");
			Set<String> applicationContents = new TreeSet<>();
			applicationContents.add("  - \"" + this.classesPath + "application.properties\"");
			applicationContents.add("  - \"" + this.classesPath + "com/\"");
			applicationContents.add("  - \"" + this.indexPath + "classpath.idx\"");
			applicationContents.add("  - \"" + this.indexPath + "layers.idx\"");
			applicationContents.add("  - \"META-INF/\"");
			applicationContents.add("  - \"org/\"");
			expected.addAll(applicationContents);
			assertThat(index).containsExactlyElementsOf(expected);
		}
	}

	@Test
	void whenArchiveIsLayeredThenLayerToolsAreAddedToTheJar() throws IOException {
		List<String> entryNames = getEntryNames(createLayeredJar());
		assertThat(entryNames).contains(this.libPath + JarModeLibrary.LAYER_TOOLS.getName());
	}

	@Test
	void whenArchiveIsLayeredAndIncludeLayerToolsIsFalseThenLayerToolsAreNotAddedToTheJar() throws IOException {
		List<String> entryNames = getEntryNames(
				createLayeredJar((configuration) -> configuration.setIncludeLayerTools(false)));
		assertThat(entryNames)
				.doesNotContain(this.indexPath + "layers/dependencies/lib/spring-boot-jarmode-layertools.jar");
	}

	protected File jarFile(String name) throws IOException {
		File file = newFile(name);
		try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(file))) {
			jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
			new Manifest().write(jar);
			jar.closeEntry();
		}
		return file;
	}

	private T configure(T task) {
		AbstractArchiveTask archiveTask = task;
		archiveTask.getArchiveBaseName().set("test");
		File destination = new File(this.temp, "destination");
		destination.mkdirs();
		archiveTask.getDestinationDirectory().set(destination);
		return task;
	}

	protected abstract void executeTask();

	protected T getTask() {
		return this.task;
	}

	protected List<String> getEntryNames(File file) throws IOException {
		try (JarFile jarFile = new JarFile(file)) {
			return getEntryNames(jarFile);
		}
	}

	protected List<String> getEntryNames(JarFile jarFile) {
		List<String> entryNames = new ArrayList<>();
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			entryNames.add(entries.nextElement().getName());
		}
		return entryNames;
	}

	protected File newFile(String name) throws IOException {
		File file = new File(this.temp, name);
		file.createNewFile();
		return file;
	}

	File createLayeredJar() throws IOException {
		return createLayeredJar((spec) -> {
		});
	}

	File createLayeredJar(Action<LayeredSpec> action) throws IOException {
		applyLayered(action);
		addContent();
		executeTask();
		return getTask().getArchiveFile().get().getAsFile();
	}

	File createPopulatedJar() throws IOException {
		addContent();
		executeTask();
		return getTask().getArchiveFile().get().getAsFile();
	}

	abstract void applyLayered(Action<LayeredSpec> action);

	@SuppressWarnings("unchecked")
	void addContent() throws IOException {
		this.task.getMainClass().set("com.example.Main");
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
		this.task.classpath(classesJavaMain, resourcesMain, jarFile("first-library.jar"), jarFile("second-library.jar"),
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
		LenientConfiguration lenientConfiguration = mock(LenientConfiguration.class);
		given(resolvedConfiguration.getLenientConfiguration()).willReturn(lenientConfiguration);
		given(lenientConfiguration.getArtifacts()).willReturn(artifacts);
		Configuration configuration = mock(Configuration.class);
		given(configuration.getResolvedConfiguration()).willReturn(resolvedConfiguration);
		ResolvableDependencies resolvableDependencies = mock(ResolvableDependencies.class);
		given(configuration.getIncoming()).willReturn(resolvableDependencies);
		DependencySet dependencies = mock(DependencySet.class);
		DomainObjectSet<ProjectDependency> projectDependencies = mock(DomainObjectSet.class);
		given(dependencies.withType(ProjectDependency.class)).willReturn(projectDependencies);
		given(configuration.getAllDependencies()).willReturn(dependencies);
		willAnswer((invocation) -> {
			invocation.getArgument(0, Action.class).execute(resolvableDependencies);
			return null;
		}).given(resolvableDependencies).afterResolve(any(Action.class));
		given(configuration.getIncoming()).willReturn(resolvableDependencies);
		populateResolvedDependencies(configuration);
	}

	abstract void populateResolvedDependencies(Configuration configuration);

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
		given(libraryArtifact.getFile()).willReturn(file);
		given(libraryArtifact.getModuleVersion()).willReturn(moduleVersion);
		return libraryArtifact;
	}

	List<String> entryLines(JarFile jarFile, String entryName) throws IOException {
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

}
