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

package org.springframework.boot.devtools.filewatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.devtools.filewatch.ChangedFile.Type;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FileSystemWatcher}.
 *
 * @author Phillip Webb
 */
public class FileSystemWatcherTests {

	private FileSystemWatcher watcher;

	private List<Set<ChangedFiles>> changes = Collections
			.synchronizedList(new ArrayList<>());

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Before
	public void setup() {
		setupWatcher(20, 10);
	}

	@Test
	public void pollIntervalMustBePositive() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileSystemWatcher(true, Duration.ofMillis(0),
						Duration.ofMillis(1)))
				.withMessageContaining("PollInterval must be positive");
	}

	@Test
	public void quietPeriodMustBePositive() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileSystemWatcher(true, Duration.ofMillis(1),
						Duration.ofMillis(0)))
				.withMessageContaining("QuietPeriod must be positive");
	}

	@Test
	public void pollIntervalMustBeGreaterThanQuietPeriod() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileSystemWatcher(true, Duration.ofMillis(1),
						Duration.ofMillis(1)))
				.withMessageContaining("PollInterval must be greater than QuietPeriod");
	}

	@Test
	public void listenerMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.watcher.addListener(null))
				.withMessageContaining("FileChangeListener must not be null");
	}

	@Test
	public void cannotAddListenerToStartedListener() {
		this.watcher.start();
		assertThatIllegalStateException()
				.isThrownBy(
						() -> this.watcher.addListener(mock(FileChangeListener.class)))
				.withMessageContaining("FileSystemWatcher already started");
	}

	@Test
	public void sourceFolderMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.watcher.addSourceFolder(null))
				.withMessageContaining("Folder must not be null");
	}

	@Test
	public void sourceFolderMustNotBeAFile() {
		File folder = new File("pom.xml");
		assertThat(folder.isFile()).isTrue();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.watcher.addSourceFolder(new File("pom.xml")))
				.withMessageContaining("Folder 'pom.xml' must not be a file");
	}

	@Test
	public void cannotAddSourceFolderToStartedListener() throws Exception {
		this.watcher.start();
		assertThatIllegalStateException()
				.isThrownBy(() -> this.watcher.addSourceFolder(this.temp.newFolder()))
				.withMessageContaining("FileSystemWatcher already started");
	}

	@Test
	public void addFile() throws Exception {
		File folder = startWithNewFolder();
		File file = touch(new File(folder, "test.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(folder, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
	}

	@Test
	public void addNestedFile() throws Exception {
		File folder = startWithNewFolder();
		File file = touch(new File(new File(folder, "sub"), "text.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(folder, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
	}

	@Test
	public void createSourceFolderAndAddFile() throws IOException {
		File folder = new File(this.temp.getRoot(), "does/not/exist");
		assertThat(folder.exists()).isFalse();
		this.watcher.addSourceFolder(folder);
		this.watcher.start();
		folder.mkdirs();
		File file = touch(new File(folder, "text.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(folder, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
	}

	@Test
	public void waitsForPollingInterval() throws Exception {
		setupWatcher(10, 1);
		File folder = startWithNewFolder();
		touch(new File(folder, "test1.txt"));
		while (this.changes.size() != 1) {
			Thread.sleep(10);
		}
		touch(new File(folder, "test2.txt"));
		this.watcher.stopAfter(1);
		assertThat(this.changes.size()).isEqualTo(2);
	}

	@Test
	public void waitsForQuietPeriod() throws Exception {
		setupWatcher(300, 200);
		File folder = startWithNewFolder();
		for (int i = 0; i < 10; i++) {
			touch(new File(folder, i + "test.txt"));
			Thread.sleep(100);
		}
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		assertThat(changedFiles.getFiles().size()).isEqualTo(10);
	}

	@Test
	public void withExistingFiles() throws Exception {
		File folder = this.temp.newFolder();
		touch(new File(folder, "test.txt"));
		this.watcher.addSourceFolder(folder);
		this.watcher.start();
		File file = touch(new File(folder, "test2.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(folder, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
	}

	@Test
	public void multipleSources() throws Exception {
		File folder1 = this.temp.newFolder();
		File folder2 = this.temp.newFolder();
		this.watcher.addSourceFolder(folder1);
		this.watcher.addSourceFolder(folder2);
		this.watcher.start();
		File file1 = touch(new File(folder1, "test.txt"));
		File file2 = touch(new File(folder2, "test.txt"));
		this.watcher.stopAfter(1);
		Set<ChangedFiles> change = getSingleOnChange();
		assertThat(change.size()).isEqualTo(2);
		for (ChangedFiles changedFiles : change) {
			if (changedFiles.getSourceFolder().equals(folder1)) {
				ChangedFile file = new ChangedFile(folder1, file1, Type.ADD);
				assertThat(changedFiles.getFiles()).containsOnly(file);
			}
			else {
				ChangedFile file = new ChangedFile(folder2, file2, Type.ADD);
				assertThat(changedFiles.getFiles()).containsOnly(file);
			}
		}
	}

	@Test
	public void multipleListeners() throws Exception {
		File folder = this.temp.newFolder();
		final Set<ChangedFiles> listener2Changes = new LinkedHashSet<>();
		this.watcher.addSourceFolder(folder);
		this.watcher.addListener(listener2Changes::addAll);
		this.watcher.start();
		File file = touch(new File(folder, "test.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(folder, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
		assertThat(listener2Changes).isEqualTo(this.changes.get(0));
	}

	@Test
	public void modifyDeleteAndAdd() throws Exception {
		File folder = this.temp.newFolder();
		File modify = touch(new File(folder, "modify.txt"));
		File delete = touch(new File(folder, "delete.txt"));
		this.watcher.addSourceFolder(folder);
		this.watcher.start();
		FileCopyUtils.copy("abc".getBytes(), modify);
		delete.delete();
		File add = touch(new File(folder, "add.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		Set<ChangedFile> actual = changedFiles.getFiles();
		Set<ChangedFile> expected = new HashSet<>();
		expected.add(new ChangedFile(folder, modify, Type.MODIFY));
		expected.add(new ChangedFile(folder, delete, Type.DELETE));
		expected.add(new ChangedFile(folder, add, Type.ADD));
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void withTriggerFilter() throws Exception {
		File folder = this.temp.newFolder();
		File file = touch(new File(folder, "file.txt"));
		File trigger = touch(new File(folder, "trigger.txt"));
		this.watcher.addSourceFolder(folder);
		this.watcher.setTriggerFilter(
				(candidate) -> candidate.getName().equals("trigger.txt"));
		this.watcher.start();
		FileCopyUtils.copy("abc".getBytes(), file);
		Thread.sleep(100);
		assertThat(this.changes).isEmpty();
		FileCopyUtils.copy("abc".getBytes(), trigger);
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		Set<ChangedFile> actual = changedFiles.getFiles();
		Set<ChangedFile> expected = new HashSet<>();
		expected.add(new ChangedFile(folder, file, Type.MODIFY));
		assertThat(actual).isEqualTo(expected);
	}

	private void setupWatcher(long pollingInterval, long quietPeriod) {
		this.watcher = new FileSystemWatcher(false, Duration.ofMillis(pollingInterval),
				Duration.ofMillis(quietPeriod));
		this.watcher.addListener(
				(changeSet) -> FileSystemWatcherTests.this.changes.add(changeSet));
	}

	private File startWithNewFolder() throws IOException {
		File folder = this.temp.newFolder();
		this.watcher.addSourceFolder(folder);
		this.watcher.start();
		return folder;
	}

	private ChangedFiles getSingleChangedFiles() {
		Set<ChangedFiles> singleChange = getSingleOnChange();
		assertThat(singleChange.size()).isEqualTo(1);
		return singleChange.iterator().next();
	}

	private Set<ChangedFiles> getSingleOnChange() {
		assertThat(this.changes.size()).isEqualTo(1);
		return this.changes.get(0);
	}

	private File touch(File file) throws IOException {
		file.getParentFile().mkdirs();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.close();
		return file;
	}

}
