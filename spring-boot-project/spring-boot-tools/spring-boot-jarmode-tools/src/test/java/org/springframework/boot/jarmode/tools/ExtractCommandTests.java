/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ExtractCommand}.
 *
 * @author Moritz Halbritter
 */
class ExtractCommandTests extends AbstractJarModeTests {

	private static final Instant CREATION_TIME = Instant.parse("2020-01-01T00:00:00Z");

	private static final Instant LAST_MODIFIED_TIME = Instant.parse("2021-01-01T00:00:00Z");

	private static final Instant LAST_ACCESS_TIME = Instant.parse("2022-01-01T00:00:00Z");

	private Manifest manifest;

	private File archive;

	@BeforeEach
	void setUp() throws IOException {
		this.manifest = createManifest("Spring-Boot-Classpath-Index: BOOT-INF/classpath.idx",
				"Spring-Boot-Lib: BOOT-INF/lib/", "Spring-Boot-Classes: BOOT-INF/classes/",
				"Start-Class: org.example.Main", "Spring-Boot-Layers-Index: BOOT-INF/layers.idx",
				"Some-Attribute: Some-Value");
		this.archive = createArchive(this.manifest, CREATION_TIME, LAST_MODIFIED_TIME, LAST_ACCESS_TIME,
				"BOOT-INF/classpath.idx", "/jar-contents/classpath.idx", "BOOT-INF/layers.idx",
				"/jar-contents/layers.idx", "BOOT-INF/lib/dependency-1.jar", "/jar-contents/dependency-1",
				"BOOT-INF/lib/dependency-2.jar", "/jar-contents/dependency-2", "BOOT-INF/lib/dependency-3-SNAPSHOT.jar",
				"/jar-contents/dependency-3-SNAPSHOT", "org/springframework/boot/loader/launch/JarLauncher.class",
				"/jar-contents/JarLauncher", "BOOT-INF/classes/application.properties",
				"/jar-contents/application.properties", "META-INF/build-info.properties",
				"/jar-contents/build-info.properties");
	}

	private File file(String name) {
		return new File(this.tempDir, name);
	}

	private TestPrintStream run(File archive, String... args) {
		return runCommand(ExtractCommand::new, archive, args);
	}

