/*
 * Copyright 2012-2015 the original author or authors.
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
 * Archive layout types.
 *
 * @author Dave Syer
 */
public enum LayoutType {

	/**
	 * Jar Layout.
	 */
	JAR,

	/**
	 * War Layout.
	 */
	WAR,

	/**
	 * Zip Layout.
	 */
	ZIP,

	/**
	 * Dir Layout.
	 */
	DIR,

	/**
	 * Module Layout.
	 */
	MODULE,

	/**
	 * No Layout.
	 */
	NONE;


	/**
	 * Return a layout type for the given source file.
	 * @param file the source file
	 * @return a {@link Layout}
	 */
	public static LayoutType forFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File must not be null");
		}
		if (file.getName().toLowerCase().endsWith(".jar")) {
			return JAR;
		}
		if (file.getName().toLowerCase().endsWith(".war")) {
			return WAR;
		}
		if (file.isDirectory() || file.getName().toLowerCase().endsWith(".zip")) {
			return ZIP;
		}
		throw new IllegalArgumentException("Unable to deduce layout for '" + file + "'");
	}

}
