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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.logging.Log;

/**
 * Manages a {@link ServletWebServerFactory} document root.
 *
 * @author Phillip Webb
 * @see AbstractServletWebServerFactory
 */
class DocumentRoot {

	private static final String[] COMMON_DOC_ROOTS = { "src/main/webapp", "public", "static" };

	private final Log logger;

	private File directory;

	/**
	 * Sets the logger for the DocumentRoot class.
	 * @param logger the logger to be set
	 */
	DocumentRoot(Log logger) {
		this.logger = logger;
	}

	/**
	 * Returns the directory associated with the DocumentRoot.
	 * @return the directory associated with the DocumentRoot
	 */
	File getDirectory() {
		return this.directory;
	}

	/**
	 * Sets the directory for the DocumentRoot.
	 * @param directory the directory to set
	 */
	void setDirectory(File directory) {
		this.directory = directory;
	}

	/**
	 * Returns the absolute document root when it points to a valid directory, logging a
	 * warning and returning {@code null} otherwise.
	 * @return the valid document root
	 */
	final File getValidDirectory() {
		File file = this.directory;
		file = (file != null) ? file : getWarFileDocumentRoot();
		file = (file != null) ? file : getExplodedWarFileDocumentRoot();
		file = (file != null) ? file : getCommonDocumentRoot();
		if (file == null && this.logger.isDebugEnabled()) {
			logNoDocumentRoots();
		}
		else if (this.logger.isDebugEnabled()) {
			this.logger.debug("Document root: " + file);
		}
		return file;
	}

	/**
	 * Returns the document root file for a WAR file.
	 * @return the document root file for a WAR file
	 */
	private File getWarFileDocumentRoot() {
		return getArchiveFileDocumentRoot(".war");
	}

	/**
	 * Retrieves the archive file from the document root based on the given extension.
	 * @param extension The file extension to filter the archive file.
	 * @return The archive file from the document root if found, otherwise null.
	 */
	private File getArchiveFileDocumentRoot(String extension) {
		File file = getCodeSourceArchive();
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Code archive: " + file);
		}
		if (file != null && file.exists() && !file.isDirectory()
				&& file.getName().toLowerCase(Locale.ENGLISH).endsWith(extension)) {
			return file.getAbsoluteFile();
		}
		return null;
	}

	/**
	 * Returns the document root of the exploded WAR file.
	 * @return the document root of the exploded WAR file
	 */
	private File getExplodedWarFileDocumentRoot() {
		return getExplodedWarFileDocumentRoot(getCodeSourceArchive());
	}

	/**
	 * Returns the code source archive file associated with the current class.
	 * @return the code source archive file
	 */
	private File getCodeSourceArchive() {
		return getCodeSourceArchive(getClass().getProtectionDomain().getCodeSource());
	}

	/**
	 * Retrieves the source archive file for the given CodeSource.
	 * @param codeSource the CodeSource object representing the source location
	 * @return the File object representing the source archive file, or null if not found
	 */
	File getCodeSourceArchive(CodeSource codeSource) {
		try {
			URL location = (codeSource != null) ? codeSource.getLocation() : null;
			if (location == null) {
				return null;
			}
			String path;
			URLConnection connection = location.openConnection();
			if (connection instanceof JarURLConnection jarURLConnection) {
				path = jarURLConnection.getJarFile().getName();
			}
			else {
				path = location.toURI().getPath();
			}
			int index = path.indexOf("!/");
			if (index != -1) {
				path = path.substring(0, index);
			}
			return new File(path);
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Returns the document root directory of the exploded WAR file.
	 * @param codeSourceFile the code source file of the exploded WAR file
	 * @return the document root directory as a File object, or null if the code source
	 * file is null or does not exist
	 */
	final File getExplodedWarFileDocumentRoot(File codeSourceFile) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Code archive: " + codeSourceFile);
		}
		if (codeSourceFile != null && codeSourceFile.exists()) {
			String path = codeSourceFile.getAbsolutePath();
			int webInfPathIndex = path.indexOf(File.separatorChar + "WEB-INF" + File.separatorChar);
			if (webInfPathIndex >= 0) {
				path = path.substring(0, webInfPathIndex);
				return new File(path);
			}
		}
		return null;
	}

	/**
	 * Returns the common document root directory.
	 *
	 * This method checks a list of common document root paths and returns the first one
	 * that exists and is a directory.
	 * @return The common document root directory as a File object, or null if no valid
	 * root directory is found.
	 */
	private File getCommonDocumentRoot() {
		for (String commonDocRoot : COMMON_DOC_ROOTS) {
			File root = new File(commonDocRoot);
			if (root.exists() && root.isDirectory()) {
				return root.getAbsoluteFile();
			}
		}
		return null;
	}

	/**
	 * Logs a message indicating that none of the document roots point to a directory and
	 * will be ignored.
	 * @param none
	 * @return void
	 */
	private void logNoDocumentRoots() {
		this.logger.debug("None of the document roots " + Arrays.asList(COMMON_DOC_ROOTS)
				+ " point to a directory and will be ignored.");
	}

}
