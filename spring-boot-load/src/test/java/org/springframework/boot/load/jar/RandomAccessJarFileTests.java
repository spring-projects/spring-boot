/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.boot.load.jar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.load.TestJarCreator;
import org.springframework.boot.load.data.RandomAccessDataFile;
import org.springframework.boot.load.jar.JarEntryFilter;
import org.springframework.boot.load.jar.RandomAccessJarFile;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RandomAccessJarFile}.
 * 
 * @author Phillip Webb
 */
public class RandomAccessJarFileTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File rootJarFile;

	private RandomAccessJarFile jarFile;

	@Before
	public void setup() throws Exception {
		this.rootJarFile = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(this.rootJarFile);
		this.jarFile = new RandomAccessJarFile(this.rootJarFile);
	}

	@Test
	public void createFromFile() throws Exception {
		RandomAccessJarFile jarFile = new RandomAccessJarFile(this.rootJarFile);
		assertThat(jarFile.getName(), notNullValue(String.class));
	}

	@Test
	public void createFromRandomAccessDataFile() throws Exception {
		RandomAccessDataFile randomAccessDataFile = new RandomAccessDataFile(
				this.rootJarFile, 1);
		RandomAccessJarFile jarFile = new RandomAccessJarFile(randomAccessDataFile);
		assertThat(jarFile.getName(), notNullValue(String.class));
	}

	@Test
	public void getManifest() throws Exception {
		assertThat(this.jarFile.getManifest().getMainAttributes().getValue("Built-By"),
				equalTo("j1"));
	}

	@Test
	public void getEntries() throws Exception {
		Enumeration<JarEntry> entries = this.jarFile.entries();
		assertThat(entries.nextElement().getName(), equalTo("META-INF/"));
		assertThat(entries.nextElement().getName(), equalTo("META-INF/MANIFEST.MF"));
		assertThat(entries.nextElement().getName(), equalTo("1.dat"));
		assertThat(entries.nextElement().getName(), equalTo("2.dat"));
		assertThat(entries.nextElement().getName(), equalTo("d/"));
		assertThat(entries.nextElement().getName(), equalTo("d/9.dat"));
		assertThat(entries.nextElement().getName(), equalTo("nested.jar"));
		assertThat(entries.hasMoreElements(), equalTo(false));
	}

	@Test
	public void getJarEntry() throws Exception {
		JarEntry entry = this.jarFile.getJarEntry("1.dat");
		assertThat(entry, notNullValue(ZipEntry.class));
		assertThat(entry.getName(), equalTo("1.dat"));
	}

	@Test
	public void getInputStream() throws Exception {
		InputStream inputStream = this.jarFile.getInputStream(this.jarFile
				.getEntry("1.dat"));
		assertThat(inputStream.read(), equalTo(1));
		assertThat(inputStream.read(), equalTo(-1));
	}

	@Test
	public void getName() throws Exception {
		assertThat(this.jarFile.getName(), equalTo(this.rootJarFile.getPath()));
	}

	@Test
	public void getSize() throws Exception {
		assertThat(this.jarFile.size(), equalTo((int) this.rootJarFile.length()));
	}

	@Test
	public void close() throws Exception {
		RandomAccessDataFile randomAccessDataFile = spy(new RandomAccessDataFile(
				this.rootJarFile, 1));
		RandomAccessJarFile jarFile = new RandomAccessJarFile(randomAccessDataFile);
		jarFile.close();
		verify(randomAccessDataFile).close();
	}

	@Test
	public void getUrl() throws Exception {
		URL url = this.jarFile.getUrl();
		assertThat(url.toString(), equalTo("jar:file:" + this.rootJarFile.getPath()
				+ "!/"));
		JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
		assertThat(jarURLConnection.getJarFile(), sameInstance((JarFile) this.jarFile));
		assertThat(jarURLConnection.getJarEntry(), nullValue());
		assertThat(jarURLConnection.getContentLength(), greaterThan(1));
		assertThat(jarURLConnection.getContent(), sameInstance((Object) this.jarFile));
		assertThat(jarURLConnection.getContentType(), equalTo("x-java/jar"));
	}

	@Test
	public void getEntryUrl() throws Exception {
		URL url = new URL(this.jarFile.getUrl(), "1.dat");
		assertThat(url.toString(), equalTo("jar:file:" + this.rootJarFile.getPath()
				+ "!/1.dat"));
		JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
		assertThat(jarURLConnection.getJarFile(), sameInstance((JarFile) this.jarFile));
		assertThat(jarURLConnection.getJarEntry(),
				sameInstance(this.jarFile.getJarEntry("1.dat")));
		assertThat(jarURLConnection.getContentLength(), equalTo(1));
		assertThat(jarURLConnection.getContent(), instanceOf(InputStream.class));
		assertThat(jarURLConnection.getContentType(), equalTo("content/unknown"));
	}

	@Test
	public void getMissingEntryUrl() throws Exception {
		URL url = new URL(this.jarFile.getUrl(), "missing.dat");
		assertThat(url.toString(), equalTo("jar:file:" + this.rootJarFile.getPath()
				+ "!/missing.dat"));
		this.thrown.expect(FileNotFoundException.class);
		((JarURLConnection) url.openConnection()).getJarEntry();
	}

	@Test
	public void getUrlStream() throws Exception {
		URL url = this.jarFile.getUrl();
		url.openConnection();
		this.thrown.expect(IOException.class);
		url.openStream();
	}

	@Test
	public void getEntryUrlStream() throws Exception {
		URL url = new URL(this.jarFile.getUrl(), "1.dat");
		url.openConnection();
		InputStream stream = url.openStream();
		assertThat(stream.read(), equalTo(1));
		assertThat(stream.read(), equalTo(-1));
	}

	@Test
	public void getNestedJarFile() throws Exception {
		RandomAccessJarFile nestedJarFile = this.jarFile.getNestedJarFile(this.jarFile
				.getEntry("nested.jar"));

		Enumeration<JarEntry> entries = nestedJarFile.entries();
		assertThat(entries.nextElement().getName(), equalTo("META-INF/"));
		assertThat(entries.nextElement().getName(), equalTo("META-INF/MANIFEST.MF"));
		assertThat(entries.nextElement().getName(), equalTo("3.dat"));
		assertThat(entries.nextElement().getName(), equalTo("4.dat"));
		assertThat(entries.hasMoreElements(), equalTo(false));

		InputStream inputStream = nestedJarFile.getInputStream(nestedJarFile
				.getEntry("3.dat"));
		assertThat(inputStream.read(), equalTo(3));
		assertThat(inputStream.read(), equalTo(-1));

		URL url = nestedJarFile.getUrl();
		assertThat(url.toString(), equalTo("jar:file:" + this.rootJarFile.getPath()
				+ "!/nested.jar!/"));
		assertThat(((JarURLConnection) url.openConnection()).getJarFile(),
				sameInstance((JarFile) nestedJarFile));
	}

	@Test
	public void getNestedJarDirectory() throws Exception {
		RandomAccessJarFile nestedJarFile = this.jarFile.getNestedJarFile(this.jarFile
				.getEntry("d/"));

		Enumeration<JarEntry> entries = nestedJarFile.entries();
		assertThat(entries.nextElement().getName(), equalTo("9.dat"));
		assertThat(entries.hasMoreElements(), equalTo(false));

		InputStream inputStream = nestedJarFile.getInputStream(nestedJarFile
				.getEntry("9.dat"));
		assertThat(inputStream.read(), equalTo(9));
		assertThat(inputStream.read(), equalTo(-1));

		URL url = nestedJarFile.getUrl();
		assertThat(url.toString(), equalTo("jar:file:" + this.rootJarFile.getPath()
				+ "!/d!/"));
		assertThat(((JarURLConnection) url.openConnection()).getJarFile(),
				sameInstance((JarFile) nestedJarFile));
	}

	@Test
	public void getDirectoryInputStream() throws Exception {
		InputStream inputStream = this.jarFile
				.getInputStream(this.jarFile.getEntry("d/"));
		assertThat(inputStream, notNullValue());
		assertThat(inputStream.read(), equalTo(-1));
	}

	@Test
	public void getDirectoryInputStreamWithoutSlash() throws Exception {
		InputStream inputStream = this.jarFile.getInputStream(this.jarFile.getEntry("d"));
		assertThat(inputStream, notNullValue());
		assertThat(inputStream.read(), equalTo(-1));
	}

	@Test
	public void getFilteredJarFile() throws Exception {
		RandomAccessJarFile filteredJarFile = this.jarFile
				.getFilteredJarFile(new JarEntryFilter() {
					@Override
					public String apply(String entryName, JarEntry entry) {
						if (entryName.equals("1.dat")) {
							return "x.dat";
						}
						return null;
					}
				});
		Enumeration<JarEntry> entries = filteredJarFile.entries();
		assertThat(entries.nextElement().getName(), equalTo("x.dat"));
		assertThat(entries.hasMoreElements(), equalTo(false));

		InputStream inputStream = filteredJarFile.getInputStream(filteredJarFile
				.getEntry("x.dat"));
		assertThat(inputStream.read(), equalTo(1));
		assertThat(inputStream.read(), equalTo(-1));
	}

	@Test
	public void sensibleToString() throws Exception {
		assertThat(this.jarFile.toString(), equalTo(this.rootJarFile.getPath()));
		assertThat(this.jarFile.getNestedJarFile(this.jarFile.getEntry("nested.jar"))
				.toString(), equalTo(this.rootJarFile.getPath() + "!/nested.jar"));
	}
}
