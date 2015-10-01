/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.loader.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.loader.ByteArrayStartsWith;
import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link RandomAccessDataFile}.
 *
 * @author Phillip Webb
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
		this.inputStream = this.file.getInputStream(ResourceAccess.PER_READ);
	}

	@After
	public void cleanup() throws Exception {
		this.inputStream.close();
		this.file.close();
	}

	@Test
	public void fileNotNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.equals("File must not be null");
		new RandomAccessDataFile(null);
	}

	@Test
	public void fileExists() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.equals("File must exist");
		new RandomAccessDataFile(new File("/does/not/exist"));
	}

	@Test
	public void fileNotNullWithConcurrentReads() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.equals("File must not be null");
		new RandomAccessDataFile(null, 1);
	}

	@Test
	public void fileExistsWithConcurrentReads() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.equals("File must exist");
		new RandomAccessDataFile(new File("/does/not/exist"), 1);
	}

	@Test
	public void inputStreamRead() throws Exception {
		for (int i = 0; i <= 255; i++) {
			assertThat(this.inputStream.read(), equalTo(i));
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
		assertThat(b, equalTo(BYTES));
		assertThat(amountRead, equalTo(256));
	}

	@Test
	public void inputSteamReadOffsetBytes() throws Exception {
		byte[] b = new byte[7];
		this.inputStream.skip(1);
		int amountRead = this.inputStream.read(b, 2, 3);
		assertThat(b, equalTo(new byte[] { 0, 0, 1, 2, 3, 0, 0 }));
		assertThat(amountRead, equalTo(3));
	}

	@Test
	public void inputStreamReadMoreBytesThanAvailable() throws Exception {
		byte[] b = new byte[257];
		int amountRead = this.inputStream.read(b);
		assertThat(b, startsWith(BYTES));
		assertThat(amountRead, equalTo(256));
	}

	@Test
	public void inputStreamReadPastEnd() throws Exception {
		this.inputStream.skip(255);
		assertThat(this.inputStream.read(), equalTo(0xFF));
		assertThat(this.inputStream.read(), equalTo(-1));
		assertThat(this.inputStream.read(), equalTo(-1));
	}

	@Test
	public void inputStreamReadZeroLength() throws Exception {
		byte[] b = new byte[] { 0x0F };
		int amountRead = this.inputStream.read(b, 0, 0);
		assertThat(b, equalTo(new byte[] { 0x0F }));
		assertThat(amountRead, equalTo(0));
		assertThat(this.inputStream.read(), equalTo(0));
	}

	@Test
	public void inputStreamSkip() throws Exception {
		long amountSkipped = this.inputStream.skip(4);
		assertThat(this.inputStream.read(), equalTo(4));
		assertThat(amountSkipped, equalTo(4L));
	}

	@Test
	public void inputStreamSkipMoreThanAvailable() throws Exception {
		long amountSkipped = this.inputStream.skip(257);
		assertThat(this.inputStream.read(), equalTo(-1));
		assertThat(amountSkipped, equalTo(256L));
	}

	@Test
	public void inputStreamSkipPastEnd() throws Exception {
		this.inputStream.skip(256);
		long amountSkipped = this.inputStream.skip(1);
		assertThat(amountSkipped, equalTo(0L));
	}

	@Test
	public void subsectionNegativeOffset() throws Exception {
		this.thrown.expect(IndexOutOfBoundsException.class);
		this.file.getSubsection(-1, 1);
	}

	@Test
	public void subsectionNegativeLength() throws Exception {
		this.thrown.expect(IndexOutOfBoundsException.class);
		this.file.getSubsection(0, -1);
	}

	@Test
	public void subsectionZeroLength() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(0, 0);
		assertThat(subsection.getInputStream(ResourceAccess.PER_READ).read(), equalTo(-1));
	}

	@Test
	public void subsectionTooBig() throws Exception {
		this.file.getSubsection(0, 256);
		this.thrown.expect(IndexOutOfBoundsException.class);
		this.file.getSubsection(0, 257);
	}

	@Test
	public void subsectionTooBigWithOffset() throws Exception {
		this.file.getSubsection(1, 255);
		this.thrown.expect(IndexOutOfBoundsException.class);
		this.file.getSubsection(1, 256);
	}

	@Test
	public void subsection() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(1, 1);
		assertThat(subsection.getInputStream(ResourceAccess.PER_READ).read(), equalTo(1));
	}

	@Test
	public void inputStreamReadPastSubsection() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(1, 2);
		InputStream inputStream = subsection.getInputStream(ResourceAccess.PER_READ);
		assertThat(inputStream.read(), equalTo(1));
		assertThat(inputStream.read(), equalTo(2));
		assertThat(inputStream.read(), equalTo(-1));
	}

	@Test
	public void inputStreamReadBytesPastSubsection() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(1, 2);
		InputStream inputStream = subsection.getInputStream(ResourceAccess.PER_READ);
		byte[] b = new byte[3];
		int amountRead = inputStream.read(b);
		assertThat(b, equalTo(new byte[] { 1, 2, 0 }));
		assertThat(amountRead, equalTo(2));
	}

	@Test
	public void inputStreamSkipPastSubsection() throws Exception {
		RandomAccessData subsection = this.file.getSubsection(1, 2);
		InputStream inputStream = subsection.getInputStream(ResourceAccess.PER_READ);
		assertThat(inputStream.skip(3), equalTo(2L));
		assertThat(inputStream.read(), equalTo(-1));
	}

	@Test
	public void inputStreamSkipNegative() throws Exception {
		assertThat(this.inputStream.skip(-1), equalTo(0L));
	}

	@Test
	public void getFile() throws Exception {
		assertThat(this.file.getFile(), equalTo(this.tempFile));
	}

	@Test
	public void concurrentReads() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(20);
		List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
		for (int i = 0; i < 100; i++) {
			results.add(executorService.submit(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					InputStream subsectionInputStream = RandomAccessDataFileTests.this.file
							.getSubsection(0, 256)
							.getInputStream(ResourceAccess.PER_READ);
					byte[] b = new byte[256];
					subsectionInputStream.read(b);
					return Arrays.equals(b, BYTES);
				}
			}));
		}
		for (Future<Boolean> future : results) {
			assertThat(future.get(), equalTo(true));
		}
	}

	@Test
	public void close() throws Exception {
		this.file.getInputStream(ResourceAccess.PER_READ).read();
		this.file.close();
		Field filePoolField = RandomAccessDataFile.class.getDeclaredField("filePool");
		filePoolField.setAccessible(true);
		Object filePool = filePoolField.get(this.file);
		Field filesField = filePool.getClass().getDeclaredField("files");
		filesField.setAccessible(true);
		Queue<?> queue = (Queue<?>) filesField.get(filePool);
		assertThat(queue.size(), equalTo(0));
	}

	private static Matcher<? super byte[]> startsWith(byte[] bytes) {
		return new ByteArrayStartsWith(bytes);
	}

}
