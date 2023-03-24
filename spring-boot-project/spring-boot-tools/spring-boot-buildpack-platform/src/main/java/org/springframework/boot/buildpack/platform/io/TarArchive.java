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

package org.springframework.boot.buildpack.platform.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * A TAR archive that can be written to an output stream.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface TarArchive {

	/**
	 * {@link Instant} that can be used to normalize TAR files so all entries have the
	 * same modification time.
	 */
	Instant NORMALIZED_TIME = OffsetDateTime.of(1980, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC).toInstant();

	/**
	 * Write the TAR archive to the given output stream.
	 * @param outputStream the output stream to write to
	 * @throws IOException on IO error
	 */
	void writeTo(OutputStream outputStream) throws IOException;

	/**
	 * Factory method to create a new {@link TarArchive} instance with a specific layout.
	 * @param layout the TAR layout
	 * @return a new {@link TarArchive} instance
	 */
	static TarArchive of(IOConsumer<Layout> layout) {
		return (outputStream) -> {
			TarLayoutWriter writer = new TarLayoutWriter(outputStream);
			layout.accept(writer);
			writer.finish();
		};
	}

	/**
	 * Factory method to adapt a ZIP file to {@link TarArchive}.
	 * @param zip the source zip file
	 * @param owner the owner of the entries in the TAR
	 * @return a new {@link TarArchive} instance
	 */
	static TarArchive fromZip(File zip, Owner owner) {
		return new ZipFileTarArchive(zip, owner);
	}

}
