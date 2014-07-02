/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;

/**
 * Encapsulates information about a single library that may be packed into the archive.
 *
 * @author Phillip Webb
 * @since 1.1.2
 * @see Libraries
 */
public class Library {

	private final File file;

	private final LibraryScope scope;

	private final boolean unpackRequired;

	/**
	 * Create a new {@link Library}.
	 * @param file the source file
	 * @param scope the scope of the library
	 */
	public Library(File file, LibraryScope scope) {
		this(file, scope, false);
	}

	/**
	 * Create a new {@link Library}.
	 * @param file the source file
	 * @param scope the scope of the library
	 * @param unpackRequired if the library needs to be unpacked before it can be used
	 */
	public Library(File file, LibraryScope scope, boolean unpackRequired) {
		this.file = file;
		this.scope = scope;
		this.unpackRequired = unpackRequired;
	}

	/**
	 * @return the library file
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * @return the scope of the library
	 */
	public LibraryScope getScope() {
		return this.scope;
	}

	/**
	 * @return if the file cannot be used directly as a nested jar and needs to be
	 * unpacked.
	 */
	public boolean isUnpackRequired() {
		return this.unpackRequired;
	}

}
