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

package org.springframework.boot.loader.net.protocol.nested;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.zip.CloseableDataBlock;
import org.springframework.boot.loader.zip.ZipContent;

/**
 * Resources created managed and cleaned by a {@link NestedUrlConnection} instance and
 * suitable for registration with a {@link Cleaner}.
 *
 * @author Phillip Webb
 */
class NestedUrlConnectionResources implements Runnable {

	private final NestedLocation location;

	private volatile ZipContent zipContent;

	private volatile long size = -1;

	private volatile InputStream inputStream;

	/**
	 * Constructs a new NestedUrlConnectionResources object with the specified location.
	 * @param location the location of the nested resource
	 */
	NestedUrlConnectionResources(NestedLocation location) {
		this.location = location;
	}

	/**
	 * Returns the location of the NestedUrlConnectionResources object.
	 * @return the location of the NestedUrlConnectionResources object
	 */
	NestedLocation getLocation() {
		return this.location;
	}

	/**
	 * Connects to the specified URL and opens a zip content if not already opened.
	 * @throws IOException if an I/O error occurs while connecting or opening the zip
	 * content
	 */
	void connect() throws IOException {
		synchronized (this) {
			if (this.zipContent == null) {
				this.zipContent = ZipContent.open(this.location.path(), this.location.nestedEntryName());
				try {
					connectData();
				}
				catch (IOException | RuntimeException ex) {
					this.zipContent.close();
					this.zipContent = null;
					throw ex;
				}
			}
		}
	}

	/**
	 * Connects to the data by opening the raw zip data and setting the size and input
	 * stream.
	 * @throws IOException if an I/O error occurs while opening the raw zip data
	 */
	private void connectData() throws IOException {
		CloseableDataBlock data = this.zipContent.openRawZipData();
		try {
			this.size = data.size();
			this.inputStream = data.asInputStream();
		}
		catch (IOException | RuntimeException ex) {
			data.close();
		}
	}

	/**
	 * Returns the input stream associated with this NestedUrlConnectionResources object.
	 * @return the input stream
	 * @throws IOException if the input stream is not found for the nested location
	 */
	InputStream getInputStream() throws IOException {
		synchronized (this) {
			if (this.inputStream == null) {
				throw new IOException("Nested location not found " + this.location);
			}
			return this.inputStream;
		}
	}

	/**
	 * Returns the content length of the resource.
	 * @return the content length of the resource
	 */
	long getContentLength() {
		return this.size;
	}

	/**
	 * Releases all resources associated with the NestedUrlConnectionResources class.
	 */
	@Override
	public void run() {
		releaseAll();
	}

	/**
	 * Releases all resources associated with the NestedUrlConnectionResources object.
	 * This method is synchronized to ensure thread safety. If the zipContent is not null,
	 * it closes the inputStream and zipContent. If any IOException occurs during the
	 * closing of resources, it adds the exception to an exception chain. After closing
	 * the resources, it sets the size to -1. If any exception occurred during the closing
	 * of resources, it throws an UncheckedIOException with the exception chain.
	 */
	private void releaseAll() {
		synchronized (this) {
			if (this.zipContent != null) {
				IOException exceptionChain = null;
				try {
					this.inputStream.close();
				}
				catch (IOException ex) {
					exceptionChain = addToExceptionChain(exceptionChain, ex);
				}
				try {
					this.zipContent.close();
				}
				catch (IOException ex) {
					exceptionChain = addToExceptionChain(exceptionChain, ex);
				}
				this.size = -1;
				if (exceptionChain != null) {
					throw new UncheckedIOException(exceptionChain);
				}
			}
		}
	}

	/**
	 * Adds the specified exception to the exception chain.
	 * @param exceptionChain the exception chain to add the exception to
	 * @param ex the exception to be added to the chain
	 * @return the updated exception chain with the new exception added
	 */
	private IOException addToExceptionChain(IOException exceptionChain, IOException ex) {
		if (exceptionChain != null) {
			exceptionChain.addSuppressed(ex);
			return exceptionChain;
		}
		return ex;
	}

}
