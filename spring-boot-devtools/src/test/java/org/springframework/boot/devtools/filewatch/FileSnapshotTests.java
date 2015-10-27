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

package org.springframework.boot.devtools.filewatch;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.util.FileCopyUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link FileSnapshot}.
 *
 * @author Phillip Webb
 */
public class FileSnapshotTests {

	private static final long TWO_MINS = TimeUnit.MINUTES.toMillis(2);

	private static final long MODIFIED = new Date().getTime()
			- TimeUnit.DAYS.toMillis(10);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void fileMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("File must not be null");
		new FileSnapshot(null);
	}

	@Test
	public void fileMustNotBeAFolder() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("File must not be a folder");
		new FileSnapshot(this.temporaryFolder.newFolder());
	}

	@Test
	public void equalsIfTheSame() throws Exception {
		File file = createNewFile("abc", MODIFIED);
		File fileCopy = new File(file, "x").getParentFile();
		FileSnapshot snapshot1 = new FileSnapshot(file);
		FileSnapshot snapshot2 = new FileSnapshot(fileCopy);
		assertThat(snapshot1, equalTo(snapshot2));
		assertThat(snapshot1.hashCode(), equalTo(snapshot2.hashCode()));
	}

	@Test
	public void notEqualsIfDeleted() throws Exception {
		File file = createNewFile("abc", MODIFIED);
		FileSnapshot snapshot1 = new FileSnapshot(file);
		file.delete();
		assertThat(snapshot1, not(equalTo(new FileSnapshot(file))));
	}

	@Test
	public void notEqualsIfLengthChanges() throws Exception {
		File file = createNewFile("abc", MODIFIED);
		FileSnapshot snapshot1 = new FileSnapshot(file);
		setupFile(file, "abcd", MODIFIED);
		assertThat(snapshot1, not(equalTo(new FileSnapshot(file))));
	}

	@Test
	public void notEqualsIfLastModifiedChanges() throws Exception {
		File file = createNewFile("abc", MODIFIED);
		FileSnapshot snapshot1 = new FileSnapshot(file);
		setupFile(file, "abc", MODIFIED + TWO_MINS);
		assertThat(snapshot1, not(equalTo(new FileSnapshot(file))));
	}

	private File createNewFile(String content, long lastModified) throws IOException {
		File file = this.temporaryFolder.newFile();
		setupFile(file, content, lastModified);
		return file;
	}

	private void setupFile(File file, String content, long lastModified)
			throws IOException {
		FileCopyUtils.copy(content.getBytes(), file);
		file.setLastModified(lastModified);
	}

}
