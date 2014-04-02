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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class FileUtilsTests {

	private File origin;
	private File target;

	@Before
	public void init() {
		this.origin = new File("target/test/remove");
		this.target = new File("target/test/keep");
		FileSystemUtils.deleteRecursively(this.origin);
		FileSystemUtils.deleteRecursively(this.target);
		this.origin.mkdirs();
		this.target.mkdirs();
	}

	@Test
	public void simpleDuplicateFile() throws IOException {
		File file = new File(this.origin, "logback.xml");
		file.createNewFile();
		new File(this.target, "logback.xml").createNewFile();
		FileUtils.removeDuplicatesFromCopy(this.origin, this.target);
		assertFalse(file.exists());
	}

	@Test
	public void nestedDuplicateFile() throws IOException {
		assertTrue(new File(this.origin, "sub").mkdirs());
		assertTrue(new File(this.target, "sub").mkdirs());
		File file = new File(this.origin, "sub/logback.xml");
		file.createNewFile();
		new File(this.target, "sub/logback.xml").createNewFile();
		FileUtils.removeDuplicatesFromCopy(this.origin, this.target);
		assertFalse(file.exists());
	}

	@Test
	public void nestedNonDuplicateFile() throws IOException {
		assertTrue(new File(this.origin, "sub").mkdirs());
		assertTrue(new File(this.target, "sub").mkdirs());
		File file = new File(this.origin, "sub/logback.xml");
		file.createNewFile();
		new File(this.target, "sub/different.xml").createNewFile();
		FileUtils.removeDuplicatesFromCopy(this.origin, this.target);
		assertTrue(file.exists());
	}

	@Test
	public void nonDuplicateFile() throws IOException {
		File file = new File(this.origin, "logback.xml");
		file.createNewFile();
		new File(this.target, "different.xml").createNewFile();
		FileUtils.removeDuplicatesFromCopy(this.origin, this.target);
		assertTrue(file.exists());
	}

}
