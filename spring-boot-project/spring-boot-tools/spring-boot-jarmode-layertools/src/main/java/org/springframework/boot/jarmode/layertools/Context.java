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
import java.nio.file.Paths;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.util.Assert;

/**
 * Context for use by commands.
 *
 * @author Phillip Webb
 */
class Context {

	private final File jarFile;

	private final File workingDir;

	private String relativeDir;

	/**
	 * Create a new {@link Context} instance.
	 */
	Context() {
		this(new ApplicationHome().getSource(), Paths.get(".").toAbsolutePath().normalize().toFile());
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

	private String deduceRelativeDir(File sourceFolder, File workingDir) {
		String sourcePath = sourceFolder.getAbsolutePath();
		String workingPath = workingDir.getAbsolutePath();
		if (sourcePath.equals(workingPath) || !sourcePath.startsWith(workingPath)) {
			return null;
		}
		String relativePath = sourcePath.substring(workingPath.length() + 1);
		return (relativePath.length() > 0) ? relativePath : null;
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
