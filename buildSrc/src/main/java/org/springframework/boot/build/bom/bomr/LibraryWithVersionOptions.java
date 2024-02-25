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

package org.springframework.boot.build.bom.bomr;

import java.util.List;

import org.springframework.boot.build.bom.Library;

/**
 * LibraryWithVersionOptions class.
 */
class LibraryWithVersionOptions {

	private final Library library;

	private final List<VersionOption> versionOptions;

	/**
	 * Constructs a new LibraryWithVersionOptions object with the specified library and
	 * version options.
	 * @param library the library to be used
	 * @param versionOptions the list of version options
	 */
	LibraryWithVersionOptions(Library library, List<VersionOption> versionOptions) {
		this.library = library;
		this.versionOptions = versionOptions;
	}

	/**
	 * Returns the library object associated with this LibraryWithVersionOptions instance.
	 * @return the library object
	 */
	Library getLibrary() {
		return this.library;
	}

	/**
	 * Returns the list of version options available in the library.
	 * @return the list of version options
	 */
	List<VersionOption> getVersionOptions() {
		return this.versionOptions;
	}

}
