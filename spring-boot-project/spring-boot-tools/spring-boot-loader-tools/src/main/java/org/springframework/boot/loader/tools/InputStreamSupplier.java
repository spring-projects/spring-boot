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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Supplier to provide an {@link InputStream}.
 *
 * @author Phillip Webb
 */
@FunctionalInterface
interface InputStreamSupplier {

	/**
	 * Returns a new open {@link InputStream} at the beginning of the content.
	 * @return a new {@link InputStream}
	 * @throws IOException on IO error
	 */
	InputStream openStream() throws IOException;

	/**
	 * Factory method to create an {@link InputStreamSupplier} for the given {@link File}.
	 * @param file the source file
	 * @return a new {@link InputStreamSupplier} instance
	 */
	static InputStreamSupplier forFile(File file) {
		return () -> new FileInputStream(file);
	}

}
