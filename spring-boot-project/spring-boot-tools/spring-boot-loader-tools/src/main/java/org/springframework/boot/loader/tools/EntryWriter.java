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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface used to write jar entry data.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface EntryWriter {

	/**
	 * Write entry data to the specified output stream.
	 * @param outputStream the destination for the data
	 * @throws IOException in case of I/O errors
	 */
	void write(OutputStream outputStream) throws IOException;

	/**
	 * Return the size of the content that will be written, or {@code -1} if the size is
	 * not known.
	 * @return the size of the content
	 */
	default int size() {
		return -1;
	}

}
