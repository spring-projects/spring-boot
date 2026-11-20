/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Enumeration;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.jarmode.JarModeErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for extracting an unpacked archive with {@link ExtractCommand}.
 *
 * @author Xinwei Zhang
 */
class ExtractCommandExplodedTests extends AbstractJarModeTests {

	private File archive;

	@BeforeEach
	void setUp() throws IOException {
		Manifest manifest = createManifest("Spring-Boot-Classpath-Index: BOOT-INF/classpath.idx",
				"Spring-Boot-Lib: BOOT-INF/lib/", "Spring-Boot-Classes: BOOT-INF/classes/",
				"Start-Class: org.example.Main", "Spring-Boot-Layers-Index: BOOT-INF/layers.idx");
		File packed = createArchive(manifest, "BOOT-INF/classpath.idx", "/jar-contents/classpath.idx",
				"BOOT-INF/layers.idx", "/jar-contents/layers.idx", "BOOT-INF/lib/dependency-1.jar",
				"/jar-contents/dependency-1", "BOOT-INF/lib/dependency-2.jar", "/jar-contents/dependency-2",
				"BOOT-INF/lib/dependency-3-SNAPSHOT.jar", "/jar-contents/dependency-3-SNAPSHOT",
				"BOOT-INF/classes/application.properties", "/jar-contents/application.properties",
				"org/springframework/boot/loader/launch/JarLauncher.class", "/jar-contents/JarLauncher");
		this.archive = unpack(packed, "unpacked-1.2");
	}

