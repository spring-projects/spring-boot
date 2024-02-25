/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.buildtoolplugins.otherbuildsystems.examplerepackageimplementation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;
import org.springframework.boot.loader.tools.Repackager;

/**
 * MyBuildTool class.
 */
public class MyBuildTool {

	/**
     * Builds the project by repackaging the source JAR file.
     * 
     * @throws IOException if an I/O error occurs during the repackaging process.
     */
    public void build() throws IOException {
		File sourceJarFile = /**/ null;
		Repackager repackager = new Repackager(sourceJarFile);
		repackager.setBackupSource(false);
		repackager.repackage(this::getLibraries);
	}

	/**
     * Retrieves the libraries used in the build process.
     * 
     * @param callback the callback to be invoked for each library
     * @throws IOException if an I/O error occurs while retrieving the libraries
     */
    private void getLibraries(LibraryCallback callback) throws IOException {
		// Build system specific implementation, callback for each dependency
		for (File nestedJar : getCompileScopeJars()) {
			callback.library(new Library(nestedJar, LibraryScope.COMPILE));
		}
		// ...
	}

	/**
     * Returns the list of JAR files that are part of the compile scope.
     *
     * @return the list of JAR files in the compile scope, or null if none are found
     */
    private List<File> getCompileScopeJars() {
		return /**/ null;
	}

}
