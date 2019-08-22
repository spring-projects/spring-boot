/*
 * Copyright 2012-2019 the original author or authors.
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

/**
 * Encapsulates information about a single library that may be packed into the archive.
 *
 * @author Phillip Webb
 * @since 1.1.2
 * @see Libraries
 */
public class Library {

	private final String name;

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
		this(null, file, scope, unpackRequired);
	}

	/**
	 * Create a new {@link Library}.
	 * @param name the name of the library as it should be written or {@code null} to use
	 * the file name
	 * @param file the source file
	 * @param scope the scope of the library
	 * @param unpackRequired if the library needs to be unpacked before it can be used
	 */
	public Library(String name, File file, LibraryScope scope, boolean unpackRequired) {
		this.name = (name != null) ? name : file.getName();
		this.file = file;
		this.scope = scope;
		this.unpackRequired = unpackRequired;
	}

	/**
	 * Return the name of file as it should be written.
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the library file.
	 * @return the file
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * Return the scope of the library.
	 * @return the scope
	 */
	public LibraryScope getScope() {
		return this.scope;
	}

	/**
	 * Return if the file cannot be used directly as a nested jar and needs to be
	 * unpacked.
	 * @return if unpack is required
	 */
	public boolean isUnpackRequired() {
		return this.unpackRequired;
	}

}
