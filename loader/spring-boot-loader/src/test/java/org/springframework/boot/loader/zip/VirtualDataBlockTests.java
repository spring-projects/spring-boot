/*
 * Copyright 2012-present the original author or authors.
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
