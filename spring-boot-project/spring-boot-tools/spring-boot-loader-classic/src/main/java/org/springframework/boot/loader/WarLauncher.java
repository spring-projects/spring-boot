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

package org.springframework.boot.loader;

import org.springframework.boot.loader.archive.Archive;

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 1.0.0
 */
public class WarLauncher extends ExecutableArchiveLauncher {

	/**
	 * Creates a new instance of the WarLauncher class.
	 */
	public WarLauncher() {
	}

	/**
	 * Constructs a new WarLauncher object with the specified Archive.
	 * @param archive the Archive object to be used by the WarLauncher
	 */
	protected WarLauncher(Archive archive) {
		super(archive);
	}

	/**
	 * Returns whether or not the class path archives should be post-processed.
	 * @return {@code true} if the class path archives should be post-processed,
	 * {@code false} otherwise.
	 */
	@Override
	protected boolean isPostProcessingClassPathArchives() {
		return false;
	}

	/**
	 * Determines if the given archive entry is a nested archive.
	 * @param entry the archive entry to check
	 * @return true if the entry is a nested archive, false otherwise
	 */
	@Override
	public boolean isNestedArchive(Archive.Entry entry) {
		if (entry.isDirectory()) {
			return entry.getName().equals("WEB-INF/classes/");
		}
		return entry.getName().startsWith("WEB-INF/lib/") || entry.getName().startsWith("WEB-INF/lib-provided/");
	}

	/**
	 * Returns the prefix path for the archive entry in the WAR file. The prefix path is
	 * set to "WEB-INF/".
	 * @return the prefix path for the archive entry
	 */
	@Override
	protected String getArchiveEntryPathPrefix() {
		return "WEB-INF/";
	}

	/**
	 * The main method is the entry point of the program. It launches the WarLauncher
	 * class and passes the command line arguments.
	 * @param args the command line arguments
	 * @throws Exception if an error occurs during program execution
	 */
	public static void main(String[] args) throws Exception {
		new WarLauncher().launch(args);
	}

}
