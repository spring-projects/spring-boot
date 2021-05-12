/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SizeCalculatingEntryWriter}.
 *
 * @author Phillip Webb
 */
class SizeCalculatingEntryWriterTests {

	@Test
	void getWhenWithinThreshold() throws Exception {
		TestEntryWriter original = new TestEntryWriter(SizeCalculatingEntryWriter.THRESHOLD - 1);
		EntryWriter writer = SizeCalculatingEntryWriter.get(original);
		assertThat(writer.size()).isEqualTo(original.getBytes().length);
		assertThat(writeBytes(writer)).isEqualTo(original.getBytes());
		assertThat(writer).extracting("content").isNotInstanceOf(File.class);
	}

	@Test
	void getWhenExceedingThreshold() throws Exception {
		TestEntryWriter original = new TestEntryWriter(SizeCalculatingEntryWriter.THRESHOLD + 1);
		EntryWriter writer = SizeCalculatingEntryWriter.get(original);
		assertThat(writer.size()).isEqualTo(original.getBytes().length);
		assertThat(writeBytes(writer)).isEqualTo(original.getBytes());
		assertThat(writer).extracting("content").isInstanceOf(File.class);
	}

	private byte[] writeBytes(EntryWriter writer) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		writer.write(outputStream);
		outputStream.close();
		return outputStream.toByteArray();
	}

	private static class TestEntryWriter implements EntryWriter {

		private byte[] bytes;

		TestEntryWriter(int size) {
			this.bytes = new byte[size];
			new Random().nextBytes(this.bytes);
		}

		byte[] getBytes() {
			return this.bytes;
		}

		@Override
		public void write(OutputStream outputStream) throws IOException {
			outputStream.write(this.bytes);
		}

	}

}
