/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link TomcatNestedJar}.
 *
 * @author Manthan Sharma
 */
class TomcatNestedJarTests {

	@TempDir
	File tempDir;

	private File jarFile;

	@BeforeEach
	void setUp() throws IOException {
		this.jarFile = new File(this.tempDir, "test.jar");
		createTestJar(this.jarFile, false);
	}

	@Test
	void getJarFileUrlReturnsUrl() throws Exception {
		URL jarUrl = new URL("jar:" + this.jarFile.toURI().toURL() + "!/");
		try (TomcatNestedJar jar = new TomcatNestedJar(jarUrl)) {
			assertThat(jar.getJarFileURL()).isEqualTo(this.jarFile.toURI().toURL());
		}
	}

	@Test
	void existsReturnsTrueWhenEntryExists() throws Exception {
		URL jarUrl = new URL("jar:" + this.jarFile.toURI().toURL() + "!/");
		try (TomcatNestedJar jar = new TomcatNestedJar(jarUrl)) {
			assertThat(jar.exists("entry1.txt")).isTrue();
			assertThat(jar.exists("nested/entry2.txt")).isTrue();
			assertThat(jar.exists("nonexistent.txt")).isFalse();
		}
	}

	@Test
	void getInputStreamReturnsContentWhenEntryExists() throws Exception {
		URL jarUrl = new URL("jar:" + this.jarFile.toURI().toURL() + "!/");
		try (TomcatNestedJar jar = new TomcatNestedJar(jarUrl)) {
			try (InputStream inputStream = jar.getInputStream("entry1.txt")) {
				assertThat(inputStream).isNotNull();
				assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("content1");
			}
			assertThat(jar.getInputStream("nonexistent.txt")).isNull();
		}
	}

	@Test
	void getLastModifiedReturnsTimestamp() throws Exception {
		URL jarUrl = new URL("jar:" + this.jarFile.toURI().toURL() + "!/");
		try (TomcatNestedJar jar = new TomcatNestedJar(jarUrl)) {
			assertThat(jar.getLastModified("entry1.txt")).isGreaterThan(0);
			assertThat(jar.getLastModified("nonexistent.txt")).isEqualTo(-1);
		}
	}

	@Test
	void getUrlReturnsJarUrlForEntry() throws Exception {
		URL jarUrl = new URL("jar:" + this.jarFile.toURI().toURL() + "!/");
		try (TomcatNestedJar jar = new TomcatNestedJar(jarUrl)) {
			String entryUrl = jar.getURL("entry1.txt");
			assertThat(entryUrl).isEqualTo("jar:" + this.jarFile.toURI().toURL() + "!/entry1.txt");
		}
	}

	@Test
	void getManifestReturnsManifest() throws Exception {
		URL jarUrl = new URL("jar:" + this.jarFile.toURI().toURL() + "!/");
		try (TomcatNestedJar jar = new TomcatNestedJar(jarUrl)) {
			Manifest manifest = jar.getManifest();
			assertThat(manifest).isNotNull();
			assertThat(manifest.getMainAttributes().getValue("Test-Attribute")).isEqualTo("test-value");
		}
	}

	@Test
	void nextEntryIteratesEntriesAndResets() throws Exception {
		URL jarUrl = new URL("jar:" + this.jarFile.toURI().toURL() + "!/");
		try (TomcatNestedJar jar = new TomcatNestedJar(jarUrl)) {
			List<String> entryNames = new ArrayList<>();
			jar.nextEntry();
			while (jar.getEntryName() != null) {
				entryNames.add(jar.getEntryName());
				try (InputStream in = jar.getEntryInputStream()) {
					assertThat(in).isNotNull();
				}
				jar.nextEntry();
			}
			assertThat(entryNames).contains("entry1.txt", "nested/entry2.txt");

			jar.reset();
			List<String> resetNames = new ArrayList<>();
			jar.nextEntry();
			while (jar.getEntryName() != null) {
				resetNames.add(jar.getEntryName());
				jar.nextEntry();
			}
			assertThat(resetNames).isEqualTo(entryNames);
		}
	}

	@Test
	void multiReleaseJarEnumeratesDeduplicatedEntries() throws Exception {
		File mrJarFile = new File(this.tempDir, "mr.jar");
		createTestJar(mrJarFile, true);
		URL jarUrl = new URL("jar:" + mrJarFile.toURI().toURL() + "!/");
		try (TomcatNestedJar jar = new TomcatNestedJar(jarUrl)) {
			List<String> entryNames = new ArrayList<>();
			jar.nextEntry();
			while (jar.getEntryName() != null) {
				entryNames.add(jar.getEntryName());
				jar.nextEntry();
			}
			assertThat(entryNames).contains("entry1.txt", "nested/entry2.txt");
		}
	}

	@Test
	void closeClosesUnderlyingJarFile() throws Exception {
		JarFile rawJar = new JarFile(this.jarFile);
		TomcatNestedJar jar = new TomcatNestedJar(rawJar, this.jarFile.toURI().toURL());
		jar.close();
		// underlying JarFile is closed, further access should fail
		assertThatIllegalStateException().isThrownBy(() -> rawJar.getEntry("entry1.txt"));
	}

	private void createTestJar(File file, boolean multiRelease) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().putValue("Test-Attribute", "test-value");
		if (multiRelease) {
			manifest.getMainAttributes().put(Attributes.Name.MULTI_RELEASE, "true");
		}
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(file), manifest)) {
			JarEntry entry1 = new JarEntry("entry1.txt");
			out.putNextEntry(entry1);
			out.write("content1".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();

			JarEntry entry2 = new JarEntry("nested/entry2.txt");
			out.putNextEntry(entry2);
			out.write("content2".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();

			if (multiRelease) {
				JarEntry mrEntry = new JarEntry("META-INF/versions/21/entry1.txt");
				out.putNextEntry(mrEntry);
				out.write("content1-v21".getBytes(StandardCharsets.UTF_8));
				out.closeEntry();
			}
		}
	}

}
