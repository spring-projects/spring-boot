/*
 * Copyright 2012-present the original author or authors.
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
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
 * @author Andy Wilkinson
 */
class FileSystemWatcherTests {

	private FileSystemWatcher watcher;

	private final List<Set<ChangedFiles>> changes = Collections.synchronizedList(new ArrayList<>());

	@TempDir
	@SuppressWarnings("NullAway.Init")
	File tempDir;

	@BeforeEach
	void setup() {
		this.watcher = setupWatcher(20, 10);
	}

	@Test
	void pollIntervalMustBePositive() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new FileSystemWatcher(true, Duration.ofMillis(0), Duration.ofMillis(1)))
			.withMessageContaining("'pollInterval' must be positive");
	}

	@Test
	void quietPeriodMustBePositive() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new FileSystemWatcher(true, Duration.ofMillis(1), Duration.ofMillis(0)))
			.withMessageContaining("'quietPeriod' must be positive");
	}

	@Test
	void pollIntervalMustBeGreaterThanQuietPeriod() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new FileSystemWatcher(true, Duration.ofMillis(1), Duration.ofMillis(1)))
			.withMessageContaining("'pollInterval' must be greater than QuietPeriod");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void listenerMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.watcher.addListener(null))
			.withMessageContaining("'fileChangeListener' must not be null");
	}

	@Test
	void cannotAddListenerToStartedListener() {
		this.watcher.start();
		assertThatIllegalStateException().isThrownBy(() -> this.watcher.addListener(mock(FileChangeListener.class)))
			.withMessageContaining("FileSystemWatcher already started");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void sourceDirectoryMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.watcher.addSourceDirectory(null))
			.withMessageContaining("'directory' must not be null");
	}

	@Test
	void sourceDirectoryMustNotBeAFile() throws IOException {
		File file = new File(this.tempDir, "file");
		assertThat(file.createNewFile()).isTrue();
		assertThat(file).isFile();
		assertThatIllegalArgumentException().isThrownBy(() -> this.watcher.addSourceDirectory(file))
			.withMessageContaining("'directory' [" + file + "] must not be a file");
	}

	@Test
	void cannotAddSourceDirectoryToStartedListener() {
		this.watcher.start();
		assertThatIllegalStateException().isThrownBy(() -> this.watcher.addSourceDirectory(this.tempDir))
			.withMessageContaining("FileSystemWatcher already started");
	}

	@Test
	void addFile() throws Exception {
		File directory = startWithNewDirectory();
		File file = touch(new File(directory, "test.txt"));
		this.watcher.stopAfter(1);
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		assertThat(getAllFileChanges()).containsExactly(expected);
	}

	@Test
	void addNestedFile() throws Exception {
		File directory = startWithNewDirectory();
		File file = touch(new File(new File(directory, "sub"), "text.txt"));
		this.watcher.stopAfter(1);
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		assertThat(getAllFileChanges()).containsExactly(expected);
	}

	@Test
	void createSourceDirectoryAndAddFile() throws IOException {
		File directory = new File(this.tempDir, "does/not/exist");
		assertThat(directory).doesNotExist();
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		directory.mkdirs();
		File file = touch(new File(directory, "text.txt"));
		this.watcher.stopAfter(1);
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		assertThat(getAllFileChanges()).containsExactly(expected);
	}

	@Test
	void waitsForPollingInterval() throws Exception {
		this.watcher = setupWatcher(10, 1);
		File directory = startWithNewDirectory();
		touch(new File(directory, "test1.txt"));
		while (this.changes.size() != 1) {
			Thread.sleep(10);
		}
		touch(new File(directory, "test2.txt"));
		this.watcher.stopAfter(1);
		assertThat(this.changes).hasSize(2);
	}

	@Test
	void waitsForQuietPeriod() throws Exception {
		this.watcher = setupWatcher(300, 200);
		File directory = startWithNewDirectory();
		for (int i = 0; i < 100; i++) {
			touch(new File(directory, i + "test.txt"));
			Thread.sleep(10);
		}
		this.watcher.stopAfter(1);
		assertThat(getAllFileChanges()).hasSize(100);
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
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		assertThat(getAllFileChanges()).contains(expected);
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
		Set<ChangedFiles> change = this.changes.stream().flatMap(Set<ChangedFiles>::stream).collect(Collectors.toSet());
		assertThat(change).hasSize(2);
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
		final List<Set<ChangedFiles>> listener2Changes = new ArrayList<>();
		this.watcher.addSourceDirectory(directory);
		this.watcher.addListener(listener2Changes::add);
		this.watcher.start();
		File file = touch(new File(directory, "test.txt"));
		this.watcher.stopAfter(1);
		ChangedFile expected = new ChangedFile(directory, file, Type.ADD);
		Set<ChangedFile> changeSet = getAllFileChanges();
		assertThat(changeSet).contains(expected);
		assertThat(getAllFileChanges(listener2Changes)).isEqualTo(changeSet);
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
		Set<ChangedFile> actual = getAllFileChanges();
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
		Set<ChangedFile> actual = getAllFileChanges();
		Set<ChangedFile> expected = new HashSet<>();
		expected.add(new ChangedFile(directory, file, Type.MODIFY));
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void withSnapshotRepository() throws Exception {
		SnapshotStateRepository repository = new TestSnapshotStateRepository();
		this.watcher = setupWatcher(20, 10, repository);
		File directory = new File(this.tempDir, UUID.randomUUID().toString());
		directory.mkdir();
		File file = touch(new File(directory, "file.txt"));
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		file.delete();
		this.watcher.stopAfter(1);
		this.changes.clear();
		File recreate = touch(new File(directory, "file.txt"));
		this.watcher = setupWatcher(20, 10, repository);
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		this.watcher.stopAfter(1);
		Set<ChangedFile> actual = getAllFileChanges();
		Set<ChangedFile> expected = new HashSet<>();
		expected.add(new ChangedFile(directory, recreate, Type.ADD));
		assertThat(actual).isEqualTo(expected);
	}

	private FileSystemWatcher setupWatcher(long pollingInterval, long quietPeriod) {
		return setupWatcher(pollingInterval, quietPeriod, null);
	}

	private FileSystemWatcher setupWatcher(long pollingInterval, long quietPeriod,
			@Nullable SnapshotStateRepository snapshotStateRepository) {
		FileSystemWatcher watcher = new FileSystemWatcher(false, Duration.ofMillis(pollingInterval),
				Duration.ofMillis(quietPeriod), snapshotStateRepository);
		watcher.addListener(FileSystemWatcherTests.this.changes::add);
		return watcher;
	}

	private File startWithNewDirectory() {
		File directory = new File(this.tempDir, UUID.randomUUID().toString());
		directory.mkdir();
		this.watcher.addSourceDirectory(directory);
		this.watcher.start();
		return directory;
	}

	private Set<ChangedFile> getAllFileChanges() {
		return getAllFileChanges(this.changes);
	}

	private Set<ChangedFile> getAllFileChanges(List<Set<ChangedFiles>> changes) {
		return changes.stream()
			.flatMap(Set<ChangedFiles>::stream)
			.flatMap((changedFiles) -> changedFiles.getFiles().stream())
			.collect(Collectors.toSet());
	}

	private File touch(File file) throws IOException {
		file.getParentFile().mkdirs();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.close();
		return file;
	}

	private static final class TestSnapshotStateRepository implements SnapshotStateRepository {

		private @Nullable Object state;

		@Override
		public void save(Object state) {
			this.state = state;
		}

		@Override
		public @Nullable Object restore() {
			return this.state;
		}

	}

}
