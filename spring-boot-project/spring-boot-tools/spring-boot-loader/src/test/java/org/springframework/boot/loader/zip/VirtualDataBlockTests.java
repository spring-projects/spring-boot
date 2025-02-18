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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VirtualDataBlock}.
 *
 * @author Phillip Webb
 */
class VirtualDataBlockTests {

	private VirtualDataBlock virtualDataBlock;

	@BeforeEach
	void setup() throws IOException {
		List<DataBlock> subsections = new ArrayList<>();
		subsections.add(new ByteArrayDataBlock("abc".getBytes(StandardCharsets.UTF_8)));
		subsections.add(new ByteArrayDataBlock("defg".getBytes(StandardCharsets.UTF_8)));
		subsections.add(new ByteArrayDataBlock("h".getBytes(StandardCharsets.UTF_8)));
		this.virtualDataBlock = new VirtualDataBlock(subsections);
	}

	@Test
	void sizeReturnsSize() throws IOException {
		assertThat(this.virtualDataBlock.size()).isEqualTo(8);
	}

	@Test
	void readFullyReadsAllBlocks() throws IOException {
		ByteBuffer dst = ByteBuffer.allocate((int) this.virtualDataBlock.size());
		this.virtualDataBlock.readFully(dst, 0);
		assertThat(dst.array()).containsExactly("abcdefgh".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void readWithShortBlock() throws IOException {
		ByteBuffer dst = ByteBuffer.allocate(2);
		assertThat(this.virtualDataBlock.read(dst, 1)).isEqualTo(2);
		assertThat(dst.array()).containsExactly("bc".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void readWithShortBlockAcrossSubsections() throws IOException {
		ByteBuffer dst = ByteBuffer.allocate(3);
		assertThat(this.virtualDataBlock.read(dst, 2)).isEqualTo(3);
		assertThat(dst.array()).containsExactly("cde".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void readWithBigBlock() throws IOException {
		ByteBuffer dst = ByteBuffer.allocate(16);
		assertThat(this.virtualDataBlock.read(dst, 1)).isEqualTo(7);
		assertThat(dst.array()).startsWith("bcdefgh".getBytes(StandardCharsets.UTF_8));

	}

}
