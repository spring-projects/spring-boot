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

/**
 * Utilities for manipulating files and directories in Spring Boot tooling.
 * 
 * @author Dave Syer
 */
public class FileUtils {

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

}
