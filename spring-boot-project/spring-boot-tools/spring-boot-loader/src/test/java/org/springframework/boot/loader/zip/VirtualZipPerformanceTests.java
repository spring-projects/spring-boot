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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Performance tests for {@link ZipContent} that creates a {@link VirtualZipDataBlock}.
 *
 * @author Phillip Webb
 */
@Disabled("Only used for manual testing")
class VirtualZipPerformanceTests {

	@TempDir
	Path temp;

	@Test
	void sequentialReadPerformace() throws IOException {
		File file = createZipWithLargeEntries();
		long start = System.nanoTime();
		try (ZipContent zipContent = ZipContent.open(file.toPath(), "test/")) {
			try (InputStream in = zipContent.openRawZipData().asInputStream()) {
				ZipInputStream zip = new ZipInputStream(in);
				ZipEntry entry = zip.getNextEntry();
				while (entry != null) {
					entry = zip.getNextEntry();
				}
			}
		}
		System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
	}

	private File createZipWithLargeEntries() throws IOException {
		byte[] bytes = new byte[1024 * 1024];
		new Random().nextBytes(bytes);
		File file = this.temp.resolve("test.zip").toFile();
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
			out.putNextEntry(new ZipEntry("test/"));
			out.closeEntry();
			for (int i = 0; i < 50; i++) {
				out.putNextEntry(new ZipEntry("test/" + i + ".dat"));
				out.write(bytes);
				out.closeEntry();
			}
		}
		return file;
	}

}