	private File unpack(File packed, String name) throws IOException {
		File directory = new File(this.tempDir, name);
		try (ZipFile zip = new ZipFile(packed)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				Path target = directory.toPath().resolve(entry.getName());
				Files.createDirectories(target.getParent());
				try (InputStream input = zip.getInputStream(entry)) {
					Files.copy(input, target);
				}
			}
		}
		return directory;
	}

	@Test
	void extractsLibrariesAndCreatesApplication() throws IOException {
		run("--destination", "out");
		assertThat(listFilenames())
			.contains("out/lib/dependency-1.jar", "out/lib/dependency-2.jar", "out/lib/dependency-3-SNAPSHOT.jar",
					"out/unpacked-1.2.jar")
			.doesNotContain("out/org/springframework/boot/loader/launch/JarLauncher.class");
		assertThat(getJarEntryNames(new File(this.tempDir, "out/unpacked-1.2.jar"))).contains("application.properties");
		assertThat(getJarManifestAttributes(new File(this.tempDir, "out/unpacked-1.2.jar")))
			.containsEntry("Main-Class", "org.example.Main")
			.containsEntry("Class-Path", "lib/dependency-1.jar lib/dependency-2.jar lib/dependency-3-SNAPSHOT.jar");
	}

	@Test
	void preservesWarWebResourcesWithoutWarDirectorySuffix() throws IOException {
		Manifest manifest = createManifest("Spring-Boot-Classpath-Index: WEB-INF/classpath.idx",
				"Spring-Boot-Lib: WEB-INF/lib/", "Spring-Boot-Lib-Provided: WEB-INF/lib-provided/",
				"Spring-Boot-Classes: WEB-INF/classes/", "Start-Class: org.example.Main");
		File packed = createArchive("source.war", manifest, null, null, null, "WEB-INF/classpath.idx",
				"/jar-contents/war-classpath.idx", "WEB-INF/lib/dependency-1.jar", "/jar-contents/dependency-1",
				"WEB-INF/lib-provided/dependency-2.jar", "/jar-contents/dependency-2",
				"WEB-INF/classes/application.properties", "/jar-contents/application.properties", "index.html",
				"/jar-contents/empty-file", "WEB-INF/jsp/hello.jsp", "/jar-contents/empty-file");
		this.archive = unpack(packed, "web-1.2");
		run("--destination", "out");
		assertThat(getJarEntryNames(new File(this.tempDir, "out/web-1.2.jar"))).contains("application.properties",
				"META-INF/resources/index.html", "META-INF/resources/WEB-INF/jsp/hello.jsp");
		assertThat(listFilenames()).contains("out/lib/dependency-1.jar", "out/lib/dependency-2.jar");
	}

	@Test
	void requiresDestination() {
		assertThatExceptionOfType(JarModeErrorException.class).isThrownBy(this::run)
			.withMessageContaining("--destination");
	}

	@Test
	void rejectsDestinationInsideSourceEvenWhenForced() {
		assertThatExceptionOfType(JarModeErrorException.class)
			.isThrownBy(() -> run("--destination", new File(this.archive, "out").getAbsolutePath(), "--force"))
			.withMessageContaining("source");
	}

	@Test
	void rejectsDestinationContainingSourceEvenWhenForced() {
		assertThatExceptionOfType(JarModeErrorException.class)
			.isThrownBy(() -> run("--destination", this.tempDir.getAbsolutePath(), "--force"))
			.withMessageContaining("source");
	}

	@Test
	void supportsCustomApplicationFilenameAndLibrariesDirectory() throws IOException {
		run("--destination", "out", "--application-filename", "app.jar", "--libraries", "dependencies");
		assertThat(getJarManifestAttributes(new File(this.tempDir, "out/app.jar"))).containsEntry("Class-Path",
				"dependencies/dependency-1.jar dependencies/dependency-2.jar dependencies/dependency-3-SNAPSHOT.jar");
		assertThat(new File(this.tempDir, "out/dependencies/dependency-1.jar")).exists();
	}

	@Test
	void extractsLayers() throws IOException {
		run("--destination", "out", "--layers");
		assertThat(listFilenames()).contains("out/dependencies/lib/dependency-1.jar",
				"out/dependencies/lib/dependency-2.jar", "out/snapshot-dependencies/lib/dependency-3-SNAPSHOT.jar",
				"out/application/unpacked-1.2.jar");
	}

	@Test
	void extractsOnlySelectedLayers() throws IOException {
		run("--destination", "out", "--layers", "dependencies");
		assertThat(listFilenames()).contains("out/dependencies/lib/dependency-1.jar")
			.doesNotContain("out/application/unpacked-1.2.jar",
					"out/snapshot-dependencies/lib/dependency-3-SNAPSHOT.jar");
	}

	@Test
	void extractsLauncher() throws IOException {
		run("--destination", "out", "--launcher");
		assertThat(listFilenames())
			.contains("out/BOOT-INF/lib/dependency-1.jar", "out/BOOT-INF/classes/application.properties",
					"out/org/springframework/boot/loader/launch/JarLauncher.class")
			.doesNotContain("out/unpacked-1.2.jar");
	}

	@Test
	void preservesFileTimesAndAppliesManifestTimeToCreatedDirectories() throws IOException {
		FileTime fileTime = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));
		FileTime manifestTime = FileTime.from(Instant.parse("2021-01-01T00:00:00Z"));
		Files.setLastModifiedTime(new File(this.archive, "BOOT-INF/lib/dependency-1.jar").toPath(), fileTime);
		Files.setLastModifiedTime(new File(this.archive, "META-INF/MANIFEST.MF").toPath(), manifestTime);
		run("--destination", "out");
		assertThat(Files.getLastModifiedTime(new File(this.tempDir, "out/lib/dependency-1.jar").toPath()))
			.isEqualTo(fileTime);
		assertThat(Files.getLastModifiedTime(new File(this.tempDir, "out/lib").toPath())).isEqualTo(manifestTime);
		assertThat(Files.getLastModifiedTime(new File(this.tempDir, "out").toPath())).isEqualTo(manifestTime);
	}

	private void run(String... arguments) {
		runCommand(ExtractCommand::new, this.archive, arguments);
	}

}
