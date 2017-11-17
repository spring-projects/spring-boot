/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.util.GUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.tools.DefaultLaunchScript;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for testing {@link BootArchive} implementations.
 *
 * @param <T> the type of the concrete BootArchive implementation
 * @author Andy Wilkinson
 */
public abstract class AbstractBootArchiveTests<T extends Jar & BootArchive> {

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	private final Class<T> taskClass;

	private final String launcherClass;

	private final String libPath;

	private final String classesPath;

	private T task;

	protected AbstractBootArchiveTests(Class<T> taskClass, String launcherClass,
			String libPath, String classesPath) {
		this.taskClass = taskClass;
		this.launcherClass = launcherClass;
		this.libPath = libPath;
		this.classesPath = classesPath;
	}

	@Before
	public void createTask() {
		try {
			Project project = ProjectBuilder.builder()
					.withProjectDir(this.temp.newFolder()).build();
			this.task = configure(
					project.getTasks().create("testArchive", this.taskClass));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Test
	public void basicArchiveCreation() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.execute();
		assertThat(this.task.getArchivePath().exists());
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Main-Class"))
					.isEqualTo(this.launcherClass);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.Main");
		}
	}

	@Test
	public void classpathJarsArePackagedBeneathLibPath() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.classpath(this.temp.newFile("one.jar"), this.temp.newFile("two.jar"));
		this.task.execute();
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getEntry(this.libPath + "/one.jar")).isNotNull();
			assertThat(jarFile.getEntry(this.libPath + "/two.jar")).isNotNull();
		}
	}

	@Test
	public void classpathFoldersArePackagedBeneathClassesPath() throws IOException {
		this.task.setMainClassName("com.example.Main");
		File classpathFolder = this.temp.newFolder();
		File applicationClass = new File(classpathFolder,
				"com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		this.task.classpath(classpathFolder);
		this.task.execute();
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(
					jarFile.getEntry(this.classesPath + "/com/example/Application.class"))
							.isNotNull();
		}
	}

	@Test
	public void loaderIsWrittenToTheRootOfTheJar() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.execute();
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getEntry(
					"org/springframework/boot/loader/LaunchedURLClassLoader.class"))
							.isNotNull();
			assertThat(jarFile.getEntry("org/springframework/boot/loader/")).isNotNull();
		}
	}

	@Test
	public void loaderIsWrittenToTheRootOfTheJarWhenUsingThePropertiesLauncher()
			throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.execute();
		this.task.getManifest().getAttributes().put("Main-Class",
				"org.springframework.boot.loader.PropertiesLauncher");
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getEntry(
					"org/springframework/boot/loader/LaunchedURLClassLoader.class"))
							.isNotNull();
			assertThat(jarFile.getEntry("org/springframework/boot/loader/")).isNotNull();
		}
	}

	@Test
	public void unpackCommentIsAddedToEntryIdentifiedByAPattern() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.classpath(this.temp.newFile("one.jar"), this.temp.newFile("two.jar"));
		this.task.requiresUnpack("**/one.jar");
		this.task.execute();
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getEntry(this.libPath + "/one.jar").getComment())
					.startsWith("UNPACK:");
			assertThat(jarFile.getEntry(this.libPath + "/two.jar").getComment()).isNull();
		}
	}

	@Test
	public void unpackCommentIsAddedToEntryIdentifiedByASpec() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.classpath(this.temp.newFile("one.jar"), this.temp.newFile("two.jar"));
		this.task.requiresUnpack((element) -> element.getName().endsWith("two.jar"));
		this.task.execute();
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getEntry(this.libPath + "/two.jar").getComment())
					.startsWith("UNPACK:");
			assertThat(jarFile.getEntry(this.libPath + "/one.jar").getComment()).isNull();
		}
	}

	@Test
	public void launchScriptCanBePrepended() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.launchScript();
		this.task.execute();
		assertThat(Files.readAllBytes(this.task.getArchivePath().toPath()))
				.startsWith(new DefaultLaunchScript(null, null).toByteArray());
		try {
			Set<PosixFilePermission> permissions = Files
					.getPosixFilePermissions(this.task.getArchivePath().toPath());
			assertThat(permissions).contains(PosixFilePermission.OWNER_EXECUTE);
		}
		catch (UnsupportedOperationException ex) {
			// Windows, presumably. Continue
		}
	}

	@Test
	public void customLaunchScriptCanBePrepended() throws IOException {
		this.task.setMainClassName("com.example.Main");
		File customScript = this.temp.newFile("custom.script");
		Files.write(customScript.toPath(), Arrays.asList("custom script"),
				StandardOpenOption.CREATE);
		this.task.launchScript((configuration) -> configuration.setScript(customScript));
		this.task.execute();
		assertThat(Files.readAllBytes(this.task.getArchivePath().toPath()))
				.startsWith("custom script".getBytes());
	}

	@Test
	public void launchScriptPropertiesAreReplaced() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.launchScript((configuration) -> configuration.getProperties()
				.put("initInfoProvides", "test property value"));
		this.task.execute();
		assertThat(Files.readAllBytes(this.task.getArchivePath().toPath()))
				.containsSequence("test property value".getBytes());
	}

	@Test
	public void customMainClassInTheManifestIsHonored() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.getManifest().getAttributes().put("Main-Class",
				"com.example.CustomLauncher");
		this.task.execute();
		assertThat(this.task.getArchivePath().exists());
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Main-Class"))
					.isEqualTo("com.example.CustomLauncher");
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.Main");
			assertThat(jarFile.getEntry(
					"org/springframework/boot/loader/LaunchedURLClassLoader.class"))
							.isNull();
		}
	}

	@Test
	public void customStartClassInTheManifestIsHonored() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.getManifest().getAttributes().put("Start-Class",
				"com.example.CustomMain");
		this.task.execute();
		assertThat(this.task.getArchivePath().exists());
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Main-Class"))
					.isEqualTo(this.launcherClass);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.CustomMain");
		}
	}

	@Test
	public void fileTimestampPreservationCanBeDisabled() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.setPreserveFileTimestamps(false);
		this.task.execute();
		assertThat(this.task.getArchivePath().exists());
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				assertThat(entry.getTime())
						.isEqualTo(GUtil.CONSTANT_TIME_FOR_ZIP_ENTRIES);
			}
		}
	}

	@Test
	public void reproducibleOrderingCanBeEnabled() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.from(this.temp.newFile("bravo.txt"), this.temp.newFile("alpha.txt"),
				this.temp.newFile("charlie.txt"));
		this.task.setReproducibleFileOrder(true);
		this.task.execute();
		assertThat(this.task.getArchivePath().exists());
		List<String> textFiles = new ArrayList<>();
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
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
	public void devtoolsJarIsExcludedByDefault() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.classpath(this.temp.newFile("spring-boot-devtools-0.1.2.jar"));
		this.task.execute();
		assertThat(this.task.getArchivePath().exists());
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getEntry(this.libPath + "/spring-boot-devtools-0.1.2.jar"))
					.isNull();
		}
	}

	@Test
	public void devtoolsJarCanBeIncluded() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.classpath(this.temp.newFile("spring-boot-devtools-0.1.2.jar"));
		this.task.setExcludeDevtools(false);
		this.task.execute();
		assertThat(this.task.getArchivePath().exists());
		try (JarFile jarFile = new JarFile(this.task.getArchivePath())) {
			assertThat(jarFile.getEntry(this.libPath + "/spring-boot-devtools-0.1.2.jar"))
					.isNotNull();
		}
	}

	@Test
	public void allEntriesUseUnixPlatformAndUtf8NameEncoding() throws IOException {
		this.task.setMainClassName("com.example.Main");
		this.task.setMetadataCharset("UTF-8");
		File classpathFolder = this.temp.newFolder();
		File resource = new File(classpathFolder, "some-resource.xml");
		resource.getParentFile().mkdirs();
		resource.createNewFile();
		this.task.classpath(classpathFolder);
		this.task.execute();
		File archivePath = this.task.getArchivePath();
		try (ZipFile zip = new ZipFile(archivePath)) {
			Enumeration<ZipArchiveEntry> entries = zip.getEntries();
			while (entries.hasMoreElements()) {
				ZipArchiveEntry entry = entries.nextElement();
				assertThat(entry.getPlatform()).isEqualTo(ZipArchiveEntry.PLATFORM_UNIX);
				assertThat(entry.getGeneralPurposeBit().usesUTF8ForNames()).isTrue();
			}
		}
	}

	private T configure(T task) throws IOException {
		AbstractArchiveTask archiveTask = task;
		archiveTask.setBaseName("test");
		archiveTask.setDestinationDir(this.temp.newFolder());
		return task;
	}

	protected T getTask() {
		return this.task;
	}

}
