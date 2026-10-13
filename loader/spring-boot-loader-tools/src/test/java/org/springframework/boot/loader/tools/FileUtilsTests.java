/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileUtils}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class FileUtilsTests {

	@TempDir
	File tempDir;

	private File outputDirectory;

	private File originDirectory;

	@BeforeEach
	void init() {
		this.outputDirectory = new File(this.tempDir, "remove");
		this.originDirectory = new File(this.tempDir, "keep");
		this.outputDirectory.mkdirs();
		this.originDirectory.mkdirs();
	}

	@Test
	void simpleDuplicateFile() throws IOException {
		File file = new File(this.outputDirectory, "logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "logback.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory, this.originDirectory);
		assertThat(file).doesNotExist();
	}

	@Test
	void nestedDuplicateFile() throws IOException {
		assertThat(new File(this.outputDirectory, "sub").mkdirs()).isTrue();
		assertThat(new File(this.originDirectory, "sub").mkdirs()).isTrue();
		File file = new File(this.outputDirectory, "sub/logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "sub/logback.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory, this.originDirectory);
		assertThat(file).doesNotExist();
	}

	@Test
	void nestedNonDuplicateFile() throws IOException {
		assertThat(new File(this.outputDirectory, "sub").mkdirs()).isTrue();
		assertThat(new File(this.originDirectory, "sub").mkdirs()).isTrue();
		File file = new File(this.outputDirectory, "sub/logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "sub/different.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory, this.originDirectory);
		assertThat(file).exists();
	}

	@Test
	void nonDuplicateFile() throws IOException {
		File file = new File(this.outputDirectory, "logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "different.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory, this.originDirectory);
		assertThat(file).exists();
	}

	@Test
	void isSignedJarFileWhenSignedReturnsTrue() throws IOException {
		Manifest manifest = new Manifest(getClass().getResourceAsStream("signed-manifest.mf"));
		File jarFile = new File(this.tempDir, "test.jar");
		writeTestJar(manifest, jarFile);
		assertThat(FileUtils.isSignedJarFile(jarFile)).isTrue();
	}

	@Test
	void isSignedJarFileWhenNotSignedReturnsFalse() throws IOException {
		Manifest manifest = new Manifest();
		File jarFile = new File(this.tempDir, "test.jar");
		writeTestJar(manifest, jarFile);
		assertThat(FileUtils.isSignedJarFile(jarFile)).isFalse();
	}

	private void writeTestJar(Manifest manifest, File jarFile) throws IOException, FileNotFoundException {
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jarFile))) {
			out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
			manifest.write(out);
			out.closeEntry();
		}
	}

}
