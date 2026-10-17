/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.net.protocol.Handlers;
import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarUrlClassLoader}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class JarUrlClassLoaderTests {

	private static final URL APP_JAR;
	static {
		try {
			APP_JAR = new URL("jar:file:src/test/resources/jars/app.jar!/");
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@TempDir
	File tempDir;

	@BeforeAll
	static void setup() {
		Handlers.register();
	}

	@Test
	void resolveResourceFromArchive() throws Exception {
		try (JarUrlClassLoader loader = new TestJarUrlClassLoader(APP_JAR)) {
			assertThat(loader.getResource("demo/Application.java")).isNotNull();
		}
	}

	@Test
	void resolveResourcesFromArchive() throws Exception {
		try (JarUrlClassLoader loader = new TestJarUrlClassLoader(APP_JAR)) {
			assertThat(loader.getResources("demo/Application.java").hasMoreElements()).isTrue();
		}
	}

	@Test
	void resolveRootPathFromArchive() throws Exception {
		try (JarUrlClassLoader loader = new TestJarUrlClassLoader(APP_JAR)) {
			assertThat(loader.getResource("")).isNotNull();
		}
	}

	@Test
	void resolveRootResourcesFromArchive() throws Exception {
		try (JarUrlClassLoader loader = new TestJarUrlClassLoader(APP_JAR)) {
			assertThat(loader.getResources("").hasMoreElements()).isTrue();
		}
	}

	@Test
	void resolveFromNested() throws Exception {
		File jarFile = new File(this.tempDir, "test.jar");
		TestJar.create(jarFile);
		URL url = JarUrl.create(jarFile, "nested.jar");
		try (JarUrlClassLoader loader = new TestJarUrlClassLoader(url)) {
			URL resource = loader.getResource("3.dat");
			assertThat(resource).hasToString(url + "3.dat");
			try (InputStream input = resource.openConnection().getInputStream()) {
				assertThat(input.read()).isEqualTo(3);
			}
		}
	}

	@Test
	void loadClass() throws Exception {
		try (JarUrlClassLoader loader = new TestJarUrlClassLoader(APP_JAR)) {
			assertThat(loader.loadClass("demo.Application")).isNotNull().hasToString("class demo.Application");
		}
	}

	@Test
	void loadClassFromNested() throws Exception {
		File appJarFile = new File("src/test/resources/jars/app.jar");
		File jarFile = createJarWithNestedAppJar(Files.readAllBytes(appJarFile.toPath()));
		URL url = JarUrl.create(jarFile, "app.jar");
		try (JarUrlClassLoader loader = new TestJarUrlClassLoader(url)) {
			assertThat(loader.loadClass("demo.Application")).isNotNull().hasToString("class demo.Application");
		}
	}

	@Test
	void loadClassFromNestedWhenJarHasNoDirectoryEntriesDefinesPackageFromManifest() throws Exception {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "1.2.3");
		ByteArrayOutputStream nestedAppJarData = new ByteArrayOutputStream();
		try (JarOutputStream nestedAppJar = new JarOutputStream(nestedAppJarData, manifest)) {
			try (JarInputStream sourceAppJar = new JarInputStream(
					new FileInputStream("src/test/resources/jars/app.jar"))) {
				ZipEntry entry = sourceAppJar.getNextEntry();
				while (entry != null) {
					if (entry.getName().endsWith(".class")) {
						nestedAppJar.putNextEntry(entry);
						sourceAppJar.transferTo(nestedAppJar);
						nestedAppJar.closeEntry();
					}
					entry = sourceAppJar.getNextEntry();
				}
			}
		}
		File jarFile = createJarWithNestedAppJar(nestedAppJarData.toByteArray());
		URL url = JarUrl.create(jarFile, "app.jar");
		try (JarUrlClassLoader loader = new TestJarUrlClassLoader(url)) {
			Package pkg = loader.loadClass("demo.Application").getPackage();
			assertThat(pkg.getImplementationVersion()).isEqualTo("1.2.3");
		}
	}

	private File createJarWithNestedAppJar(byte[] nestedAppJarData) throws Exception {
		File jarFile = new File(this.tempDir, "test.jar");
		FileOutputStream fileOutputStream = new FileOutputStream(jarFile);
		try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {
			JarEntry nestedEntry = new JarEntry("app.jar");
			nestedEntry.setSize(nestedAppJarData.length);
			nestedEntry.setCompressedSize(nestedAppJarData.length);
			CRC32 crc32 = new CRC32();
			crc32.update(nestedAppJarData);
			nestedEntry.setCrc(crc32.getValue());
			nestedEntry.setMethod(ZipEntry.STORED);
			jarOutputStream.putNextEntry(nestedEntry);
			jarOutputStream.write(nestedAppJarData);
			jarOutputStream.closeEntry();
		}
		return jarFile;
	}

	static class TestJarUrlClassLoader extends JarUrlClassLoader {

		TestJarUrlClassLoader(URL... urls) {
			super(urls, JarUrlClassLoaderTests.class.getClassLoader());
		}

	}

}
