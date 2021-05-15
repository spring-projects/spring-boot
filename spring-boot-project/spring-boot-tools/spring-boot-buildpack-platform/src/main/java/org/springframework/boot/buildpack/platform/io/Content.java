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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Content with a known size that can be written to an {@link OutputStream}.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public interface Content {

	/**
	 * The size of the content in bytes.
	 * @return the content size
	 */
	int size();

	/**
	 * Write the content to the given output stream.
	 * @param outputStream the output stream to write to
	 * @throws IOException on IO error
	 */
	void writeTo(OutputStream outputStream) throws IOException;

	/**
	 * Create a new {@link Content} from the given UTF-8 string.
	 * @param string the string to write
	 * @return a new {@link Content} instance
	 */
	static Content of(String string) {
		Assert.notNull(string, "String must not be null");
		return of(string.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Create a new {@link Content} from the given input stream.
	 * @param bytes the bytes to write
	 * @return a new {@link Content} instance
	 */
	static Content of(byte[] bytes) {
		Assert.notNull(bytes, "Bytes must not be null");
		return of(bytes.length, () -> new ByteArrayInputStream(bytes));
	}

	static Content of(File file) {
		Assert.notNull(file, "File must not be null");
		return of((int) file.length(), () -> new FileInputStream(file));
	}

	/**
	 * Create a new {@link Content} from the given input stream. The stream will be closed
	 * after it has been written.
	 * @param size the size of the supplied input stream
	 * @param supplier the input stream supplier
	 * @return a new {@link Content} instance
	 */
	static Content of(int size, IOSupplier<InputStream> supplier) {
		Assert.isTrue(size >= 0, "Size must not be negative");
		Assert.notNull(supplier, "Supplier must not be null");
		return new Content() {

			@Override
			public int size() {
				return size;
			}

			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				FileCopyUtils.copy(supplier.get(), outputStream);
			}

		};
	}

}
