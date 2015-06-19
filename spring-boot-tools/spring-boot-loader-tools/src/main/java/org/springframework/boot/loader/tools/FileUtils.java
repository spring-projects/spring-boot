/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilities for manipulating files and directories in Spring Boot tooling.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public abstract class FileUtils {

	/**
	 * Utility to remove duplicate files from an "output" directory if they already exist
	 * in an "origin". Recursively scans the origin directory looking for files (not
	 * directories) that exist in both places and deleting the copy.
	 * @param outputDirectory the output directory
	 * @param originDirectory the origin directory
	 */
	public static void removeDuplicatesFromOutputDirectory(File outputDirectory,
			File originDirectory) {
		if (originDirectory.isDirectory()) {
			for (String name : originDirectory.list()) {
				File targetFile = new File(outputDirectory, name);
				if (targetFile.exists() && targetFile.canWrite()) {
					if (!targetFile.isDirectory()) {
						targetFile.delete();
					}
					else {
						FileUtils.removeDuplicatesFromOutputDirectory(targetFile,
								new File(originDirectory, name));
					}
				}
			}
		}
	}

	/**
	 * Generate a SHA.1 Hash for a given file.
	 * @param file the file to hash
	 * @return the hash value as a String
	 * @throws IOException if the file cannot be read
	 */
	public static String sha1Hash(File file) throws IOException {
		try {
			DigestInputStream inputStream = new DigestInputStream(new FileInputStream(
					file), MessageDigest.getInstance("SHA-1"));
			try {
				byte[] buffer = new byte[4098];
				while (inputStream.read(buffer) != -1) {
					// Read the entire stream
				}
				return bytesToHex(inputStream.getMessageDigest().digest());
			}
			finally {
				inputStream.close();
			}
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		for (byte b : bytes) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}
}
