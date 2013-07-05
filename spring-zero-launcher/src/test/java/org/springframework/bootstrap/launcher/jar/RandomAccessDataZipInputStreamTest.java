/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.bootstrap.launcher.jar;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.bootstrap.launcher.data.RandomAccessDataFile;

/**
 * Tests for {@link RandomAccessDataZipInputStream}.
 * 
 * @author Phillip Webb
 */
public class RandomAccessDataZipInputStreamTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File file;

	@Before
	public void setup() throws Exception {
		this.file = temporaryFolder.newFile();
		ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
		try {
			writeDataEntry(zipOutputStream, "a", new byte[10]);
			writeDataEntry(zipOutputStream, "b", new byte[20]);
		} finally {
			zipOutputStream.close();
		}
	}

	private void writeDataEntry(ZipOutputStream zipOutputStream, String name, byte[] data)
			throws IOException {
		ZipEntry entry = new ZipEntry(name);
		entry.setMethod(ZipEntry.STORED);
		entry.setSize(data.length);
		entry.setCompressedSize(data.length);
		CRC32 crc32 = new CRC32();
		crc32.update(data);
		entry.setCrc(crc32.getValue());
		zipOutputStream.putNextEntry(entry);
		zipOutputStream.write(data);
		zipOutputStream.closeEntry();
	}

	@Test
	public void entryData() throws Exception {
		RandomAccessDataZipInputStream z = new RandomAccessDataZipInputStream(
				new RandomAccessDataFile(file));
		try {
			RandomAccessDataZipEntry entry1 = z.getNextEntry();
			RandomAccessDataZipEntry entry2 = z.getNextEntry();
			assertThat(entry1.getName(), equalTo("a"));
			assertThat(entry1.getData().getSize(), equalTo(10L));
			assertThat(entry2.getName(), equalTo("b"));
			assertThat(entry2.getData().getSize(), equalTo(20L));
			assertThat(z.getNextEntry(), nullValue());
		} finally {
			z.close();
		}
	}

}
