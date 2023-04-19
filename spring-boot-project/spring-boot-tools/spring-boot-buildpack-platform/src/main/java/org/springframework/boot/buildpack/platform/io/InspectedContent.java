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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

/**
 * {@link Content} that is reads and inspects a source of data only once but allows it to
 * be consumed multiple times.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class InspectedContent implements Content {

	static final int MEMORY_LIMIT = 4 * 1024 + 3;

	private final int size;

	private final Object content;

	InspectedContent(int size, Object content) {
		this.size = size;
		this.content = content;
	}

	@Override
	public int size() {
		return this.size;
	}

	@Override
	public void writeTo(OutputStream outputStream) throws IOException {
		if (this.content instanceof byte[] bytes) {
			FileCopyUtils.copy(bytes, outputStream);
		}
		else if (this.content instanceof File file) {
			InputStream inputStream = new FileInputStream(file);
			FileCopyUtils.copy(inputStream, outputStream);
		}
		else {
			throw new IllegalStateException("Unknown content type");
		}
	}

	/**
	 * Factory method to create an {@link InspectedContent} instance from a source input
	 * stream.
	 * @param inputStream the content input stream
	 * @param inspectors any inspectors to apply
	 * @return a new inspected content instance
	 * @throws IOException on IO error
	 */
	public static InspectedContent of(InputStream inputStream, Inspector... inspectors) throws IOException {
		Assert.notNull(inputStream, "InputStream must not be null");
		return of((outputStream) -> FileCopyUtils.copy(inputStream, outputStream), inspectors);
	}

	/**
	 * Factory method to create an {@link InspectedContent} instance from source content.
	 * @param content the content
	 * @param inspectors any inspectors to apply
	 * @return a new inspected content instance
	 * @throws IOException on IO error
	 */
	public static InspectedContent of(Content content, Inspector... inspectors) throws IOException {
		Assert.notNull(content, "Content must not be null");
		return of(content::writeTo, inspectors);
	}

	/**
	 * Factory method to create an {@link InspectedContent} instance from a source write
	 * method.
	 * @param writer a consumer representing the write method
	 * @param inspectors any inspectors to apply
	 * @return a new inspected content instance
	 * @throws IOException on IO error
	 */
	public static InspectedContent of(IOConsumer<OutputStream> writer, Inspector... inspectors) throws IOException {
		Assert.notNull(writer, "Writer must not be null");
		InspectingOutputStream outputStream = new InspectingOutputStream(inspectors);
		try (outputStream) {
			writer.accept(outputStream);
		}
		return new InspectedContent(outputStream.getSize(), outputStream.getContent());
	}

	/**
	 * Interface that can be used to inspect content as it is initially read.
	 */
	public interface Inspector {

		/**
		 * Update inspected information based on the provided bytes.
		 * @param input the array of bytes.
		 * @param offset the offset to start from in the array of bytes.
		 * @param len the number of bytes to use, starting at {@code offset}.
		 * @throws IOException on IO error
		 */
		void update(byte[] input, int offset, int len) throws IOException;

	}

	/**
	 * Internal {@link OutputStream} used to capture the content either as bytes, or to a
	 * File if the content is too large.
	 */
	private static final class InspectingOutputStream extends OutputStream {

		private final Inspector[] inspectors;

		private int size;

		private OutputStream delegate;

		private File tempFile;

		private final byte[] singleByteBuffer = new byte[0];

		private InspectingOutputStream(Inspector[] inspectors) {
			this.inspectors = inspectors;
			this.delegate = new ByteArrayOutputStream();
		}

		@Override
		public void write(int b) throws IOException {
			this.singleByteBuffer[0] = (byte) (b & 0xFF);
			write(this.singleByteBuffer);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			int size = len - off;
			if (this.tempFile == null && (this.size + size) > MEMORY_LIMIT) {
				convertToTempFile();
			}
			this.delegate.write(b, off, len);
			for (Inspector inspector : this.inspectors) {
				inspector.update(b, off, len);
			}
			this.size += size;
		}

		private void convertToTempFile() throws IOException {
			this.tempFile = File.createTempFile("buildpack", ".tmp");
			byte[] bytes = ((ByteArrayOutputStream) this.delegate).toByteArray();
			this.delegate = new FileOutputStream(this.tempFile);
			StreamUtils.copy(bytes, this.delegate);
		}

		private Object getContent() {
			return (this.tempFile != null) ? this.tempFile : ((ByteArrayOutputStream) this.delegate).toByteArray();
		}

		private int getSize() {
			return this.size;
		}

	}

}
