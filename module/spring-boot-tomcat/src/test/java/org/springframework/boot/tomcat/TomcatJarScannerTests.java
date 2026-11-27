/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.tomcat.Jar;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScannerCallback;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatJarScanner}.
 *
 * @author Manthan Sharma
 */
class TomcatJarScannerTests {

	@TempDir
	File tempDir;

	private File jarFile;

	@BeforeEach
	void setUp() throws IOException {
		this.jarFile = new File(this.tempDir, "test.jar");
		createTestJar(this.jarFile, null);
	}

	@Test
	void tomcatEmbeddedContextUsesTomcatJarScannerByDefault() {
		TomcatEmbeddedContext context = new TomcatEmbeddedContext();
		assertThat(context.getJarScanner()).isInstanceOf(TomcatJarScanner.class);
	}

	@Test
	void processWhenNestedUrlProcessesNestedJar() throws Exception {
		TomcatJarScanner scanner = new TestTomcatJarScanner(this.jarFile);
		URL nestedUrl = createNestedUrl("jar:nested:/app.jar/!BOOT-INF/lib/test.jar!/");
		AtomicReference<Jar> scannedJar = new AtomicReference<>();
		AtomicBoolean scannedWebapp = new AtomicBoolean();
		JarScannerCallback callback = new TestJarScannerCallback() {
			@Override
			public void scan(Jar jar, String webappPath, boolean isWebapp) {
				scannedJar.set(jar);
				scannedWebapp.set(isWebapp);
			}
		};
		scanner.process(JarScanType.PLUGGABILITY, callback, nestedUrl, "/test", true, null);
		assertThat(scannedJar.get()).isInstanceOf(TomcatNestedJar.class);
		assertThat(scannedWebapp.get()).isTrue();
	}

	@Test
	void processWhenNestedProtocolUrlProcessesNestedJar() throws Exception {
		TomcatJarScanner scanner = new TestTomcatJarScanner(this.jarFile);
		URL nestedUrl = createNestedUrl("nested:/app.jar/!BOOT-INF/lib/test.jar");
		AtomicReference<Jar> scannedJar = new AtomicReference<>();
		JarScannerCallback callback = new TestJarScannerCallback() {
			@Override
			public void scan(Jar jar, String webappPath, boolean isWebapp) {
				scannedJar.set(jar);
			}
		};
		scanner.process(JarScanType.PLUGGABILITY, callback, nestedUrl, null, false, null);
		assertThat(scannedJar.get()).isInstanceOf(TomcatNestedJar.class);
	}

	@Test
	void processWhenNonNestedUrlDelegatesToSuper() throws Exception {
		TomcatJarScanner scanner = new TomcatJarScanner();
		AtomicBoolean fileScanned = new AtomicBoolean();
		JarScannerCallback callback = new TestJarScannerCallback() {
			@Override
			public void scan(File file, String webappPath, boolean isWebapp) {
				fileScanned.set(true);
			}
		};
		scanner.process(JarScanType.PLUGGABILITY, callback, this.tempDir.toURI().toURL(), null, false, null);
		assertThat(fileScanned.get()).isTrue();
	}

	@Test
	void processManifestAppendsClassPathUrlsForNonWebapp() throws Exception {
		File manifestJar = new File(this.tempDir, "manifest.jar");
		createTestJar(manifestJar, "dep1.jar dep2.jar");
		TomcatJarScanner scanner = new TestTomcatJarScanner(manifestJar);
		URL nestedUrl = createNestedUrl("jar:nested:/app.jar/!BOOT-INF/lib/manifest.jar!/");
		Deque<URL> urlsToProcess = new ArrayDeque<>();
		JarScannerCallback callback = new TestJarScannerCallback();
		scanner.process(JarScanType.PLUGGABILITY, callback, nestedUrl, null, false, urlsToProcess);
		assertThat(urlsToProcess).hasSize(2);
		assertThat(urlsToProcess.pop().toExternalForm()).endsWith("dep1.jar");
		assertThat(urlsToProcess.pop().toExternalForm()).endsWith("dep2.jar");
	}

	@Test
	void processManifestDoesNotAppendClassPathUrlsForWebapp() throws Exception {
		File manifestJar = new File(this.tempDir, "manifest.jar");
		createTestJar(manifestJar, "dep1.jar dep2.jar");
		TomcatJarScanner scanner = new TestTomcatJarScanner(manifestJar);
		URL nestedUrl = createNestedUrl("jar:nested:/app.jar/!BOOT-INF/lib/manifest.jar!/");
		Deque<URL> urlsToProcess = new ArrayDeque<>();
		JarScannerCallback callback = new TestJarScannerCallback();
		scanner.process(JarScanType.PLUGGABILITY, callback, nestedUrl, null, true, urlsToProcess);
		assertThat(urlsToProcess).isEmpty();
	}

	private URL createNestedUrl(String spec) throws MalformedURLException {
		URLStreamHandler handler = new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(URL u) {
				throw new UnsupportedOperationException();
			}
		};
		return new URL(null, spec, handler);
	}

	private void createTestJar(File file, @Nullable String classPath) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		if (classPath != null) {
			manifest.getMainAttributes().putValue("Class-Path", classPath);
		}
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(file), manifest)) {
			JarEntry entry = new JarEntry("entry.txt");
			out.putNextEntry(entry);
			out.write("test".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
	}

	private static class TestTomcatJarScanner extends TomcatJarScanner {

		private final File backingJar;

		TestTomcatJarScanner(File backingJar) {
			this.backingJar = backingJar;
		}

		@Override
		protected Jar createNestedJar(URL url) throws IOException {
			return new TomcatNestedJar(new JarFile(this.backingJar), this.backingJar.toURI().toURL());
		}

	}

	private static class TestJarScannerCallback implements JarScannerCallback {

		@Override
		public void scan(Jar jar, String webappPath, boolean isWebapp) throws IOException {
		}

		@Override
		public void scan(File file, String webappPath, boolean isWebapp) throws IOException {
		}

		@Override
		public void scanWebInfClasses() throws IOException {
		}

	}

}
