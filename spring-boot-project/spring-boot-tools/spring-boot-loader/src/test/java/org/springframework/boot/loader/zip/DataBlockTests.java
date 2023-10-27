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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link DataBlock}.
 *
 * @author Phillip Webb
 */
class DataBlockTests {

	@Test
	void readFullyReadsAllBytesByCallingReadMultipleTimes() throws IOException {
		DataBlock dataBlock = mock(DataBlock.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
		given(dataBlock.read(any(), anyLong()))
			.will(putBytes(new byte[] { 0, 1 }, new byte[] { 2 }, new byte[] { 3, 4, 5 }));
		ByteBuffer dst = ByteBuffer.allocate(6);
		dataBlock.readFully(dst, 0);
		assertThat(dst.array()).containsExactly(0, 1, 2, 3, 4, 5);
	}

	private Answer<?> putBytes(byte[]... bytes) {
		AtomicInteger count = new AtomicInteger();
		return (invocation) -> {
			int index = count.getAndIncrement();
			invocation.getArgument(0, ByteBuffer.class).put(bytes[index]);
			return bytes.length;
		};
	}

	@Test
	void readFullyWhenReadReturnsNegativeResultThrowsException() throws Exception {
		DataBlock dataBlock = mock(DataBlock.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
		given(dataBlock.read(any(), anyLong())).willReturn(-1);
		ByteBuffer dst = ByteBuffer.allocate(8);
		assertThatExceptionOfType(EOFException.class).isThrownBy(() -> dataBlock.readFully(dst, 0));
	}

	@Test
	void asInputStreamReturnsDataBlockInputStream() throws Exception {
		DataBlock dataBlock = mock(DataBlock.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
		assertThat(dataBlock.asInputStream()).isInstanceOf(DataBlockInputStream.class);
	}

}
