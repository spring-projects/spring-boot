/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PropertiesLauncher}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class PropertiesLauncherTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private ClassLoader contextClassLoader;

	@Before
	public void setup() {
		this.contextClassLoader = Thread.currentThread().getContextClassLoader();
		System.setProperty("loader.home",
				new File("src/test/resources").getAbsolutePath());
	}

	@After
	public void close() {
		Thread.currentThread().setContextClassLoader(this.contextClassLoader);
		System.clearProperty("loader.home");
		System.clearProperty("loader.path");
		System.clearProperty("loader.main");
		System.clearProperty("loader.config.name");
		System.clearProperty("loader.config.location");
		System.clearProperty("loader.system");
		System.clearProperty("loader.classLoader");
	}

	@Test
	public void testDefaultHome() {
		System.clearProperty("loader.home");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getHomeDirectory())
				.isEqualTo(new File(System.getProperty("user.dir")));
	}

	@Test
	public void testAlternateHome() throws Exception {
		System.setProperty("loader.home", "src/test/resources/home");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getHomeDirectory())
				.isEqualTo(new File(System.getProperty("loader.home")));
		assertThat(launcher.getMainClass()).isEqualTo("demo.HomeApplication");
	}

	@Test
	public void testNonExistentHome() {
		System.setProperty("loader.home", "src/test/resources/nonexistent");
		assertThatIllegalStateException().isThrownBy(PropertiesLauncher::new)
				.withMessageContaining("Invalid source folder")
				.withCauseInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testUserSpecifiedMain() throws Exception {
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("demo.Application");
		assertThat(System.getProperty("loader.main")).isNull();
	}

	@Test
	public void testUserSpecifiedConfigName() throws Exception {
		System.setProperty("loader.config.name", "foo");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("my.Application");
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[etc/]");
	}

	@Test
	public void testRootOfClasspathFirst() throws Exception {
		System.setProperty("loader.config.name", "bar");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("my.BarApplication");
	}

	@Test
	public void testUserSpecifiedDotPath() {
		System.setProperty("loader.path", ".");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[.]");
	}

	@Test
	public void testUserSpecifiedSlashPath() throws Exception {
		System.setProperty("loader.path", "jars/");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jars/]");
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).areExactly(1, endingWith("app.jar!/"));
	}

	@Test
	public void testUserSpecifiedWildcardPath() throws Exception {
		System.setProperty("loader.path", "jars/*");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jars/]");
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedJarPath() throws Exception {
		System.setProperty("loader.path", "jars/app.jar");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jars/app.jar]");
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedRootOfJarPath() throws Exception {
		System.setProperty("loader.path",
				"jar:file:./src/test/resources/nested-jars/app.jar!/");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jar:file:./src/test/resources/nested-jars/app.jar!/]");
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).areExactly(1, endingWith("foo.jar!/"));
		assertThat(archives).areExactly(1, endingWith("app.jar!/"));
	}

	@Test
	public void testUserSpecifiedRootOfJarPathWithDot() throws Exception {
		System.setProperty("loader.path", "nested-jars/app.jar!/./");
		PropertiesLauncher launcher = new PropertiesLauncher();
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).areExactly(1, endingWith("foo.jar!/"));
		assertThat(archives).areExactly(1, endingWith("app.jar!/"));
	}

	@Test
	public void testUserSpecifiedRootOfJarPathWithDotAndJarPrefix() throws Exception {
		System.setProperty("loader.path",
				"jar:file:./src/test/resources/nested-jars/app.jar!/./");
		PropertiesLauncher launcher = new PropertiesLauncher();
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).areExactly(1, endingWith("foo.jar!/"));
	}

	@Test
	public void testUserSpecifiedJarFileWithNestedArchives() throws Exception {
		System.setProperty("loader.path", "nested-jars/app.jar");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).areExactly(1, endingWith("foo.jar!/"));
		assertThat(archives).areExactly(1, endingWith("app.jar!/"));
	}

	@Test
	public void testUserSpecifiedNestedJarPath() throws Exception {
		System.setProperty("loader.path", "nested-jars/app.jar!/foo.jar");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives).hasSize(1).areExactly(1, endingWith("foo.jar!/"));
	}

	@Test
	public void testUserSpecifiedDirectoryContainingJarFileWithNestedArchives()
			throws Exception {
		System.setProperty("loader.path", "nested-jars");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedJarPathWithDot() throws Exception {
		System.setProperty("loader.path", "./jars/app.jar");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jars/app.jar]");
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedClassLoader() throws Exception {
		System.setProperty("loader.path", "jars/app.jar");
		System.setProperty("loader.classLoader", URLClassLoader.class.getName());
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jars/app.jar]");
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedClassPathOrder() throws Exception {
		System.setProperty("loader.path", "more-jars/app.jar,jars/app.jar");
		System.setProperty("loader.classLoader", URLClassLoader.class.getName());
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[more-jars/app.jar, jars/app.jar]");
		launcher.launch(new String[0]);
		waitFor("Hello Other World");
	}

	@Test
	public void testCustomClassLoaderCreation() throws Exception {
		System.setProperty("loader.classLoader", TestLoader.class.getName());
		PropertiesLauncher launcher = new PropertiesLauncher();
		ClassLoader loader = launcher.createClassLoader(archives());
		assertThat(loader).isNotNull();
		assertThat(loader.getClass().getName()).isEqualTo(TestLoader.class.getName());
	}

	private List<Archive> archives() throws Exception {
		List<Archive> archives = new ArrayList<>();
		String path = System.getProperty("java.class.path");
		for (String url : path.split(File.pathSeparator)) {
			archives.add(archive(url));
		}
		return archives;
	}

	private Archive archive(String url) throws IOException {
		File file = new FileSystemResource(url).getFile();
		if (url.endsWith(".jar")) {
			return new JarFileArchive(file);
		}
		return new ExplodedArchive(file);
	}

	@Test
	public void testUserSpecifiedConfigPathWins() throws Exception {

		System.setProperty("loader.config.name", "foo");
		System.setProperty("loader.config.location", "classpath:bar.properties");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("my.BarApplication");
	}

	@Test
	public void testSystemPropertySpecifiedMain() throws Exception {
		System.setProperty("loader.main", "foo.Bar");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("foo.Bar");
	}

	@Test
	public void testSystemPropertiesSet() {
		System.setProperty("loader.system", "true");
		new PropertiesLauncher();
		assertThat(System.getProperty("loader.main")).isEqualTo("demo.Application");
	}

	@Test
	public void testArgsEnhanced() throws Exception {
		System.setProperty("loader.args", "foo");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(Arrays.asList(launcher.getArgs("bar")).toString())
				.isEqualTo("[foo, bar]");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLoadPathCustomizedUsingManifest() throws Exception {
		System.setProperty("loader.home",
				this.temporaryFolder.getRoot().getAbsolutePath());
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().putValue("Loader-Path", "/foo.jar, /bar");
		File manifestFile = new File(this.temporaryFolder.getRoot(),
				"META-INF/MANIFEST.MF");
		manifestFile.getParentFile().mkdirs();
		manifest.write(new FileOutputStream(manifestFile));
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat((List<String>) ReflectionTestUtils.getField(launcher, "paths"))
				.containsExactly("/foo.jar", "/bar/");
	}

	@Test
	public void testManifestWithPlaceholders() throws Exception {
		System.setProperty("loader.home", "src/test/resources/placeholders");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("demo.FooApplication");
	}

	@Test
	public void encodedFileUrlLoaderPathIsHandledCorrectly() throws Exception {
		File loaderPath = this.temporaryFolder.newFolder("loader path");
		System.setProperty("loader.path", loaderPath.toURI().toURL().toString());
		PropertiesLauncher launcher = new PropertiesLauncher();
		List<Archive> archives = launcher.getClassPathArchives();
		assertThat(archives.size()).isEqualTo(1);
		File archiveRoot = (File) ReflectionTestUtils.getField(archives.get(0), "root");
		assertThat(archiveRoot).isEqualTo(loaderPath);
	}

	private void waitFor(String value) throws Exception {
		int count = 0;
		boolean timeout = false;
		while (!timeout && count < 100) {
			count++;
			Thread.sleep(50L);
			timeout = this.output.toString().contains(value);
		}
		assertThat(timeout).as("Timed out waiting for (" + value + ")").isTrue();
	}

	private Condition<Archive> endingWith(String value) {
		return new Condition<Archive>() {

			@Override
			public boolean matches(Archive archive) {
				return archive.toString().endsWith(value);
			}

		};
	}

	public static class TestLoader extends URLClassLoader {

		public TestLoader(ClassLoader parent) {
			super(new URL[0], parent);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			return super.findClass(name);
		}

	}

}
