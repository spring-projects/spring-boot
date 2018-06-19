/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.tools;

/**
 * Tests for {@link ZipHeaderPeekInputStream}.
 *
 * @author Andy Wilkinson
 */
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import org.springframework.boot.loader.tools.JarWriter.ZipHeaderPeekInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipHeaderPeekInputStreamTests {

	@Test
	public void hasZipHeaderReturnsTrueWhenStreamStartsWithZipHeader()
			throws IOException {
		try (ZipHeaderPeekInputStream in = new ZipHeaderPeekInputStream(
				new ByteArrayInputStream(new byte[] { 0x50, 0x4b, 0x03, 0x04, 5, 6 }))) {
			assertThat(in.hasZipHeader()).isTrue();
		}
	}

	@Test
	public void hasZipHeaderReturnsFalseWhenStreamDoesNotStartWithZipHeader()
			throws IOException {
		try (ZipHeaderPeekInputStream in = new ZipHeaderPeekInputStream(
				new ByteArrayInputStream(new byte[] { 0, 1, 2, 3, 4, 5 }))) {
			assertThat(in.hasZipHeader()).isFalse();
		}
	}

	@Test
	public void readIndividualBytes() throws IOException {
		try (ZipHeaderPeekInputStream in = new ZipHeaderPeekInputStream(
				new ByteArrayInputStream(new byte[] { 0, 1, 2, 3, 4, 5 }))) {
			assertThat(in.read()).isEqualTo(0);
			assertThat(in.read()).isEqualTo(1);
			assertThat(in.read()).isEqualTo(2);
			assertThat(in.read()).isEqualTo(3);
			assertThat(in.read()).isEqualTo(4);
			assertThat(in.read()).isEqualTo(5);
		}
	}

	@Test
	public void readMultipleBytes() throws IOException {
		try (ZipHeaderPeekInputStream in = new ZipHeaderPeekInputStream(
				new ByteArrayInputStream(new byte[] { 0, 1, 2, 3, 4, 5 }))) {
			byte[] bytes = new byte[3];
			assertThat(in.read(bytes)).isEqualTo(3);
			assertThat(bytes).containsExactly(0, 1, 2);
			assertThat(in.read(bytes)).isEqualTo(3);
			assertThat(bytes).containsExactly(3, 4, 5);
			assertThat(in.read(bytes)).isEqualTo(-1);
		}
	}

	@Test
	public void readingMoreThanEntireStreamReadsToEndOfStream() throws IOException {
		try (ZipHeaderPeekInputStream in = new ZipHeaderPeekInputStream(
				new ByteArrayInputStream(new byte[] { 0, 1, 2, 3, 4, 5 }))) {
			byte[] bytes = new byte[8];
			assertThat(in.read(bytes)).isEqualTo(6);
			assertThat(bytes).containsExactly(0, 1, 2, 3, 4, 5, 0, 0);
			assertThat(in.read(bytes)).isEqualTo(-1);
		}
	}

	@Test
	public void readOfSomeOfTheHeaderThenMoreThanEntireStreamReadsToEndOfStream()
			throws IOException {
		try (ZipHeaderPeekInputStream in = new ZipHeaderPeekInputStream(
				new ByteArrayInputStream(new byte[] { 0, 1, 2, 3, 4, 5 }))) {
			byte[] bytes = new byte[8];
			assertThat(in.read(bytes, 0, 3)).isEqualTo(3);
			assertThat(bytes).containsExactly(0, 1, 2, 0, 0, 0, 0, 0);
			assertThat(in.read(bytes, 3, 5)).isEqualTo(3);
			assertThat(bytes).containsExactly(0, 1, 2, 3, 4, 5, 0, 0);
		}
	}

	@Test
	public void readMoreThanEntireStreamWhenStreamLengthIsLessThanZipHeaderLength()
			throws IOException {
		try (ZipHeaderPeekInputStream in = new ZipHeaderPeekInputStream(
				new ByteArrayInputStream(new byte[] { 10 }))) {
			byte[] bytes = new byte[8];
			assertThat(in.read(bytes)).isEqualTo(1);
			assertThat(bytes).containsExactly(10, 0, 0, 0, 0, 0, 0, 0);
		}
	}

	@Test
	public void readMoreThanEntireStreamWhenStreamLengthIsSameAsHeaderLength()
			throws IOException {
		try (ZipHeaderPeekInputStream in = new ZipHeaderPeekInputStream(
				new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }))) {
			byte[] bytes = new byte[8];
			assertThat(in.read(bytes)).isEqualTo(4);
			assertThat(bytes).containsExactly(1, 2, 3, 4, 0, 0, 0, 0);
		}
	}

	@Test
	public void readMoreThanEntireStreamWhenStreamLengthIsZero() throws IOException {
		try (ZipHeaderPeekInputStream in = new ZipHeaderPeekInputStream(
				new ByteArrayInputStream(new byte[0]))) {
			byte[] bytes = new byte[8];
			assertThat(in.read(bytes)).isEqualTo(-1);
			assertThat(bytes).containsExactly(0, 0, 0, 0, 0, 0, 0, 0);
		}
	}

}
