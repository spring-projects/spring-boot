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
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link DataBlockInputStream}.
 *
 * @author Phillip Webb
 */
class DataBlockInputStreamTests {

	private ByteArrayDataBlock dataBlock;

	private InputStream inputStream;

	@BeforeEach
	void setup() throws Exception {
		this.dataBlock = new ByteArrayDataBlock(new byte[] { 0, 1, 2 });
		this.inputStream = this.dataBlock.asInputStream();
	}

	@Test
	void readSingleByteReadsByte() throws Exception {
		assertThat(this.inputStream.read()).isEqualTo(0);
		assertThat(this.inputStream.read()).isEqualTo(1);
		assertThat(this.inputStream.read()).isEqualTo(2);
		assertThat(this.inputStream.read()).isEqualTo(-1);
	}

	@Test
	void readByteArrayWhenNotOpenThrowsException() throws Exception {
		byte[] bytes = new byte[10];
		this.inputStream.close();
		assertThatIOException().isThrownBy(() -> this.inputStream.read(bytes)).withMessage("InputStream closed");
	}

	@Test
	void readByteArrayWhenReadingMultipleTimesReadsBytes() throws Exception {
		byte[] bytes = new byte[3];
		assertThat(this.inputStream.read(bytes, 0, 2)).isEqualTo(2);
		assertThat(this.inputStream.read(bytes, 2, 1)).isEqualTo(1);
		assertThat(bytes).containsExactly(0, 1, 2);
	}

	@Test
	void readByteArrayWhenReadingMoreThanAvailableReadsRemainingBytes() throws Exception {
		byte[] bytes = new byte[5];
		assertThat(this.inputStream.read(bytes, 0, 5)).isEqualTo(3);
		assertThat(bytes).containsExactly(0, 1, 2, 0, 0);
	}

	@Test
	void skipSkipsBytes() throws Exception {
		assertThat(this.inputStream.skip(2)).isEqualTo(2);
		assertThat(this.inputStream.read()).isEqualTo(2);
		assertThat(this.inputStream.read()).isEqualTo(-1);
	}

	@Test
	void skipWhenSkippingMoreThanRemainingSkipsBytes() throws Exception {
		assertThat(this.inputStream.skip(100)).isEqualTo(3);
		assertThat(this.inputStream.read()).isEqualTo(-1);
	}

	@Test
	void skipBackwardsSkipsBytes() throws IOException {
		assertThat(this.inputStream.skip(2)).isEqualTo(2);
		assertThat(this.inputStream.skip(-1)).isEqualTo(-1);
		assertThat(this.inputStream.read()).isEqualTo(1);
	}

	@Test
	void skipBackwardsPastBeginningSkipsBytes() throws Exception {
		assertThat(this.inputStream.skip(1)).isEqualTo(1);
		assertThat(this.inputStream.skip(-100)).isEqualTo(-1);
		assertThat(this.inputStream.read()).isEqualTo(0);
	}

	@Test
	void availableReturnsRemainingBytes() throws IOException {
		assertThat(this.inputStream.available()).isEqualTo(3);
		this.inputStream.read();
		assertThat(this.inputStream.available()).isEqualTo(2);
		this.inputStream.skip(1);
		assertThat(this.inputStream.available()).isEqualTo(1);
	}

	@Test
	void availableWhenClosedReturnsZero() throws IOException {
		this.inputStream.close();
		assertThat(this.inputStream.available()).isZero();
	}

	@Test
	void closeClosesDataBlock() throws Exception {
		this.dataBlock = spy(new ByteArrayDataBlock(new byte[] { 0, 1, 2 }));
		this.inputStream = this.dataBlock.asInputStream();
		this.inputStream.close();
		then(this.dataBlock).should().close();
	}

	@Test
	void closeMultipleTimesClosesDataBlockOnce() throws Exception {
		this.dataBlock = spy(new ByteArrayDataBlock(new byte[] { 0, 1, 2 }));
		this.inputStream = this.dataBlock.asInputStream();
		this.inputStream.close();
		this.inputStream.close();
		then(this.dataBlock).should(times(1)).close();
	}

}
