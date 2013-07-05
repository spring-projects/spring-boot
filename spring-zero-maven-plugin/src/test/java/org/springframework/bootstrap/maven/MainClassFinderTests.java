/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.bootstrap.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.bootstrap.maven.sample.ClassWithMainMethod;
import org.springframework.bootstrap.maven.sample.ClassWithoutMainMethod;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link MainClassFinder}.
 * 
 * @author Phillip Webb
 */
public class MainClassFinderTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void findMainClass() throws Exception {
		File expected = copyToTemp("b.class", ClassWithMainMethod.class);
		copyToTemp("a.class", ClassWithoutMainMethod.class);
		File actual = MainClassFinder.findMainClassFile(this.temporaryFolder.getRoot());
		assertThat(actual, equalTo(expected));
	}

	@Test
	public void findMainClassInSubFolder() throws Exception {
		File expected = copyToTemp("a/b/c/d.class", ClassWithMainMethod.class);
		copyToTemp("a/b/c/e.class", ClassWithoutMainMethod.class);
		copyToTemp("a/b/f.class", ClassWithoutMainMethod.class);
		File actual = MainClassFinder.findMainClassFile(this.temporaryFolder.getRoot());
		assertThat(actual, equalTo(expected));
	}

	@Test
	public void usesBreadthFirst() throws Exception {
		File expected = copyToTemp("a/b.class", ClassWithMainMethod.class);
		copyToTemp("a/b/c/e.class", ClassWithMainMethod.class);
		File actual = MainClassFinder.findMainClassFile(this.temporaryFolder.getRoot());
		assertThat(actual, equalTo(expected));
	}

	@Test
	public void findsClassName() throws Exception {
		copyToTemp("org/test/MyApp.class", ClassWithMainMethod.class);
		assertThat(MainClassFinder.findMainClass(this.temporaryFolder.getRoot()),
				equalTo("org.test.MyApp"));

	}

	private File copyToTemp(String filename, Class<?> classToCopy) throws IOException {
		String[] paths = filename.split("\\/");
		File file = this.temporaryFolder.getRoot();
		for (String path : paths) {
			file = new File(file, path);
		}
		file.getParentFile().mkdirs();
		InputStream inputStream = getClass().getResourceAsStream(
				"/" + classToCopy.getName().replace(".", "/") + ".class");
		OutputStream outputStream = new FileOutputStream(file);
		try {
			IOUtil.copy(inputStream, outputStream);
		} finally {
			outputStream.close();
		}
		return file;
	}
}
