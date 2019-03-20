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

package org.springframework.boot.devtools.restart.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RestartClassLoader}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class RestartClassLoaderTests {

	private static final String PACKAGE = RestartClassLoaderTests.class.getPackage()
			.getName();

	private static final String PACKAGE_PATH = PACKAGE.replace('.', '/');

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private File sampleJarFile;

	private URLClassLoader parentClassLoader;

	private ClassLoaderFiles updatedFiles;

	private RestartClassLoader reloadClassLoader;

	@Before
	public void setup() throws Exception {
		this.sampleJarFile = createSampleJarFile();
		URL url = this.sampleJarFile.toURI().toURL();
		ClassLoader classLoader = getClass().getClassLoader();
		URL[] urls = new URL[] { url };
		this.parentClassLoader = new URLClassLoader(urls, classLoader);
		this.updatedFiles = new ClassLoaderFiles();
		this.reloadClassLoader = new RestartClassLoader(this.parentClassLoader, urls,
				this.updatedFiles);
	}

	private File createSampleJarFile() throws IOException {
		File file = this.temp.newFile("sample.jar");
		JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file));
		jarOutputStream.putNextEntry(new ZipEntry(PACKAGE_PATH + "/Sample.class"));
		StreamUtils.copy(getClass().getResourceAsStream("Sample.class"), jarOutputStream);
		jarOutputStream.closeEntry();
		jarOutputStream.putNextEntry(new ZipEntry(PACKAGE_PATH + "/Sample.txt"));
		StreamUtils.copy("fromchild", UTF_8, jarOutputStream);
		jarOutputStream.closeEntry();
		jarOutputStream.close();
		return file;
	}

	@Test
	public void parentMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Parent must not be null");
		new RestartClassLoader(null, new URL[] {});
	}

	@Test
	public void updatedFilesMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("UpdatedFiles must not be null");
		new RestartClassLoader(this.parentClassLoader, new URL[] {}, null);
	}

	@Test
	public void getResourceFromReloadableUrl() throws Exception {
		String content = readString(
				this.reloadClassLoader.getResourceAsStream(PACKAGE_PATH + "/Sample.txt"));
		assertThat(content).startsWith("fromchild");
	}

	@Test
	public void getResourceFromParent() throws Exception {
		String content = readString(
				this.reloadClassLoader.getResourceAsStream(PACKAGE_PATH + "/Parent.txt"));
		assertThat(content).startsWith("fromparent");
	}

	@Test
	public void getResourcesFiltersDuplicates() throws Exception {
		List<URL> resources = toList(
				this.reloadClassLoader.getResources(PACKAGE_PATH + "/Sample.txt"));
		assertThat(resources.size()).isEqualTo(1);
	}

	@Test
	public void loadClassFromReloadableUrl() throws Exception {
		Class<?> loaded = this.reloadClassLoader.loadClass(PACKAGE + ".Sample");
		assertThat(loaded.getClassLoader()).isEqualTo(this.reloadClassLoader);
	}

	@Test
	public void loadClassFromParent() throws Exception {
		Class<?> loaded = this.reloadClassLoader.loadClass(PACKAGE + ".SampleParent");
		assertThat(loaded.getClassLoader()).isEqualTo(getClass().getClassLoader());
	}

	@Test
	public void getDeletedResource() throws Exception {
		String name = PACKAGE_PATH + "/Sample.txt";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.DELETED, null));
		assertThat(this.reloadClassLoader.getResource(name)).isEqualTo(null);
	}

	@Test
	public void getDeletedResourceAsStream() throws Exception {
		String name = PACKAGE_PATH + "/Sample.txt";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.DELETED, null));
		assertThat(this.reloadClassLoader.getResourceAsStream(name)).isEqualTo(null);
	}

	@Test
	public void getUpdatedResource() throws Exception {
		String name = PACKAGE_PATH + "/Sample.txt";
		byte[] bytes = "abc".getBytes();
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.MODIFIED, bytes));
		URL resource = this.reloadClassLoader.getResource(name);
		assertThat(FileCopyUtils.copyToByteArray(resource.openStream())).isEqualTo(bytes);
	}

	@Test
	public void getResourcesWithDeleted() throws Exception {
		String name = PACKAGE_PATH + "/Sample.txt";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.DELETED, null));
		List<URL> resources = toList(this.reloadClassLoader.getResources(name));
		assertThat(resources.size()).isEqualTo(0);
	}

	@Test
	public void getResourcesWithUpdated() throws Exception {
		String name = PACKAGE_PATH + "/Sample.txt";
		byte[] bytes = "abc".getBytes();
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.MODIFIED, bytes));
		List<URL> resources = toList(this.reloadClassLoader.getResources(name));
		assertThat(FileCopyUtils.copyToByteArray(resources.get(0).openStream()))
				.isEqualTo(bytes);
	}

	@Test
	public void getDeletedClass() throws Exception {
		String name = PACKAGE_PATH + "/Sample.class";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.DELETED, null));
		this.thrown.expect(ClassNotFoundException.class);
		this.reloadClassLoader.loadClass(PACKAGE + ".Sample");
	}

	@Test
	public void getUpdatedClass() throws Exception {
		String name = PACKAGE_PATH + "/Sample.class";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.MODIFIED, new byte[10]));
		this.thrown.expect(ClassFormatError.class);
		this.reloadClassLoader.loadClass(PACKAGE + ".Sample");
	}

	@Test
	public void getAddedClass() throws Exception {
		String name = PACKAGE_PATH + "/SampleParent.class";
		byte[] bytes = FileCopyUtils
				.copyToByteArray(getClass().getResourceAsStream("SampleParent.class"));
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.ADDED, bytes));
		Class<?> loaded = this.reloadClassLoader.loadClass(PACKAGE + ".SampleParent");
		assertThat(loaded.getClassLoader()).isEqualTo(this.reloadClassLoader);
	}

	private String readString(InputStream in) throws IOException {
		return new String(FileCopyUtils.copyToByteArray(in));
	}

	private <T> List<T> toList(Enumeration<T> enumeration) {
		return (enumeration != null) ? Collections.list(enumeration)
				: Collections.<T>emptyList();
	}

}
