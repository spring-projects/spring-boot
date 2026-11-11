/*
 * Copyright 2012-present the original author or authors.
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
