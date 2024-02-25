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

package org.springframework.boot.web.servlet.server;

import java.io.File;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.util.Assert;

/**
 * Manages a session store directory.
 *
 * @author Phillip Webb
 * @see AbstractServletWebServerFactory
 */
class SessionStoreDirectory {

	private File directory;

	/**
	 * Returns the directory associated with this SessionStoreDirectory object.
	 * @return the directory associated with this SessionStoreDirectory object
	 */
	File getDirectory() {
		return this.directory;
	}

	/**
	 * Sets the directory for the session store.
	 * @param directory the directory to set
	 */
	void setDirectory(File directory) {
		this.directory = directory;
	}

	/**
	 * Retrieves a valid directory for storing session data.
	 * @param mkdirs a boolean indicating whether to create the directory if it does not
	 * exist
	 * @return a File object representing the valid directory
	 */
	File getValidDirectory(boolean mkdirs) {
		File dir = getDirectory();
		if (dir == null) {
			return new ApplicationTemp().getDir("servlet-sessions");
		}
		if (!dir.isAbsolute()) {
			dir = new File(new ApplicationHome().getDir(), dir.getPath());
		}
		if (!dir.exists() && mkdirs) {
			dir.mkdirs();
		}
		assertDirectory(mkdirs, dir);
		return dir;
	}

	/**
	 * Asserts that the given directory exists and is not a file.
	 * @param mkdirs a boolean indicating whether the directory should be created if it
	 * does not exist
	 * @param dir the directory to be checked
	 * @throws IllegalStateException if the directory does not exist or if it is a file
	 */
	private void assertDirectory(boolean mkdirs, File dir) {
		Assert.state(!mkdirs || dir.exists(), () -> "Session dir " + dir + " does not exist");
		Assert.state(!dir.isFile(), () -> "Session dir " + dir + " points to a file");
	}

}
