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

package org.springframework.boot.loader.tools;

/**
 * Strategy interface used to determine the layout for a particular type of archive.
 *
 * @author Phillip Webb
 * @see Layouts
 */
public interface Layout {

	/**
	 * Returns the launcher class name for this layout.
	 * @return the launcher class name
	 */
	String getLauncherClassName();

	/**
	 * Returns the destination path for a given library.
	 * @param libraryName the name of the library (excluding any path)
	 * @param scope the scope of the library
	 * @return the destination relative to the root of the archive (should end with '/')
	 * or {@code null} if the library should not be included.
	 */
	String getLibraryDestination(String libraryName, LibraryScope scope);

	/**
	 * Returns the location of classes within the archive.
	 */
	String getClassesLocation();

	/**
	 * Returns if loader classes should be included to make the archive executable.
	 */
	boolean isExecutable();

}
