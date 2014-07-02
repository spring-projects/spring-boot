/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.util.FileSystemUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests fir {@link FileUtils}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class FileUtilsTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File outputDirectory;

	private File originDirectory;

	@Before
	public void init() {
		this.outputDirectory = new File("target/test/remove");
		this.originDirectory = new File("target/test/keep");
		FileSystemUtils.deleteRecursively(this.outputDirectory);
		FileSystemUtils.deleteRecursively(this.originDirectory);
		this.outputDirectory.mkdirs();
		this.originDirectory.mkdirs();
	}

	@Test
	public void simpleDuplicateFile() throws IOException {
		File file = new File(this.outputDirectory, "logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "logback.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory,
				this.originDirectory);
		assertFalse(file.exists());
	}

	@Test
	public void nestedDuplicateFile() throws IOException {
		assertTrue(new File(this.outputDirectory, "sub").mkdirs());
		assertTrue(new File(this.originDirectory, "sub").mkdirs());
		File file = new File(this.outputDirectory, "sub/logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "sub/logback.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory,
				this.originDirectory);
		assertFalse(file.exists());
	}

	@Test
	public void nestedNonDuplicateFile() throws IOException {
		assertTrue(new File(this.outputDirectory, "sub").mkdirs());
		assertTrue(new File(this.originDirectory, "sub").mkdirs());
		File file = new File(this.outputDirectory, "sub/logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "sub/different.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory,
				this.originDirectory);
		assertTrue(file.exists());
	}

	@Test
	public void nonDuplicateFile() throws IOException {
		File file = new File(this.outputDirectory, "logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "different.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory,
				this.originDirectory);
		assertTrue(file.exists());
	}

	@Test
	public void hash() throws Exception {
		File file = this.temporaryFolder.newFile();
		OutputStream outputStream = new FileOutputStream(file);
		try {
			outputStream.write(new byte[] { 1, 2, 3 });
		}
		finally {
			outputStream.close();
		}
		assertThat(FileUtils.sha1Hash(file),
				equalTo("7037807198c22a7d2b0807371d763779a84fdfcf"));
	}

}
