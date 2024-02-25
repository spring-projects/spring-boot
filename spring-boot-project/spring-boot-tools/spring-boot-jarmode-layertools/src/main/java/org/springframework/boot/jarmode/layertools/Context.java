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

package org.springframework.boot.jarmode.layertools;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;

import org.springframework.util.Assert;

/**
 * Context for use by commands.
 *
 * @author Phillip Webb
 */
class Context {

	private final File archiveFile;

	private final File workingDir;

	private final String relativeDir;

	/**
	 * Create a new {@link Context} instance.
	 */
	Context() {
		this(getSourceArchiveFile(), Paths.get(".").toAbsolutePath().normalize().toFile());
	}

	/**
	 * Create a new {@link Context} instance with the specified value.
	 * @param archiveFile the source archive file
	 * @param workingDir the working directory
	 */
	Context(File archiveFile, File workingDir) {
		Assert.state(isExistingFile(archiveFile), "Unable to find source archive");
		Assert.state(isJarOrWar(archiveFile), "Source archive " + archiveFile + " must end with .jar or .war");
		this.archiveFile = archiveFile;
		this.workingDir = workingDir;
		this.relativeDir = deduceRelativeDir(archiveFile.getParentFile(), this.workingDir);
	}

	/**
     * Checks if the given file is an existing file.
     * 
     * @param archiveFile the file to be checked
     * @return true if the file exists and is a regular file, false otherwise
     */
    private boolean isExistingFile(File archiveFile) {
		return archiveFile != null && archiveFile.isFile() && archiveFile.exists();
	}

	/**
     * Checks if the given file is a JAR or WAR file.
     * 
     * @param jarFile the file to be checked
     * @return true if the file is a JAR or WAR file, false otherwise
     */
    private boolean isJarOrWar(File jarFile) {
		String name = jarFile.getName().toLowerCase();
		return name.endsWith(".jar") || name.endsWith(".war");
	}

	/**
     * Returns the source archive file for the Context class.
     * 
     * @return the source archive file, or null if it cannot be found
     */
    private static File getSourceArchiveFile() {
		try {
			ProtectionDomain domain = Context.class.getProtectionDomain();
			CodeSource codeSource = (domain != null) ? domain.getCodeSource() : null;
			URL location = (codeSource != null) ? codeSource.getLocation() : null;
			File source = (location != null) ? findSource(location) : null;
			if (source != null && source.exists()) {
				return source.getAbsoluteFile();
			}
			return null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
     * Finds the source file for the given URL location.
     * 
     * @param location the URL location to find the source file for
     * @return the source file for the given URL location
     * @throws IOException if an I/O error occurs while opening the connection
     * @throws URISyntaxException if the URL location is not a valid URI
     */
    private static File findSource(URL location) throws IOException, URISyntaxException {
		URLConnection connection = location.openConnection();
		if (connection instanceof JarURLConnection jarURLConnection) {
			return getRootJarFile(jarURLConnection.getJarFile());
		}
		return new File(location.toURI());
	}

	/**
     * Returns the root JAR file associated with the given {@link JarFile}.
     * 
     * @param jarFile the {@link JarFile} object
     * @return the root JAR file
     */
    private static File getRootJarFile(JarFile jarFile) {
		String name = jarFile.getName();
		int separator = name.indexOf("!/");
		if (separator > 0) {
			name = name.substring(0, separator);
		}
		return new File(name);
	}

	/**
     * Deduces the relative directory path between the source directory and the working directory.
     * 
     * @param sourceDirectory The source directory to deduce the relative path from.
     * @param workingDir The working directory to deduce the relative path to.
     * @return The relative directory path between the source directory and the working directory,
     *         or null if the source directory is the same as the working directory or if the source
     *         directory is not a subdirectory of the working directory.
     */
    private String deduceRelativeDir(File sourceDirectory, File workingDir) {
		String sourcePath = sourceDirectory.getAbsolutePath();
		String workingPath = workingDir.getAbsolutePath();
		if (sourcePath.equals(workingPath) || !sourcePath.startsWith(workingPath)) {
			return null;
		}
		String relativePath = sourcePath.substring(workingPath.length() + 1);
		return !relativePath.isEmpty() ? relativePath : null;
	}

	/**
	 * Return the source archive file that is running in tools mode.
	 * @return the archive file
	 */
	File getArchiveFile() {
		return this.archiveFile;
	}

	/**
	 * Return the current working directory.
	 * @return the working dir
	 */
	File getWorkingDir() {
		return this.workingDir;
	}

	/**
	 * Return the directory relative to {@link #getWorkingDir()} that contains the archive
	 * or {@code null} if none relative directory can be deduced.
	 * @return the relative dir ending in {@code /} or {@code null}
	 */
	String getRelativeArchiveDir() {
		return this.relativeDir;
	}

}
