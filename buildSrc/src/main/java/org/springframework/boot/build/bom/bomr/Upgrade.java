/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * An upgrade to change a {@link Library} to use a new version.
 *
 * @author Andy Wilkinson
 */
final class Upgrade {

	private final Library library;

	private final DependencyVersion version;

	/**
     * Upgrades the library to the specified version.
     * 
     * @param library the library to be upgraded
     * @param version the version to upgrade to
     */
    Upgrade(Library library, DependencyVersion version) {
		this.library = library;
		this.version = version;
	}

	/**
     * Returns the library object associated with this Upgrade.
     *
     * @return the library object associated with this Upgrade
     */
    Library getLibrary() {
		return this.library;
	}

	/**
     * Returns the version of the dependency.
     *
     * @return the version of the dependency
     */
    DependencyVersion getVersion() {
		return this.version;
	}

}
