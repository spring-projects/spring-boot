/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.loader.data;

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
	 * Returns an {@link InputStream} that can be used to read the underling data. The
	 * caller is responsible close the underlying stream.
	 * @param access hint indicating how the underlying data should be accessed
	 * @return a new input stream that can be used to read the underlying data.
	 * @throws IOException
	 */
	InputStream getInputStream(ResourceAccess access) throws IOException;

	/**
	 * Returns a new {@link RandomAccessData} for a specific subsection of this data.
	 * @param offset the offset of the subsection
	 * @param length the length of the subsection
	 * @return the subsection data
	 */
	RandomAccessData getSubsection(long offset, long length);

	/**
	 * Returns the size of the data.
	 * @return the size
	 */
	long getSize();

	/**
	 * Lock modes for accessing the underlying resource.
	 */
	public static enum ResourceAccess {

		/**
		 * Obtain access to the underlying resource once and keep it until the stream is
		 * closed.
		 */
		ONCE,

		/**
		 * Obtain access to the underlying resource on each read, releasing it when done.
		 */
		PER_READ
	}
}
