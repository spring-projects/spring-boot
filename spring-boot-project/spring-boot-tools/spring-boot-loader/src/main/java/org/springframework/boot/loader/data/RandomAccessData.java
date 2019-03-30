/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.loader.data;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface that provides read-only random access to some underlying data.
 * Implementations must allow concurrent reads in a thread-safe manner.
 *
 * @author Phillip Webb
 */
public interface RandomAccessData {

	/**
	 * Returns an {@link InputStream} that can be used to read the underlying data. The
	 * caller is responsible close the underlying stream.
	 * @return a new input stream that can be used to read the underlying data.
	 * @throws IOException if the stream cannot be opened
	 */
	InputStream getInputStream() throws IOException;

	/**
	 * Returns a new {@link RandomAccessData} for a specific subsection of this data.
	 * @param offset the offset of the subsection
	 * @param length the length of the subsection
	 * @return the subsection data
	 */
	RandomAccessData getSubsection(long offset, long length);

	/**
	 * Reads all the data and returns it as a byte array.
	 * @return the data
	 * @throws IOException if the data cannot be read
	 */
	byte[] read() throws IOException;

	/**
	 * Reads the {@code length} bytes of data starting at the given {@code offset}.
	 * @param offset the offset from which data should be read
	 * @param length the number of bytes to be read
	 * @return the data
	 * @throws IOException if the data cannot be read
	 * @throws IndexOutOfBoundsException if offset is beyond the end of the file or
	 * subsection
	 * @throws EOFException if offset plus length is greater than the length of the file
	 * or subsection
	 */
	byte[] read(long offset, long length) throws IOException;

	/**
	 * Returns the size of the data.
	 * @return the size
	 */
	long getSize();

}
