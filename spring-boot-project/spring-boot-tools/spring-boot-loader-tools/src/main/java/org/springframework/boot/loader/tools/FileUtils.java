/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Utilities for manipulating files and directories in Spring Boot tooling.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.0.0
 */
public abstract class FileUtils {

	/**
	 * Utility to remove duplicate files from an "output" directory if they already exist
	 * in an "origin". Recursively scans the origin directory looking for files (not
	 * directories) that exist in both places and deleting the copy.
	 * @param outputDirectory the output directory
	 * @param originDirectory the origin directory
	 */
	public static void removeDuplicatesFromOutputDirectory(File outputDirectory, File originDirectory) {
		if (originDirectory.isDirectory()) {
			for (String name : originDirectory.list()) {
				File targetFile = new File(outputDirectory, name);
				if (targetFile.exists() && targetFile.canWrite()) {
					if (!targetFile.isDirectory()) {
						targetFile.delete();
					}
					else {
						FileUtils.removeDuplicatesFromOutputDirectory(targetFile, new File(originDirectory, name));
					}
				}
			}
		}
	}

	/**
	 * Generate a SHA-1 Hash for a given file.
	 * @param file the file to hash
	 * @return the hash value as a String
	 * @throws IOException if the file cannot be read
	 */
	public static String sha1Hash(File file) throws IOException {
		return Digest.sha1(InputStreamSupplier.forFile(file));
	}

	/**
	 * Returns {@code true} if the given jar file has been signed.
	 * @param file the file to check
	 * @return if the file has been signed
	 * @throws IOException on IO error
	 */
	public static boolean isSignedJarFile(File file) throws IOException {
		try (JarFile jarFile = new JarFile(file)) {
			if (hasDigestEntry(jarFile.getManifest())) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasDigestEntry(Manifest manifest) {
		return (manifest != null) && manifest.getEntries().values().stream().anyMatch(FileUtils::hasDigestName);
	}

	private static boolean hasDigestName(Attributes attributes) {
		return attributes.keySet().stream().anyMatch(FileUtils::isDigestName);
	}

	private static boolean isDigestName(Object name) {
		return String.valueOf(name).toUpperCase(Locale.ROOT).endsWith("-DIGEST");
	}

}
