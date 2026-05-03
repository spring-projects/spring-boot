/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Encapsulates information about a single library that may be packed into the archive.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 1.1.2
 * @see Libraries
 */
public class Library {

	private final String name;

	private final @Nullable File file;

	private final @Nullable LibraryScope scope;

	private final @Nullable LibraryCoordinates coordinates;

	private final boolean unpackRequired;

	private final boolean local;

	private final boolean included;

	/**
	 * Create a new {@link Library}.
	 * @param file the source file
	 * @param scope the scope of the library
	 */
	public Library(File file, LibraryScope scope) {
		this(null, file, scope, null, false, false, true);
	}

	/**
	 * Create a new {@link Library}.
	 * @param name the name of the library as it should be written or {@code null} to use
	 * the file name
	 * @param file the source file
	 * @param scope the scope of the library
	 * @param coordinates the library coordinates or {@code null}
	 * @param unpackRequired if the library needs to be unpacked before it can be used
	 * @param local if the library is local (part of the same build) to the application
	 * that is being packaged
	 * @param included if the library is included in the uber jar
	 * @since 2.4.8
	 */
	public Library(@Nullable String name, @Nullable File file, @Nullable LibraryScope scope,
			@Nullable LibraryCoordinates coordinates, boolean unpackRequired, boolean local, boolean included) {
		this.name = (name != null) ? name : getFileName(file);
		this.file = file;
		this.scope = scope;
		this.coordinates = coordinates;
		this.unpackRequired = unpackRequired;
		this.local = local;
		this.included = included;
	}

	private static String getFileName(@Nullable File file) {
		Assert.state(file != null, "'file' must not be null");
		return file.getName();
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
	public @Nullable File getFile() {
		return this.file;
	}

	/**
	 * Open a stream that provides the content of the source file.
	 * @return the file content
	 * @throws IOException on error
	 */
	InputStream openStream() throws IOException {
		Assert.state(this.file != null, "'file' must not be null");
		return new FileInputStream(this.file);
	}

	/**
	 * Return the scope of the library.
	 * @return the scope
	 */
	public @Nullable LibraryScope getScope() {
		return this.scope;
	}

	/**
	 * Return the {@linkplain LibraryCoordinates coordinates} of the library.
	 * @return the coordinates
	 */
	public @Nullable LibraryCoordinates getCoordinates() {
		return this.coordinates;
	}

	/**
	 * Return if the file cannot be used directly as a nested jar and needs to be
	 * unpacked.
	 * @return if unpack is required
	 */
	public boolean isUnpackRequired() {
		return this.unpackRequired;
	}

	long getLastModified() {
		Assert.state(this.file != null, "'file' must not be null");
		return this.file.lastModified();
	}

	/**
	 * Return if the library is local (part of the same build) to the application that is
	 * being packaged.
	 * @return if the library is local
	 */
	public boolean isLocal() {
		return this.local;
	}

	/**
	 * Return if the library is included in the uber jar.
	 * @return if the library is included
	 */
	public boolean isIncluded() {
		return this.included;
	}

}
