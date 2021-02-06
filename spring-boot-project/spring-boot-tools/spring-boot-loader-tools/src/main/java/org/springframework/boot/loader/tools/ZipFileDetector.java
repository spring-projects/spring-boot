/*
 * Copyright 2012-2021 the original author or authors.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Detects zip files by examining their header.
 *
 * @author Phil Clay
 * @since 2.5.0
 */
public class ZipFileDetector {

	private static final byte[] ZIP_FILE_HEADER = new byte[] { 'P', 'K', 3, 4 };

	/**
	 * Returns true if the given file is a zip file (as identified by the file header).
	 * @param file the file to inspect
	 * @return true if the given file is a zip file (as identified by the file header).
	 */
	public boolean isZip(File file) {
		try {
			try (FileInputStream fileInputStream = new FileInputStream(file)) {
				return isZip(fileInputStream);
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * Returns true if the given inputStream is a zip file (as identified by the file
	 * header). Consumes up to 4 bytes of the inputStream.
	 * @param inputStream the inputStream to inspect
	 * @return true if the given inputStream is a zip file (as identified by the file
	 * header).
	 */
	public boolean isZip(InputStream inputStream) {
		try {
			for (byte headerByte : ZIP_FILE_HEADER) {
				if (inputStream.read() != headerByte) {
					return false;
				}
			}
			return true;
		}
		catch (IOException ex) {
			return false;
		}
	}

}
