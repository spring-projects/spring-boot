/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.util.function.UnaryOperator;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Provide information about a fat jar structure that is meant to be extracted.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
interface JarStructure {

	/**
	 * Resolve the specified {@link ZipEntry}, return {@code null} if the entry should not
	 * be handled.
	 * @param entry the entry to handle
	 * @return the resolved {@link Entry}
	 */
	default Entry resolve(ZipEntry entry) {
		return resolve(entry.getName());
	}

	/**
	 * Resolve the entry with the specified name, return {@code null} if the entry should
	 * not be handled.
	 * @param name the name of the entry to handle
	 * @return the resolved {@link Entry}
	 */
	Entry resolve(String name);

	/**
	 * Create the {@link Manifest} for the launcher jar, applying the specified operator
	 * on each classpath entry.
	 * @param libraryTransformer the operator to apply on each classpath entry
	 * @return the manifest to use for the launcher jar
	 */
	Manifest createLauncherManifest(UnaryOperator<String> libraryTransformer);

	/**
	 * Return the location of the application classes.
	 * @return the location of the application classes
	 */
	String getClassesLocation();

	/**
	 * An entry to handle in the exploded structure.
	 *
	 * @param originalLocation the original location
	 * @param location the relative location
	 * @param type of the entry
	 */
	record Entry(String originalLocation, String location, Type type) {

		enum Type {

			LIBRARY, APPLICATION_CLASS_OR_RESOURCE, LOADER, META_INF

		}

	}

}
