/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * File utility methods
 * 
 * @author Andy Wilkinson
 */
public class FileUtils {

	private FileUtils() {

	}

	/**
	 * Recursively deletes the given {@code file} and all files beneath it.
	 * @param file The root of the structure to delete
	 * @throw IllegalStateException if the delete fails
	 */
	public static void recursiveDelete(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				for (File inDir : file.listFiles()) {
					recursiveDelete(inDir);
				}
			}
			if (!file.delete()) {
				throw new IllegalStateException("Failed to delete " + file);
			}
		}
	}

	/**
	 * Lists the given {@code file} and all the files beneath it.
	 * @param file The root of the structure to delete
	 * @return The list of files and directories
	 */
	public static List<File> recursiveList(File file) {
		List<File> files = new ArrayList<File>();
		if (file.isDirectory()) {
			for (File inDir : file.listFiles()) {
				files.addAll(recursiveList(inDir));
			}
		}
		files.add(file);
		return files;
	}

	/**
	 * Copies the data read from the given {@code source} {@link URL} to the given
	 * {@code target} {@link File}.
	 * @param source The source to copy from
	 * @param target The target to copy to
	 */
	public static void copy(URL source, File target) {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = source.openStream();
			output = new FileOutputStream(target);
			IoUtils.copy(input, output);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to copy '" + source + "' to '"
					+ target + "'", ex);
		}
		finally {
			IoUtils.closeQuietly(input, output);
		}
	}
}
