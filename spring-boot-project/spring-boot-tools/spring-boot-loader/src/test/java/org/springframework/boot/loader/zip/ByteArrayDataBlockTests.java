/*
 * Copyright 2012-2024 the original author or authors.
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

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ByteArrayDataBlock}.
 *
 * @author Phillip Webb
 */
class ByteArrayDataBlockTests {

	private final byte[] BYTES = { 0, 1, 2, 3, 4, 5, 6, 7 };

	@Test
	void sizeReturnsByteArrayLength() throws Exception {
		try (ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES)) {
			assertThat(dataBlock.size()).isEqualTo(this.BYTES.length);
		}
	}

	@Test
	void readPutsBytes() throws Exception {
		try (ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES)) {
			ByteBuffer dst = ByteBuffer.allocate(8);
			int result = dataBlock.read(dst, 0);
			assertThat(result).isEqualTo(8);
			assertThat(dst.array()).containsExactly(this.BYTES);
		}
	}

	@Test
	void readWhenLessBytesThanRemainingInBufferPutsBytes() throws Exception {
		try (ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES)) {
			ByteBuffer dst = ByteBuffer.allocate(9);
			int result = dataBlock.read(dst, 0);
			assertThat(result).isEqualTo(8);
			assertThat(dst.array()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 0);
		}
	}

	@Test
	void readWhenLessRemainingInBufferThanLengthPutsBytes() throws Exception {
		try (ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES)) {
			ByteBuffer dst = ByteBuffer.allocate(7);
			int result = dataBlock.read(dst, 0);
			assertThat(result).isEqualTo(7);
			assertThat(dst.array()).containsExactly(0, 1, 2, 3, 4, 5, 6);
		}
	}

	@Test
	void readWhenHasPosOffsetReadsBytes() throws Exception {
		try (ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(this.BYTES)) {
			ByteBuffer dst = ByteBuffer.allocate(3);
			int result = dataBlock.read(dst, 4);
			assertThat(result).isEqualTo(3);
			assertThat(dst.array()).containsExactly(4, 5, 6);
		}
	}

}
