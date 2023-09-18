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

package org.springframework.boot.loader.zip;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.zip.FileChannelDataBlock.Tracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FileChannelDataBlock}.
 *
 * @author Phillip Webb
 */
class FileChannelDataBlockTests {

	private static final byte[] CONTENT = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };

	@TempDir
	File tempDir;

	File tempFile;

	@BeforeEach
	void writeTempFile() throws IOException {
		this.tempFile = new File(this.tempDir, "content");
		Files.write(this.tempFile.toPath(), CONTENT);
	}

	@AfterEach
	void resetTracker() {
		FileChannelDataBlock.tracker = null;
	}

	@Test
	void sizeReturnsFileSize() throws IOException {
		try (FileChannelDataBlock block = createAndOpenBlock()) {
			assertThat(block.size()).isEqualTo(CONTENT.length);
		}
	}

	@Test
	void readReadsFile() throws IOException {
		try (FileChannelDataBlock block = createAndOpenBlock()) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThat(block.read(buffer, 0)).isEqualTo(6);
			assertThat(buffer.array()).containsExactly(CONTENT);
		}
	}

	@Test
	void readDoesNotReadPastEndOfFile() throws IOException {
		try (FileChannelDataBlock block = createAndOpenBlock()) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThat(block.read(buffer, 2)).isEqualTo(4);
			assertThat(buffer.array()).containsExactly(0x02, 0x03, 0x04, 0x05, 0x0, 0x0);
		}
	}

	@Test
	void readWhenPosAtSizeReturnsMinusOne() throws IOException {
		try (FileChannelDataBlock block = createAndOpenBlock()) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThat(block.read(buffer, 6)).isEqualTo(-1);
		}
	}

	@Test
	void readWhenPosOverSizeReturnsMinusOne() throws IOException {
		try (FileChannelDataBlock block = createAndOpenBlock()) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThat(block.read(buffer, 7)).isEqualTo(-1);
		}
	}

	@Test
	void readWhenPosIsNegativeThrowsException() throws IOException {
		try (FileChannelDataBlock block = createAndOpenBlock()) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThatIllegalArgumentException().isThrownBy(() -> block.read(buffer, -1));
		}
	}

	@Test
	void sliceWhenOffsetIsNegativeThrowsException() throws IOException {
		try (FileChannelDataBlock block = createAndOpenBlock()) {
			assertThatIllegalArgumentException().isThrownBy(() -> block.slice(-1, 0))
				.withMessage("Offset must not be negative");
		}
	}

	@Test
	void sliceWhenSizeIsNegativeThrowsException() throws IOException {
		try (FileChannelDataBlock block = createAndOpenBlock()) {
			assertThatIllegalArgumentException().isThrownBy(() -> block.slice(0, -1))
				.withMessage("Size must not be negative and must be within bounds");
		}
	}

	@Test
	void sliceWhenSizeIsOutOfBoundsThrowsException() throws IOException {
		try (FileChannelDataBlock block = createAndOpenBlock()) {
			assertThatIllegalArgumentException().isThrownBy(() -> block.slice(2, 5))
				.withMessage("Size must not be negative and must be within bounds");
		}
	}

	@Test
	void sliceReturnsSlice() throws IOException {
		try (FileChannelDataBlock slice = createAndOpenBlock().slice(1, 4)) {
			assertThat(slice.size()).isEqualTo(4);
			ByteBuffer buffer = ByteBuffer.allocate(4);
			assertThat(slice.read(buffer, 0)).isEqualTo(4);
			assertThat(buffer.array()).containsExactly(0x01, 0x02, 0x03, 0x04);
		}
	}

	@Test
	void openAndCloseHandleReferenceCounting() throws IOException {
		TestTracker tracker = new TestTracker();
		FileChannelDataBlock.tracker = tracker;
		FileChannelDataBlock block = createBlock();
		assertThat(block).extracting("channel.referenceCount").isEqualTo(0);
		tracker.assertOpenCloseCounts(0, 0);
		block.open();
		assertThat(block).extracting("channel.referenceCount").isEqualTo(1);
		tracker.assertOpenCloseCounts(1, 0);
		block.open();
		assertThat(block).extracting("channel.referenceCount").isEqualTo(2);
		tracker.assertOpenCloseCounts(1, 0);
		block.close();
		assertThat(block).extracting("channel.referenceCount").isEqualTo(1);
		tracker.assertOpenCloseCounts(1, 0);
		block.close();
		assertThat(block).extracting("channel.referenceCount").isEqualTo(0);
		tracker.assertOpenCloseCounts(1, 1);
		block.open();
		assertThat(block).extracting("channel.referenceCount").isEqualTo(1);
		tracker.assertOpenCloseCounts(2, 1);
		block.close();
		assertThat(block).extracting("channel.referenceCount").isEqualTo(0);
		tracker.assertOpenCloseCounts(2, 2);
	}

	@Test
	void openAndCloseSliceHandleReferenceCounting() throws IOException {
		TestTracker tracker = new TestTracker();
		FileChannelDataBlock.tracker = tracker;
		FileChannelDataBlock block = createBlock();
		FileChannelDataBlock slice = block.slice(1, 4);
		assertThat(block).extracting("channel.referenceCount").isEqualTo(0);
		tracker.assertOpenCloseCounts(0, 0);
		block.open();
		assertThat(block).extracting("channel.referenceCount").isEqualTo(1);
		tracker.assertOpenCloseCounts(1, 0);
		slice.open();
		assertThat(slice).extracting("channel.referenceCount").isEqualTo(2);
		tracker.assertOpenCloseCounts(1, 0);
		slice.open();
		assertThat(slice).extracting("channel.referenceCount").isEqualTo(3);
		tracker.assertOpenCloseCounts(1, 0);
		slice.close();
		assertThat(slice).extracting("channel.referenceCount").isEqualTo(2);
		tracker.assertOpenCloseCounts(1, 0);
		slice.close();
		assertThat(slice).extracting("channel.referenceCount").isEqualTo(1);
		tracker.assertOpenCloseCounts(1, 0);
		block.close();
		assertThat(block).extracting("channel.referenceCount").isEqualTo(0);
		tracker.assertOpenCloseCounts(1, 1);
		slice.open();
		assertThat(slice).extracting("channel.referenceCount").isEqualTo(1);
		tracker.assertOpenCloseCounts(2, 1);
		slice.close();
		assertThat(slice).extracting("channel.referenceCount").isEqualTo(0);
		tracker.assertOpenCloseCounts(2, 2);
	}

	private FileChannelDataBlock createAndOpenBlock() throws IOException {
		FileChannelDataBlock block = createBlock();
		block.open();
		return block;
	}

	private FileChannelDataBlock createBlock() throws IOException {
		return new FileChannelDataBlock(this.tempFile.toPath());
	}

	static class TestTracker implements Tracker {

		private int openCount;

		private int closeCount;

		@Override
		public void openedFileChannel(Path path, FileChannel fileChannel) {
			this.openCount++;
		}

		@Override
		public void closedFileChannel(Path path, FileChannel fileChannel) {
			this.closeCount++;
		}

		void assertOpenCloseCounts(int expectedOpenCount, int expectedCloseCount) {
			assertThat(this.openCount).as("openCount").isEqualTo(expectedOpenCount);
			assertThat(this.closeCount).as("closeCount").isEqualTo(expectedCloseCount);
		}

	}

}
