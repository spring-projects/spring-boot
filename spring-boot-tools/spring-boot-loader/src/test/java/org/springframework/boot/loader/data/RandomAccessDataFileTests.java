/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.data;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RandomAccessDataFile}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class RandomAccessDataFileTests {

	private static final byte[] BYTES;

	static {
		BYTES = new byte[256];
		for (int i = 0; i < BYTES.length; i++) {
			BYTES[i] = (byte) i;
		}
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File tempFile;

	private RandomAccessDataFile file;

	private InputStream inputStream;

	@Before
	public void setup() throws Exception {
		this.tempFile = this.temporaryFolder.newFile();
		FileOutputStream outputStream = new FileOutputStream(this.tempFile);
		outputStream.write(BYTES);
		outputStream.close();
		this.file = new RandomAccessDataFile(this.tempFile);
		this.inputStream = this.file.getInputStream();
	}

	@After
	public void cleanup() throws Exception {
		this.inputStream.close();
		this.file.close();
	}

	@Test
	public void fileNotNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("File must not be null");
		new RandomAccessDataFile(null);
	}

	@Test
	public void fileExists() {
		File file = new File("/does/not/exist");
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(String.format("File %s must exist", file.getAbsolutePath()));
		new RandomAccessDataFile(file);
	}

	@Test
	public void readWithOffsetAndLengthShouldRead() throws Exception {
		byte[] read = this.file.read(2, 3);
		assertThat(read).isEqualTo(new byte[] { 2, 3, 4 });
	}

	@Test
	public void readWhenOffsetIsBeyondEOFShouldThrowException() throws Exception {
		this.thrown.expect(IndexOutOfBoundsException.class);
		this.file.read(257, 0);
	}

	@Test
	public void readWhenOffsetIsBeyondEndOfSubsectionShouldThrowException() throws Exception {
		this.thrown.expect(IndexOutOfBoundsException.class);
		RandomAccessData subsection = this.file.getSubsection(0, 10);
		subsection.read(11, 0);
	}

	@Test
	public void readWhenOffsetPlusLengthGreaterThanEOFShouldThrowException() throws Exception {
		this.thrown.expect(EOFException.class);
		this.file.read(256, 1);
	}

	@Test
	public void readWhenOffsetPlusLengthGreaterThanEndOfSubsectionShouldThrowException() throws Exception {
		this.thrown.expect(EOFException.class);
		RandomAccessData subsection = this.file.getSubsection(0, 10);
		subsection.read(10, 1);
	}

	@Test
	public void inputStreamRead() throws Exception {
		for (int i = 0; i <= 255; i++) {
			assertThat(this.inputStream.read()).isEqualTo(i);
		}
	}

	@Test
	public void inputStreamReadNullBytes() throws Exception {
		this.thrown.expect(NullPointerException.class);
		this.thrown.expectMessage("Bytes must not be null");
		this.inputStream.read(null);
	}

	@Test
	public void inputStreamReadNullBytesWithOffset() throws Exception {
		this.thrown.expect(NullPointerException.class);
		this.thrown.expectMessage("Bytes must not be null");
		this.inputStream.read(null, 0, 1);
	}

	@Test
	public void inputStreamReadBytes() throws Exception {
		byte[] b = new byte[256];
		int amountRead = this.inputStream.read(b);
		assertThat(b).isEqualTo(BYTES);
		assertThat(amountRead).isEqualTo(256);
	}

	@Test
	public void inputStreamReadOffsetBytes() throws Exception {
		byte[] b = new byte[7];
		this.inputStream.skip(1);
		int amountRead = this.inputStream.read(b, 2, 3);
		assertThat(b).isEqualTo(new byte[] { 0, 0, 1, 2, 3, 0, 0 });
		assertThat(amountRead).isEqualTo(3);
	}

	@Test
	public void inputStreamReadMoreBytesThanAvailable() throws Exception {
		byte[] b = new byte[257];
		int amountRead = this.inputStream.read(b);
		assertThat(b).startsWith(BYTES);
		assertThat(amountRead).isEqualTo(256);
	}

	@Test
	public void inputStreamReadPastEnd() throws Exception {
		this.inputStream.skip(255);
		assertThat(this.inputStream.read()).isEqualTo(0xFF);
		assertThat(this.inputStream.read()).isEqualTo(-1);
		assertThat(this.inputStream.read()).isEqualTo(-1);
	}

	@Test
	public void inputStreamReadZeroLength() throws Exception {
		byte[] b = new byte[] { 0x0F };
		int amountRead = this.inputStream.read(b, 0, 0);
		assertThat(b).isEqualTo(new byte[] { 0x0F });
		assertThat(amountRead).isEqualTo(0);
		assertThat(this.inputStream.read()).isEqualTo(0);
	}

	@Test
	public void inputStreamSkip() throws Exception {
		long amountSkipped = this.inputStream.skip(4);
		assertThat(this.inputStream.read()).isEqualTo(4);
		assertThat(amountSkipped).isEqualTo(4L);
	}

	@Test
	public void inputStreamSkipMoreThanAvailable() throws Exception {
		long amountSkipped = this.inputStream.skip(257);
		assertThat(this.inputStream.read()).isEqualTo(-1);
		assertThat(amountSkipped).isEqualTo(256L);
	}

	@Test
	public void inputStreamSkipPastEnd() throws Exception {
		this.inputStream.skip(256);
		long amountSkipped = this.inputStream.skip(1);
		assertThat(amountSkipped).isEqualTo(0L);
	}

	@Test
	public void subsectionNegativeOffset() {
		this.thrown.expect(IndexOutOfBoundsException.class);
		this.file.getSubsection(-1, 1);
	}

	@Test
	public void subsectionNegativeLength() {
		this.thrown.expect(IndexOutOfBoundsException.class);
		this.file.getSubsection(0, -1);
	}

	@Test
	public void subsectionZeroLength() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(0, 0);
		assertThat(subsection.getInputStream().read()).isEqualTo(-1);
	}

	@Test
	public void subsectionTooBig() {
		this.file.getSubsection(0, 256);
		this.thrown.expect(IndexOutOfBoundsException.class);
		this.file.getSubsection(0, 257);
	}

	@Test
	public void subsectionTooBigWithOffset() {
		this.file.getSubsection(1, 255);
		this.thrown.expect(IndexOutOfBoundsException.class);
		this.file.getSubsection(1, 256);
	}

	@Test
	public void subsection() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(1, 1);
		assertThat(subsection.getInputStream().read()).isEqualTo(1);
	}

	@Test
	public void inputStreamReadPastSubsection() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(1, 2);
		InputStream inputStream = subsection.getInputStream();
		assertThat(inputStream.read()).isEqualTo(1);
		assertThat(inputStream.read()).isEqualTo(2);
		assertThat(inputStream.read()).isEqualTo(-1);
	}

	@Test
	public void inputStreamReadBytesPastSubsection() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(1, 2);
		InputStream inputStream = subsection.getInputStream();
		byte[] b = new byte[3];
		int amountRead = inputStream.read(b);
		assertThat(b).isEqualTo(new byte[] { 1, 2, 0 });
		assertThat(amountRead).isEqualTo(2);
	}

	@Test
	public void inputStreamSkipPastSubsection() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(1, 2);
		InputStream inputStream = subsection.getInputStream();
		assertThat(inputStream.skip(3)).isEqualTo(2L);
		assertThat(inputStream.read()).isEqualTo(-1);
	}

	@Test
	public void inputStreamSkipNegative() throws Exception {
		assertThat(this.inputStream.skip(-1)).isEqualTo(0L);
	}

	@Test
	public void getFile() {
		assertThat(this.file.getFile()).isEqualTo(this.tempFile);
	}

	@Test
	public void concurrentReads() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(20);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			results.add(executorService.submit(() -> {
				InputStream subsectionInputStream = RandomAccessDataFileTests.this.file.getSubsection(0, 256)
						.getInputStream();
				byte[] b = new byte[256];
				subsectionInputStream.read(b);
				return Arrays.equals(b, BYTES);
			}));
		}
		for (Future<Boolean> future : results) {
			assertThat(future.get()).isTrue();
		}
	}

}
