/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.ssl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link FileWatcher}.
 *
 * @author Moritz Halbritter
 */
class FileWatcherTests {

	private FileWatcher fileWatcher;

	@BeforeEach
	void setUp() {
		this.fileWatcher = new FileWatcher(Duration.ofMillis(10));
	}

	@AfterEach
	void tearDown() throws IOException {
		this.fileWatcher.close();
	}

	@Test
	void shouldTriggerOnFileCreation(@TempDir Path tempDir) throws Exception {
		Path newFile = tempDir.resolve("new-file.txt");
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(tempDir), callback);
		Files.createFile(newFile);
		callback.expectChanges();
	}

	@Test
	void shouldTriggerOnFileDeletion(@TempDir Path tempDir) throws Exception {
		Path deletedFile = tempDir.resolve("deleted-file.txt");
		Files.createFile(deletedFile);
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(tempDir), callback);
		Files.delete(deletedFile);
		callback.expectChanges();
	}

	@Test
	void shouldTriggerOnFileModification(@TempDir Path tempDir) throws Exception {
		Path deletedFile = tempDir.resolve("modified-file.txt");
		Files.createFile(deletedFile);
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(tempDir), callback);
		Files.writeString(deletedFile, "Some content");
		callback.expectChanges();
	}

	@Test
	void shouldWatchFile(@TempDir Path tempDir) throws Exception {
		Path watchedFile = tempDir.resolve("watched.txt");
		Files.createFile(watchedFile);
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(watchedFile), callback);
		Files.writeString(watchedFile, "Some content");
		callback.expectChanges();
	}

	@Test
	void shouldIgnoreNotWatchedFiles(@TempDir Path tempDir) throws Exception {
		Path watchedFile = tempDir.resolve("watched.txt");
		Path notWatchedFile = tempDir.resolve("not-watched.txt");
		Files.createFile(watchedFile);
		Files.createFile(notWatchedFile);
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(watchedFile), callback);
		Files.writeString(notWatchedFile, "Some content");
		callback.expectNoChanges();
	}

	@Test
	void shouldFailIfDirectoryOrFileDoesNotExist(@TempDir Path tempDir) {
		Path directory = tempDir.resolve("dir1");
		assertThatExceptionOfType(UncheckedIOException.class)
			.isThrownBy(() -> this.fileWatcher.watch(Set.of(directory), new WaitingCallback()))
			.withMessage("Failed to register paths for watching: [%s]".formatted(directory));
	}

	@Test
	void shouldNotFailIfDirectoryIsRegisteredMultipleTimes(@TempDir Path tempDir) {
		WaitingCallback callback = new WaitingCallback();
		assertThatCode(() -> {
			this.fileWatcher.watch(Set.of(tempDir), callback);
			this.fileWatcher.watch(Set.of(tempDir), callback);
		}).doesNotThrowAnyException();
	}

	@Test
	void shouldNotFailIfStoppedMultipleTimes(@TempDir Path tempDir) {
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(tempDir), callback);
		assertThatCode(() -> {
			this.fileWatcher.close();
			this.fileWatcher.close();
		}).doesNotThrowAnyException();
	}

	@Test
	void testRelativeFiles() throws Exception {
		Path watchedFile = Path.of(UUID.randomUUID() + ".txt");
		Files.createFile(watchedFile);
		try {
			WaitingCallback callback = new WaitingCallback();
			this.fileWatcher.watch(Set.of(watchedFile), callback);
			Files.delete(watchedFile);
			callback.expectChanges();
		}
		finally {
			Files.deleteIfExists(watchedFile);
		}
	}

	@Test
	void testRelativeDirectories() throws Exception {
		Path watchedDirectory = Path.of(UUID.randomUUID() + "/");
		Path file = watchedDirectory.resolve("file.txt");
		Files.createDirectory(watchedDirectory);
		try {
			WaitingCallback callback = new WaitingCallback();
			this.fileWatcher.watch(Set.of(watchedDirectory), callback);
			Files.createFile(file);
			callback.expectChanges();
		}
		finally {
			Files.deleteIfExists(file);
			Files.deleteIfExists(watchedDirectory);
		}
	}

	private static final class WaitingCallback implements Runnable {

		private final CountDownLatch latch = new CountDownLatch(1);

		volatile boolean changed = false;

		@Override
		public void run() {
			this.changed = true;
			this.latch.countDown();
		}

		void expectChanges() throws InterruptedException {
			waitForChanges(true);
			assertThat(this.changed).as("changed").isTrue();
		}

		void expectNoChanges() throws InterruptedException {
			waitForChanges(false);
			assertThat(this.changed).as("changed").isFalse();
		}

		void waitForChanges(boolean fail) throws InterruptedException {
			if (!this.latch.await(5, TimeUnit.SECONDS)) {
				if (fail) {
					fail("Timeout while waiting for changes");
				}
			}
		}

	}

}
