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

package org.springframework.boot.buildpack.platform.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TarLayoutWriter}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class TarLayoutWriterTests {

	@Test
	void writesTarArchive() throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (TarLayoutWriter writer = new TarLayoutWriter(outputStream)) {
			writer.directory("/foo", Owner.ROOT);
			writer.file("/foo/bar.txt", Owner.of(1, 1), 0777, Content.of("test"));
		}
		try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()))) {
			TarArchiveEntry directoryEntry = tarInputStream.getNextTarEntry();
			TarArchiveEntry fileEntry = tarInputStream.getNextTarEntry();
			byte[] fileContent = new byte[(int) fileEntry.getSize()];
			tarInputStream.read(fileContent);
			assertThat(tarInputStream.getNextEntry()).isNull();
			assertThat(directoryEntry.getName()).isEqualTo("/foo/");
			assertThat(directoryEntry.getMode()).isEqualTo(0755);
			assertThat(directoryEntry.getLongUserId()).isZero();
			assertThat(directoryEntry.getLongGroupId()).isZero();
			assertThat(directoryEntry.getModTime()).isEqualTo(new Date(TarLayoutWriter.NORMALIZED_MOD_TIME));
			assertThat(fileEntry.getName()).isEqualTo("/foo/bar.txt");
			assertThat(fileEntry.getMode()).isEqualTo(0777);
			assertThat(fileEntry.getLongUserId()).isOne();
			assertThat(fileEntry.getLongGroupId()).isOne();
			assertThat(fileEntry.getModTime()).isEqualTo(new Date(TarLayoutWriter.NORMALIZED_MOD_TIME));
			assertThat(fileContent).isEqualTo("test".getBytes(StandardCharsets.UTF_8));
		}
	}

}
