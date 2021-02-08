/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
class FileSystemWatcherTests {

	private FileSystemWatcher watcher;

	private List<Set<ChangedFiles>> changes = Collections.synchronizedList(new ArrayList<>());

	@TempDir
	File tempDir;

	@BeforeEach
	void setup() {
		setupWatcher(20, 10);
	}

	@Test
	void pollIntervalMustBePositive() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileSystemWatcher(true, Duration.ofMillis(0), Duration.ofMillis(1)))
				.withMessageContaining("PollInterval must be positive");
	}

	@Test
	void quietPeriodMustBePositive() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileSystemWatcher(true, Duration.ofMillis(1), Duration.ofMillis(0)))
				.withMessageContaining("QuietPeriod must be positive");
	}

	@Test
	void pollIntervalMustBeGreaterThanQuietPeriod() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileSystemWatcher(true, Duration.ofMillis(1), Duration.ofMillis(1)))
				.withMessageContaining("PollInterval must be greater than QuietPeriod");
	}

	@Test
	void listenerMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.watcher.addListener(null))
				.withMessageContaining("FileChangeListener must not be null");
	}

	@Test
	void cannotAddListenerToStartedListener() {
		this.watcher.start();
		assertThatIllegalStateException().isThrownBy(() -> this.watcher.addListener(mock(FileChangeListener.class)))
				.withMessageContaining("FileSystemWatcher already started");
	}

	@Test
	void sourceDirectoryMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.watcher.addSourceDirectory(null))
				.withMessageContaining("Directory must not be null");
	}

	@Test
	void sourceDirectoryMustNotBeAFile() throws IOException {
		File file = new File(this.tempDir, "file");
		assertThat(file.createNewFile()).isTrue();
		assertThat(file.isFile()).isTrue();
		assertThatIllegalArgumentException().isThrownBy(() -> this.watcher.addSourceDirectory(file))
				.withMessageContaining("Directory '" + file + "' must not be a file");
	}

	@Test
	void cannotAddSourceDirectoryToStartedListener() throws Exception {
		this.watcher.start();
		assertThatIllegalStateException().isThrownBy(() -> this.watcher.addSourceDirectory(this.tempDir))
				.withMessageContaining("FileSystemWatcher already started");
	}

	@Test
	void addFile() throws Exception {
		File directory = startWithNewDirectory();
		File file = touch(new File(directory, "test.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
	}

	@Test
	void addNestedFile() throws Exception {
		File directory = startWithNewDirectory();
		File file = touch(new File(new File(directory, "sub"), "text.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
	}

	@Test
	void createSourceDirectoryAndAddFile() throws IOException {
		File directory = new File(this.tempDir, "does/not/exist");
		assertThat(directory.exists()).isFalse();
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		directory.mkdirs();
		File file = touch(new File(directory, "text.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
	}

	@Test
	void waitsForPollingInterval() throws Exception {
		setupWatcher(10, 1);
		File directory = startWithNewDirectory();
		touch(new File(directory, "test1.txt"));
		while (this.changes.size() != 1) {
			Thread.sleep(10);
		}
		touch(new File(directory, "test2.txt"));
		this.watcher.stopAfter(1);
		assertThat(this.changes.size()).isEqualTo(2);
	}

	@Test
	void waitsForQuietPeriod() throws Exception {
		setupWatcher(300, 200);
		File directory = startWithNewDirectory();
		for (int i = 0; i < 100; i++) {
			touch(new File(directory, i + "test.txt"));
			Thread.sleep(10);
		}
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		assertThat(changedFiles.getFiles()).hasSize(100);
	}

	@Test
	void withExistingFiles() throws Exception {
		File directory = new File(this.tempDir, UUID.randomUUID().toString());
		directory.mkdir();
		touch(new File(directory, "test.txt"));
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		File file = touch(new File(directory, "test2.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
	}

	@Test
	void multipleSources() throws Exception {
		File directory1 = new File(this.tempDir, UUID.randomUUID().toString());
		directory1.mkdir();
		File directory2 = new File(this.tempDir, UUID.randomUUID().toString());
		directory2.mkdir();
		this.watcher.addSourceDirectory(directory1);
		this.watcher.addSourceDirectory(directory2);
		this.watcher.start();
		File file1 = touch(new File(directory1, "test.txt"));
		File file2 = touch(new File(directory2, "test.txt"));
		this.watcher.stopAfter(1);
		Set<ChangedFiles> change = getSingleOnChange();
		assertThat(change.size()).isEqualTo(2);
		for (ChangedFiles changedFiles : change) {
			if (changedFiles.getSourceDirectory().equals(directory1)) {
				ChangedFile file = new ChangedFile(directory1, file1, Type.ADD);
				assertThat(changedFiles.getFiles()).containsOnly(file);
			}
			else {
				ChangedFile file = new ChangedFile(directory2, file2, Type.ADD);
				assertThat(changedFiles.getFiles()).containsOnly(file);
			}
		}
	}

	@Test
	void multipleListeners() throws Exception {
		File directory = new File(this.tempDir, UUID.randomUUID().toString());
		directory.mkdir();
		final Set<ChangedFiles> listener2Changes = new LinkedHashSet<>();
		this.watcher.addSourceDirectory(directory);
		this.watcher.addListener(listener2Changes::addAll);
		this.watcher.start();
		File file = touch(new File(directory, "test.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		assertThat(changedFiles.getFiles()).contains(expected);
		assertThat(listener2Changes).isEqualTo(this.changes.get(0));
	}

	@Test
	void modifyDeleteAndAdd() throws Exception {
		File directory = new File(this.tempDir, UUID.randomUUID().toString());
		directory.mkdir();
		File modify = touch(new File(directory, "modify.txt"));
		File delete = touch(new File(directory, "delete.txt"));
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		FileCopyUtils.copy("abc".getBytes(), modify);
		delete.delete();
		File add = touch(new File(directory, "add.txt"));
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		Set<ChangedFile> actual = changedFiles.getFiles();
		Set<ChangedFile> expected = new HashSet<>();
		expected.add(new ChangedFile(directory, modify, Type.MODIFY));
		expected.add(new ChangedFile(directory, delete, Type.DELETE));
		expected.add(new ChangedFile(directory, add, Type.ADD));
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void withTriggerFilter() throws Exception {
		File directory = new File(this.tempDir, UUID.randomUUID().toString());
		directory.mkdir();
		File file = touch(new File(directory, "file.txt"));
		File trigger = touch(new File(directory, "trigger.txt"));
		this.watcher.addSourceDirectory(directory);
		this.watcher.setTriggerFilter((candidate) -> candidate.getName().equals("trigger.txt"));
		this.watcher.start();
		FileCopyUtils.copy("abc".getBytes(), file);
		Thread.sleep(100);
		assertThat(this.changes).isEmpty();
		FileCopyUtils.copy("abc".getBytes(), trigger);
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		Set<ChangedFile> actual = changedFiles.getFiles();
		Set<ChangedFile> expected = new HashSet<>();
		expected.add(new ChangedFile(directory, file, Type.MODIFY));
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void withSnapshotRepository() throws Exception {
		SnapshotStateRepository repository = new TestSnapshotStateRepository();
		setupWatcher(20, 10, repository);
		File directory = new File(this.tempDir, UUID.randomUUID().toString());
		directory.mkdir();
		File file = touch(new File(directory, "file.txt"));
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		file.delete();
		this.watcher.stopAfter(1);
		this.changes.clear();
		File recreate = touch(new File(directory, "file.txt"));
		setupWatcher(20, 10, repository);
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		this.watcher.stopAfter(1);
		ChangedFiles changedFiles = getSingleChangedFiles();
		Set<ChangedFile> actual = changedFiles.getFiles();
		Set<ChangedFile> expected = new HashSet<>();
		expected.add(new ChangedFile(directory, recreate, Type.ADD));
		assertThat(actual).isEqualTo(expected);
	}

	private void setupWatcher(long pollingInterval, long quietPeriod) {
		setupWatcher(pollingInterval, quietPeriod, null);
	}

	private void setupWatcher(long pollingInterval, long quietPeriod, SnapshotStateRepository snapshotStateRepository) {
		this.watcher = new FileSystemWatcher(false, Duration.ofMillis(pollingInterval), Duration.ofMillis(quietPeriod),
				snapshotStateRepository);
		this.watcher.addListener((changeSet) -> FileSystemWatcherTests.this.changes.add(changeSet));
	}

	private File startWithNewDirectory() throws IOException {
		File directory = new File(this.tempDir, UUID.randomUUID().toString());
		directory.mkdir();
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		return directory;
	}

	private ChangedFiles getSingleChangedFiles() {
		Set<ChangedFiles> singleChange = getSingleOnChange();
		assertThat(singleChange).hasSize(1);
		return singleChange.iterator().next();
	}

	private Set<ChangedFiles> getSingleOnChange() {
		assertThat(this.changes).hasSize(1);
		return this.changes.get(0);
	}

	private File touch(File file) throws IOException {
		file.getParentFile().mkdirs();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.close();
		return file;
	}

	private static class TestSnapshotStateRepository implements SnapshotStateRepository {

		private Object state;

		@Override
		public void save(Object state) {
			this.state = state;
		}

		@Override
		public Object restore() {
			return this.state;
		}

	}

}
