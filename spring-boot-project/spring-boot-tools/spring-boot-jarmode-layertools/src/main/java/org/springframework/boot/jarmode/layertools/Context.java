/*
 * Copyright 2012-2020 the original author or authors.
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

	private final File jarFile;

	private final File workingDir;

	private final String relativeDir;

	/**
	 * Create a new {@link Context} instance.
	 */
	Context() {
		this(getSourceJarFile(), Paths.get(".").toAbsolutePath().normalize().toFile());
	}

	/**
	 * Create a new {@link Context} instance with the specified value.
	 * @param jarFile the source jar file
	 * @param workingDir the working directory
	 */
	Context(File jarFile, File workingDir) {
		Assert.state(jarFile != null && jarFile.isFile() && jarFile.exists()
				&& jarFile.getName().toLowerCase().endsWith(".jar"), "Unable to find source JAR");
		this.jarFile = jarFile;
		this.workingDir = workingDir;
		this.relativeDir = deduceRelativeDir(jarFile.getParentFile(), this.workingDir);
	}

	private static File getSourceJarFile() {
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

	private static File findSource(URL location) throws IOException, URISyntaxException {
		URLConnection connection = location.openConnection();
		if (connection instanceof JarURLConnection) {
			return getRootJarFile(((JarURLConnection) connection).getJarFile());
		}
		return new File(location.toURI());
	}

	private static File getRootJarFile(JarFile jarFile) {
		String name = jarFile.getName();
		int separator = name.indexOf("!/");
		if (separator > 0) {
			name = name.substring(0, separator);
		}
		return new File(name);
	}

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
	 * Return the source jar file that is running in tools mode.
	 * @return the jar file
	 */
	File getJarFile() {
		return this.jarFile;
	}

	/**
	 * Return the current working directory.
	 * @return the working dir
	 */
	File getWorkingDir() {
		return this.workingDir;
	}

	/**
	 * Return the directory relative to {@link #getWorkingDir()} that contains the jar or
	 * {@code null} if none relative directory can be deduced.
	 * @return the relative dir ending in {@code /} or {@code null}
	 */
	String getRelativeJarDir() {
		return this.relativeDir;
	}

}
