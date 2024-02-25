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

package org.springframework.boot.loader.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.util.StreamUtils;

/**
 * {@link EntryWriter} that always provides size information.
 *
 * @author Phillip Webb
 */
final class SizeCalculatingEntryWriter implements EntryWriter {

	static final int THRESHOLD = 1024 * 20;

	private final Object content;

	private final int size;

	/**
     * Constructs a new SizeCalculatingEntryWriter object with the given EntryWriter.
     * This constructor calculates the size of the content written by the EntryWriter.
     * 
     * @param entryWriter the EntryWriter object to write the content
     * @throws IOException if an I/O error occurs while writing the content
     */
    private SizeCalculatingEntryWriter(EntryWriter entryWriter) throws IOException {
		SizeCalculatingOutputStream outputStream = new SizeCalculatingOutputStream();
		try (outputStream) {
			entryWriter.write(outputStream);
		}
		this.content = outputStream.getContent();
		this.size = outputStream.getSize();
	}

	/**
     * Writes the content of the entry to the specified output stream.
     * 
     * @param outputStream the output stream to write the content to
     * @throws IOException if an I/O error occurs while writing the content
     */
    @Override
	public void write(OutputStream outputStream) throws IOException {
		InputStream inputStream = getContentInputStream();
		copy(inputStream, outputStream);
	}

	/**
     * Returns an InputStream for the content of this SizeCalculatingEntryWriter.
     * 
     * @return the InputStream for the content
     * @throws FileNotFoundException if the content is a File and it cannot be found
     */
    private InputStream getContentInputStream() throws FileNotFoundException {
		if (this.content instanceof File file) {
			return new FileInputStream(file);
		}
		return new ByteArrayInputStream((byte[]) this.content);
	}

	/**
     * Copies the content from the given input stream to the output stream.
     *
     * @param inputStream  the input stream to read from
     * @param outputStream the output stream to write to
     * @throws IOException if an I/O error occurs during the copying process
     */
    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
		try (inputStream) {
			StreamUtils.copy(inputStream, outputStream);
		}
	}

	/**
     * Returns the size of the object.
     *
     * @return the size of the object
     */
    @Override
	public int size() {
		return this.size;
	}

	/**
     * Returns an EntryWriter object.
     * 
     * This method checks if the given entryWriter is null or if its size is not -1. If either condition is true, the method returns the entryWriter as is.
     * Otherwise, it creates a new SizeCalculatingEntryWriter object using the given entryWriter and returns it.
     * 
     * @param entryWriter the EntryWriter object to be checked and returned
     * @return an EntryWriter object
     * @throws IOException if an I/O error occurs
     */
    static EntryWriter get(EntryWriter entryWriter) throws IOException {
		if (entryWriter == null || entryWriter.size() != -1) {
			return entryWriter;
		}
		return new SizeCalculatingEntryWriter(entryWriter);
	}

	/**
	 * {@link OutputStream} to calculate the size and allow content to be written again.
	 */
	private static class SizeCalculatingOutputStream extends OutputStream {

		private int size = 0;

		private File tempFile;

		private OutputStream outputStream;

		/**
         * Constructs a new SizeCalculatingOutputStream object.
         * This constructor initializes the outputStream field with a new ByteArrayOutputStream object.
         */
        SizeCalculatingOutputStream() {
			this.outputStream = new ByteArrayOutputStream();
		}

		/**
         * Writes a single byte to the output stream.
         * 
         * @param b the byte to be written
         * @throws IOException if an I/O error occurs
         */
        @Override
		public void write(int b) throws IOException {
			write(new byte[] { (byte) b }, 0, 1);
		}

		/**
         * Writes a portion of an array of bytes to the output stream. The number of bytes written is specified by the len parameter.
         * If the total size of the data written exceeds the threshold and the output stream is an instance of ByteArrayOutputStream,
         * the output stream is converted to a FileOutputStream.
         *
         * @param b   the data.
         * @param off the start offset in the data.
         * @param len the number of bytes to write.
         * @throws IOException if an I/O error occurs.
         */
        @Override
		public void write(byte[] b, int off, int len) throws IOException {
			int updatedSize = this.size + len;
			if (updatedSize > THRESHOLD && this.outputStream instanceof ByteArrayOutputStream byteArrayOutputStream) {
				this.outputStream = convertToFileOutputStream(byteArrayOutputStream);
			}
			this.outputStream.write(b, off, len);
			this.size = updatedSize;
		}

		/**
         * Converts a ByteArrayOutputStream to a FileOutputStream and returns the FileOutputStream.
         * 
         * @param byteArrayOutputStream the ByteArrayOutputStream to convert
         * @return the converted FileOutputStream
         * @throws IOException if an I/O error occurs
         */
        private OutputStream convertToFileOutputStream(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
			initializeTempFile();
			FileOutputStream fileOutputStream = new FileOutputStream(this.tempFile);
			StreamUtils.copy(byteArrayOutputStream.toByteArray(), fileOutputStream);
			return fileOutputStream;
		}

		/**
         * Initializes the temporary file used by the SizeCalculatingOutputStream.
         * If the temporary file has not been created yet, it creates a new temporary file with a specific prefix and suffix.
         * The temporary file will be deleted when the JVM exits.
         *
         * @throws IOException if an I/O error occurs while creating the temporary file
         */
        private void initializeTempFile() throws IOException {
			if (this.tempFile == null) {
				this.tempFile = File.createTempFile("springboot-", "-entrycontent");
				this.tempFile.deleteOnExit();
			}
		}

		/**
         * Closes the output stream.
         * 
         * @throws IOException if an I/O error occurs while closing the stream
         */
        @Override
		public void close() throws IOException {
			this.outputStream.close();
		}

		/**
         * Returns the content of the output stream.
         * If the output stream is an instance of ByteArrayOutputStream,
         * it returns the byte array of the ByteArrayOutputStream.
         * Otherwise, it returns the temporary file.
         *
         * @return the content of the output stream
         */
        Object getContent() {
			return (this.outputStream instanceof ByteArrayOutputStream byteArrayOutputStream)
					? byteArrayOutputStream.toByteArray() : this.tempFile;
		}

		/**
         * Returns the size of the output stream.
         *
         * @return the size of the output stream
         */
        int getSize() {
			return this.size;
		}

	}

}
