/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.restart.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link RestartClassLoader}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class RestartClassLoaderTests {

	private static final String PACKAGE = RestartClassLoaderTests.class.getPackage()
			.getName();

	private static final String PACKAGE_PATH = PACKAGE.replace(".", "/");

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private File sampleJarFile;

	private URLClassLoader parentClassLoader;

	private RestartClassLoader reloadClassLoader;

	@Before
	public void setup() throws Exception {
		this.sampleJarFile = createSampleJarFile();
		URL url = this.sampleJarFile.toURI().toURL();
		ClassLoader classLoader = getClass().getClassLoader();
		URL[] urls = new URL[] { url };
		this.parentClassLoader = new URLClassLoader(urls, classLoader);
		this.reloadClassLoader = new RestartClassLoader(this.parentClassLoader, urls);
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
	public void getResourceFromReloadableUrl() throws Exception {
		String content = readString(this.reloadClassLoader
				.getResourceAsStream(PACKAGE_PATH + "/Sample.txt"));
		assertThat(content, startsWith("fromchild"));
	}

	@Test
	public void getResourceFromParent() throws Exception {
		String content = readString(this.reloadClassLoader
				.getResourceAsStream(PACKAGE_PATH + "/Parent.txt"));
		assertThat(content, startsWith("fromparent"));
	}

	@Test
	public void getResourcesFiltersDuplicates() throws Exception {
		List<URL> resources = toList(this.reloadClassLoader.getResources(PACKAGE_PATH
				+ "/Sample.txt"));
		assertThat(resources.size(), equalTo(1));
	}

	@Test
	public void loadClassFromReloadableUrl() throws Exception {
		Class<?> loaded = this.reloadClassLoader.loadClass(PACKAGE + ".Sample");
		assertThat(loaded.getClassLoader(), equalTo((ClassLoader) this.reloadClassLoader));
	}

	@Test
	public void loadClassFromParent() throws Exception {
		Class<?> loaded = this.reloadClassLoader.loadClass(PACKAGE + ".SampleParent");
		assertThat(loaded.getClassLoader(), equalTo(getClass().getClassLoader()));
	}

	private String readString(InputStream in) throws IOException {
		return new String(FileCopyUtils.copyToByteArray(in));
	}

	private <T> List<T> toList(Enumeration<T> enumeration) {
		List<T> list = new ArrayList<T>();
		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				list.add(enumeration.nextElement());
			}
		}
		return list;
	}

}
