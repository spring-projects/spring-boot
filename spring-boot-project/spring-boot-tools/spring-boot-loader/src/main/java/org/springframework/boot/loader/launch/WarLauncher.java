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

package org.springframework.boot.loader.launch;

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.2.0
 */
public class WarLauncher extends ExecutableArchiveLauncher {

	/**
     * Constructs a new WarLauncher object.
     * 
     * @throws Exception if an error occurs during the construction of the WarLauncher object.
     */
    public WarLauncher() throws Exception {
	}

	/**
     * Constructs a new WarLauncher object with the specified Archive.
     * 
     * @param archive the Archive object to be used by the WarLauncher
     * @throws Exception if an error occurs during the construction of the WarLauncher
     */
    protected WarLauncher(Archive archive) throws Exception {
		super(archive);
	}

	/**
     * Determines if the given entry is included on the classpath.
     * 
     * @param entry the entry to check
     * @return true if the entry is included on the classpath, false otherwise
     */
    @Override
	public boolean isIncludedOnClassPath(Archive.Entry entry) {
		return isLibraryFileOrClassesDirectory(entry);
	}

	/**
     * Returns the entry path prefix for the web application.
     * The entry path prefix is used to specify the location of the web application's resources.
     * By default, the entry path prefix is set to "WEB-INF/".
     *
     * @return the entry path prefix for the web application
     */
    @Override
	protected String getEntryPathPrefix() {
		return "WEB-INF/";
	}

	/**
     * Determines if the given Archive.Entry is a library file or classes directory.
     * 
     * @param entry the Archive.Entry to check
     * @return true if the entry is a library file or classes directory, false otherwise
     */
    static boolean isLibraryFileOrClassesDirectory(Archive.Entry entry) {
		String name = entry.name();
		if (entry.isDirectory()) {
			return name.equals("WEB-INF/classes/");
		}
		return name.startsWith("WEB-INF/lib/") || name.startsWith("WEB-INF/lib-provided/");
	}

	/**
     * The main method is the entry point of the program.
     * It launches the WarLauncher class and passes the command line arguments.
     *
     * @param args the command line arguments
     * @throws Exception if an error occurs during program execution
     */
    public static void main(String[] args) throws Exception {
		new WarLauncher().launch(args);
	}

}