	private void timeAttributes(File file) {
		try {
			BasicFileAttributes basicAttributes = Files
				.getFileAttributeView(file.toPath(), BasicFileAttributeView.class)
				.readAttributes();
			assertThat(basicAttributes.lastModifiedTime().toInstant().truncatedTo(ChronoUnit.SECONDS))
				.as("last modified time")
				.isEqualTo(LAST_MODIFIED_TIME.truncatedTo(ChronoUnit.SECONDS));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Nested
	class Extract {

		@Test
		void extractLibrariesAndCreatesApplication() throws IOException {
			run(ExtractCommandTests.this.archive);
			List<String> filenames = listFilenames();
			assertThat(filenames).contains("test/lib/dependency-1.jar")
				.contains("test/lib/dependency-2.jar")
				.contains("test/lib/dependency-3-SNAPSHOT.jar")
				.contains("test/test.jar")
				.doesNotContain("test/org/springframework/boot/loader/launch/JarLauncher.class");
		}

		@Test
		void extractLibrariesAndCreatesApplicationInDestination() throws IOException {
			run(ExtractCommandTests.this.archive, "--destination", file("out").getAbsolutePath());
			List<String> filenames = listFilenames();
			assertThat(filenames).contains("out/lib/dependency-1.jar")
				.contains("out/lib/dependency-2.jar")
				.contains("out/lib/dependency-3-SNAPSHOT.jar")
				.contains("out/test.jar");
		}

		@Test
		void applicationNameAndLibrariesDirectoriesCanBeCustomized() throws IOException {
			run(ExtractCommandTests.this.archive, "--application-filename", "application-customized.jar", "--libraries",
					"dependencies");
			List<String> filenames = listFilenames();
			assertThat(filenames).contains("test/dependencies/dependency-1.jar")
				.contains("test/dependencies/dependency-2.jar")
				.contains("test/dependencies/dependency-3-SNAPSHOT.jar");
			File application = file("test/application-customized.jar");
			assertThat(application).exists();
			Map<String, String> attributes = getJarManifestAttributes(application);
			assertThat(attributes).containsEntry("Class-Path",
					"dependencies/dependency-1.jar dependencies/dependency-2.jar dependencies/dependency-3-SNAPSHOT.jar");
		}

		@Test
		void applicationContainsManifestEntries() throws IOException {
			run(ExtractCommandTests.this.archive);
			File application = file("test/test.jar");
			Map<String, String> attributes = getJarManifestAttributes(application);
			assertThat(attributes).containsEntry("Main-Class", "org.example.Main")
				.containsEntry("Class-Path", "lib/dependency-1.jar lib/dependency-2.jar lib/dependency-3-SNAPSHOT.jar")
				.containsEntry("Some-Attribute", "Some-Value")
				.doesNotContainKeys("Start-Class", "Spring-Boot-Classes", "Spring-Boot-Lib",
						"Spring-Boot-Classpath-Index", "Spring-Boot-Layers-Index");
		}

		@Test
		void applicationContainsApplicationClassesAndResources() throws IOException {
			run(ExtractCommandTests.this.archive);
			File application = file("test/test.jar");
			List<String> entryNames = getJarEntryNames(application);
			assertThat(entryNames).contains("application.properties");
		}

		@Test
		void appliesFileTimes() {
			run(ExtractCommandTests.this.archive);
			assertThat(file("test/lib/dependency-1.jar")).exists().satisfies(ExtractCommandTests.this::timeAttributes);
			assertThat(file("test/lib/dependency-2.jar")).exists().satisfies(ExtractCommandTests.this::timeAttributes);
			assertThat(file("test/lib/dependency-3-SNAPSHOT.jar")).exists()
				.satisfies(ExtractCommandTests.this::timeAttributes);
		}

		@Test
		void applicationDoesntContainLibraries() throws IOException {
			run(ExtractCommandTests.this.archive);
			File application = file("test/test.jar");
			List<String> entryNames = getJarEntryNames(application);
			assertThat(entryNames).doesNotContain("BOOT-INF/lib/dependency-1.jar", "BOOT-INF/lib/dependency-2.jar");
		}

		@Test
		void failsOnIncompatibleJar() throws IOException {
			File file = file("empty.jar");
			try (FileWriter writer = new FileWriter(file)) {
				writer.write("text");
			}
			TestPrintStream out = run(file);
			assertThat(out).contains("is not compatible; ensure jar file is valid and launch script is not enabled");
		}

		@Test
		void shouldFailIfDirectoryIsNotEmpty() throws IOException {
			File destination = file("out");
			Files.createDirectories(destination.toPath());
			Files.createFile(new File(destination, "file.txt").toPath());
			TestPrintStream out = run(ExtractCommandTests.this.archive, "--destination", destination.getAbsolutePath());
			assertThat(out).contains("already exists and is not empty");
		}

		@Test
		void shouldNotFailIfDirectoryExistsButIsEmpty() throws IOException {
			File destination = file("out");
			Files.createDirectories(destination.toPath());
			run(ExtractCommandTests.this.archive, "--destination", destination.getAbsolutePath());
			List<String> filenames = listFilenames();
			assertThat(filenames).contains("out/lib/dependency-1.jar")
				.contains("out/lib/dependency-2.jar")
				.contains("out/lib/dependency-3-SNAPSHOT.jar")
				.contains("out/test.jar");
		}

		@Test
		void shouldNotFailIfDirectoryIsNotEmptyButForceIsPassed() throws IOException {
			File destination = file("out");
			Files.createDirectories(destination.toPath());
			Files.createFile(new File(destination, "file.txt").toPath());
			run(ExtractCommandTests.this.archive, "--destination", destination.getAbsolutePath(), "--force");
			List<String> filenames = listFilenames();
			assertThat(filenames).contains("out/lib/dependency-1.jar")
				.contains("out/lib/dependency-2.jar")
				.contains("out/lib/dependency-3-SNAPSHOT.jar")
				.contains("out/test.jar");
		}

		@Test
		void shouldExtractFilesUnderMetaInf() throws IOException {
			run(ExtractCommandTests.this.archive);
			File application = file("test/test.jar");
			List<String> entryNames = getJarEntryNames(application);
			assertThat(entryNames).contains("META-INF/build-info.properties");
		}

		@Test
		void shouldNotFailOnDuplicateDirectories() throws IOException {
			File file = createArchive(ExtractCommandTests.this.manifest, "BOOT-INF/classpath.idx",
					"/jar-contents/classpath.idx", "META-INF/native-image/", "/jar-contents/empty-file",
					"BOOT-INF/classes/META-INF/native-image/", "/jar-contents/empty-file");
			run(file);
			File application = file("test/test.jar");
			List<String> entryNames = getJarEntryNames(application);
			assertThat(entryNames).containsExactlyInAnyOrder("META-INF/native-image/", "META-INF/MANIFEST.MF");
		}

		@Test
		void shouldFailOnDuplicateFiles() throws IOException {
			File file = createArchive(ExtractCommandTests.this.manifest, "BOOT-INF/classpath.idx",
					"/jar-contents/classpath.idx", "META-INF/native-image/native-image.properties",
					"/jar-contents/empty-file", "BOOT-INF/classes/META-INF/native-image/native-image.properties",
					"/jar-contents/empty-file");
			assertThatIllegalStateException().isThrownBy(() -> run(file))
				.withMessage(
						"Duplicate jar entry 'META-INF/native-image/native-image.properties' from original location 'BOOT-INF/classes/META-INF/native-image/native-image.properties'");
		}

	}

	@Nested
	class ExtractWithLayers {

		@Test
		void extractLibrariesAndCreatesApplication() throws IOException {
			run(ExtractCommandTests.this.archive, "--layers");
			List<String> filenames = listFilenames();
			assertThat(filenames).contains("test/dependencies/lib/dependency-1.jar")
				.contains("test/dependencies/lib/dependency-2.jar")
				.contains("test/snapshot-dependencies/lib/dependency-3-SNAPSHOT.jar")
				.contains("test/application/test.jar");
		}

		@Test
		void extractsOnlySelectedLayers() throws IOException {
			run(ExtractCommandTests.this.archive, "--layers", "dependencies");
			List<String> filenames = listFilenames();
			assertThat(filenames).contains("test/dependencies/lib/dependency-1.jar")
				.contains("test/dependencies/lib/dependency-2.jar")
				.doesNotContain("test/snapshot-dependencies/lib/dependency-3-SNAPSHOT.jar")
				.doesNotContain("test/application/test.jar");
		}

		@Test
		void printErrorIfLayersAreNotEnabled() throws IOException {
			File archive = createArchive();
			TestPrintStream out = run(archive, "--layers");
			assertThat(out).hasSameContentAsResource("ExtractCommand-printErrorIfLayersAreNotEnabled.txt");
		}

	}

	@Nested
	class ExtractLauncher {

		@Test
		void extract() throws IOException {
			run(ExtractCommandTests.this.archive, "--launcher");
			List<String> filenames = listFilenames();
			assertThat(filenames).contains("test/META-INF/MANIFEST.MF")
				.contains("test/BOOT-INF/classpath.idx")
				.contains("test/BOOT-INF/layers.idx")
				.contains("test/BOOT-INF/lib/dependency-1.jar")
				.contains("test/BOOT-INF/lib/dependency-2.jar")
				.contains("test/BOOT-INF/lib/dependency-3-SNAPSHOT.jar")
				.contains("test/BOOT-INF/classes/application.properties")
				.contains("test/org/springframework/boot/loader/launch/JarLauncher.class");
		}

		@Test
		void runWithJarFileThatWouldWriteEntriesOutsideDestinationFails() throws Exception {
			File file = createArchive("e/../../e.jar", null);
			assertThatIllegalStateException().isThrownBy(() -> run(file, "--launcher"))
				.withMessageContaining("Entry 'e/../../e.jar' would be written");
		}

	}

	@Nested
	class ExtractLauncherWithLayers {

		@Test
		void extract() throws IOException {
			run(ExtractCommandTests.this.archive, "--launcher", "--layers");
			List<String> filenames = listFilenames();
			assertThat(filenames).contains("test/application/META-INF/MANIFEST.MF")
				.contains("test/application/BOOT-INF/classpath.idx")
				.contains("test/application/BOOT-INF/layers.idx")
				.contains("test/dependencies/BOOT-INF/lib/dependency-1.jar")
				.contains("test/dependencies/BOOT-INF/lib/dependency-2.jar")
				.contains("test/snapshot-dependencies/BOOT-INF/lib/dependency-3-SNAPSHOT.jar")
				.contains("test/application/BOOT-INF/classes/application.properties")
				.contains("test/spring-boot-loader/org/springframework/boot/loader/launch/JarLauncher.class");
		}

		@Test
		void printErrorIfLayersAreNotEnabled() throws IOException {
			File archive = createArchive();
			TestPrintStream out = run(archive, "--launcher", "--layers");
			assertThat(out).hasSameContentAsResource("ExtractCommand-printErrorIfLayersAreNotEnabled.txt");
		}

		@Test
		void extractsOnlySelectedLayers() throws IOException {
			run(ExtractCommandTests.this.archive, "--launcher", "--layers", "dependencies");
			List<String> filenames = listFilenames();
			assertThat(filenames).doesNotContain("test/application/META-INF/MANIFEST.MF")
				.doesNotContain("test/application/BOOT-INF/classpath.idx")
				.doesNotContain("test/application/BOOT-INF/layers.idx")
				.contains("test/dependencies/BOOT-INF/lib/dependency-1.jar")
				.contains("test/dependencies/BOOT-INF/lib/dependency-2.jar")
				.doesNotContain("test/snapshot-dependencies/BOOT-INF/lib/dependency-3-SNAPSHOT.jar")
				.doesNotContain("test/application/BOOT-INF/classes/application.properties")
				.doesNotContain("test/spring-boot-loader/org/springframework/boot/loader/launch/JarLauncher.class");
		}

	}

}
