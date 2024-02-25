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
 * {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /BOOT-INF/lib} directory and that application classes are
 * included inside a {@code /BOOT-INF/classes} directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 3.2.0
 */
public class JarLauncher extends ExecutableArchiveLauncher {

	/**
	 * Constructs a new JarLauncher object.
	 * @throws Exception if an error occurs during the construction of the JarLauncher
	 * object.
	 */
	public JarLauncher() throws Exception {
	}

	/**
	 * Constructs a new JarLauncher object with the specified Archive.
	 * @param archive the Archive object representing the JAR file to be launched
	 * @throws Exception if an error occurs during the construction of the JarLauncher
	 * object
	 */
	protected JarLauncher(Archive archive) throws Exception {
		super(archive);
	}

	/**
	 * Determines if the given entry is included on the classpath.
	 * @param entry the entry to check
	 * @return true if the entry is included on the classpath, false otherwise
	 */
	@Override
	protected boolean isIncludedOnClassPath(Archive.Entry entry) {
		return isLibraryFileOrClassesDirectory(entry);
	}

	/**
	 * Returns the entry path prefix for the JarLauncher class. This method is used to
	 * specify the prefix for the entry path in the JAR file. The default prefix is
	 * "BOOT-INF/".
	 * @return The entry path prefix as a String.
	 */
	@Override
	protected String getEntryPathPrefix() {
		return "BOOT-INF/";
	}

	/**
	 * Determines whether the given Archive.Entry is a library file or classes directory.
	 * @param entry the Archive.Entry to be checked
	 * @return true if the entry is a classes directory or a library file, false otherwise
	 */
	static boolean isLibraryFileOrClassesDirectory(Archive.Entry entry) {
		String name = entry.name();
		if (entry.isDirectory()) {
			return name.equals("BOOT-INF/classes/");
		}
		return name.startsWith("BOOT-INF/lib/");
	}

	/**
	 * The main method is the entry point of the application. It launches the JarLauncher
	 * by creating a new instance and calling the launch method.
	 * @param args the command line arguments passed to the application
	 * @throws Exception if an error occurs during the execution of the launch method
	 */
	public static void main(String[] args) throws Exception {
		new JarLauncher().launch(args);
	}

}
