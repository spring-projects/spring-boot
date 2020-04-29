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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.gradle.api.Project;
import org.gradle.api.internal.file.archive.ZipCopyAction;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.tools.DefaultLaunchScript;

import static org.assertj.core.api.Assertions.assertThat;

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

	private Project project;

	private T task;

	protected AbstractBootArchiveTests(Class<T> taskClass, String launcherClass, String libPath, String classesPath) {
		this.taskClass = taskClass;
		this.launcherClass = launcherClass;
		this.libPath = libPath;
		this.classesPath = classesPath;
	}

	@BeforeEach
	void createTask() {
		try {
			File projectDir = new File(this.temp, "project");
			projectDir.mkdirs();
			this.project = ProjectBuilder.builder().withProjectDir(projectDir).build();
			this.project.setDescription("Test project for " + this.taskClass.getSimpleName());
			this.task = configure(this.project.getTasks().create("testArchive", this.taskClass));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Test
	void basicArchiveCreation() throws IOException {
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
		this.task.classpath(new File("test.pom"));
		executeTask();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "/test.pom")).isNull();
		}
	}

	@Test
	void loaderIsWrittenToTheRootOfTheJarAfterManifest() throws IOException {
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
		executeTask();
		this.task.getManifest().getAttributes().put("Main-Class", "org.springframework.boot.loader.PropertiesLauncher");
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("org/springframework/boot/loader/LaunchedURLClassLoader.class")).isNotNull();
			assertThat(jarFile.getEntry("org/springframework/boot/loader/")).isNotNull();
		}
	}

	@Test
	void unpackCommentIsAddedToEntryIdentifiedByAPattern() throws IOException {
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
		this.task.launchScript();
		executeTask();
		Map<String, String> properties = new HashMap<>();
		properties.put("initInfoProvides", this.task.getArchiveBaseName().get());
		properties.put("initInfoShortDescription", this.project.getDescription());
		properties.put("initInfoDescription", this.project.getDescription());
		assertThat(Files.readAllBytes(this.task.getArchiveFile().get().getAsFile().toPath()))
				.startsWith(new DefaultLaunchScript(null, properties).toByteArray());
		try {
			Set<PosixFilePermission> permissions = Files
					.getPosixFilePermissions(this.task.getArchiveFile().get().getAsFile().toPath());
			assertThat(permissions).contains(PosixFilePermission.OWNER_EXECUTE);
		}
		catch (UnsupportedOperationException ex) {
			// Windows, presumably. Continue
		}
	}

	@Test
	void customLaunchScriptCanBePrepended() throws IOException {
		this.task.setMainClassName("com.example.Main");
		File customScript = new File(this.temp, "custom.script");
		Files.write(customScript.toPath(), Arrays.asList("custom script"), StandardOpenOption.CREATE);
		this.task.launchScript((configuration) -> configuration.setScript(customScript));
		executeTask();
		assertThat(Files.readAllBytes(this.task.getArchiveFile().get().getAsFile().toPath()))
				.startsWith("custom script".getBytes());
	}

	@Test
	void launchScriptInitInfoPropertiesCanBeCustomized() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.launchScript((configuration) -> {
			configuration.getProperties().put("initInfoProvides", "provides");
			configuration.getProperties().put("initInfoShortDescription", "short description");
			configuration.getProperties().put("initInfoDescription", "description");
		});
		executeTask();
		byte[] bytes = Files.readAllBytes(this.task.getArchiveFile().get().getAsFile().toPath());
		assertThat(bytes).containsSequence("Provides:          provides".getBytes());
		assertThat(bytes).containsSequence("Short-Description: short description".getBytes());
		assertThat(bytes).containsSequence("Description:       description".getBytes());
	}

	@Test
	void customMainClassInTheManifestIsHonored() throws IOException {
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
		this.task.classpath(newFile("spring-boot-devtools-0.1.2.jar"));
		executeTask();
		assertThat(this.task.getArchiveFile().get().getAsFile()).exists();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "spring-boot-devtools-0.1.2.jar")).isNull();
		}
	}

	@Test
	@Deprecated
	void devtoolsJarCanBeIncluded() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.classpath(jarFile("spring-boot-devtools-0.1.2.jar"));
		this.task.setExcludeDevtools(false);
		executeTask();
		assertThat(this.task.getArchiveFile().get().getAsFile()).exists();
		try (JarFile jarFile = new JarFile(this.task.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry(this.libPath + "spring-boot-devtools-0.1.2.jar")).isNotNull();
		}
	}

	@Test
	void allEntriesUseUnixPlatformAndUtf8NameEncoding() throws IOException {
		this.task.setMainClassName("com.example.Main");
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
		this.task.setMainClassName("com.example.Main");
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

	protected File jarFile(String name) throws IOException {
		File file = newFile(name);
		try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(file))) {
			jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
			new Manifest().write(jar);
			jar.closeEntry();
		}
		return file;
	}

	private T configure(T task) throws IOException {
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

}
