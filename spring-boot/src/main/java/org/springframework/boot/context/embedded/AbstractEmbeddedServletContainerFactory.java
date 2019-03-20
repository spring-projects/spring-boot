/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ApplicationHome;
import org.springframework.boot.ApplicationTemp;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link EmbeddedServletContainerFactory} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AbstractEmbeddedServletContainerFactory
		extends AbstractConfigurableEmbeddedServletContainer
		implements EmbeddedServletContainerFactory {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final String[] COMMON_DOC_ROOTS = { "src/main/webapp", "public",
			"static" };

	public AbstractEmbeddedServletContainerFactory() {
		super();
	}

	public AbstractEmbeddedServletContainerFactory(int port) {
		super(port);
	}

	public AbstractEmbeddedServletContainerFactory(String contextPath, int port) {
		super(contextPath, port);
	}

	/**
	 * Returns the absolute document root when it points to a valid directory, logging a
	 * warning and returning {@code null} otherwise.
	 * @return the valid document root
	 */
	protected final File getValidDocumentRoot() {
		File file = getDocumentRoot();
		// If document root not explicitly set see if we are running from a war archive
		file = (file != null) ? file : getWarFileDocumentRoot();
		// If not a war archive maybe it is an exploded war
		file = (file != null) ? file : getExplodedWarFileDocumentRoot();
		// Or maybe there is a document root in a well-known location
		file = (file != null) ? file : getCommonDocumentRoot();
		if (file == null && this.logger.isDebugEnabled()) {
			this.logger
					.debug("None of the document roots " + Arrays.asList(COMMON_DOC_ROOTS)
							+ " point to a directory and will be ignored.");
		}
		else if (this.logger.isDebugEnabled()) {
			this.logger.debug("Document root: " + file);
		}
		return file;
	}

	private File getExplodedWarFileDocumentRoot() {
		return getExplodedWarFileDocumentRoot(getCodeSourceArchive());
	}

	protected List<URL> getUrlsOfJarsWithMetaInfResources() {
		ClassLoader classLoader = getClass().getClassLoader();
		List<URL> staticResourceUrls = new ArrayList<URL>();
		if (classLoader instanceof URLClassLoader) {
			for (URL url : ((URLClassLoader) classLoader).getURLs()) {
				if (isStaticResourceJar(url)) {
					staticResourceUrls.add(url);
				}
			}
		}
		return staticResourceUrls;
	}

	private boolean isStaticResourceJar(URL url) {
		try {
			if ("file".equals(url.getProtocol())) {
				File file = new File(url.toURI());
				return (file.isDirectory()
						&& new File(file, "META-INF/resources").isDirectory())
						|| isResourcesJar(file);
			}
			else {
				URLConnection connection = url.openConnection();
				if (connection instanceof JarURLConnection
						&& isResourcesJar((JarURLConnection) connection)) {
					return true;
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		return false;
	}

	/**
	 * Converts the given {@code url} into a decoded file path.
	 * @param url the url to convert
	 * @return the file path
	 * @deprecated Since 1.5.13 in favor of {@link File#File(java.net.URI)}
	 */
	@Deprecated
	protected final String getDecodedFile(URL url) {
		try {
			return URLDecoder.decode(url.getFile(), "UTF-8");
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(
					"Failed to decode '" + url.getFile() + "' using UTF-8");
		}
	}

	private boolean isResourcesJar(JarURLConnection connection) {
		try {
			return isResourcesJar(connection.getJarFile());
		}
		catch (IOException ex) {
			this.logger.warn("Unable to open jar from connection '" + connection
					+ "' to determine if it contains static resources", ex);
			return false;
		}
	}

	private boolean isResourcesJar(File file) {
		try {
			return file.getName().endsWith(".jar") && isResourcesJar(new JarFile(file));
		}
		catch (IOException ex) {
			this.logger.warn("Unable to open jar '" + file
					+ "' to determine if it contains static resources", ex);
			return false;
		}
	}

	private boolean isResourcesJar(JarFile jar) throws IOException {
		try {
			return jar.getJarEntry("META-INF/resources") != null;
		}
		finally {
			jar.close();
		}
	}

	File getExplodedWarFileDocumentRoot(File codeSourceFile) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Code archive: " + codeSourceFile);
		}
		if (codeSourceFile != null && codeSourceFile.exists()) {
			String path = codeSourceFile.getAbsolutePath();
			int webInfPathIndex = path
					.indexOf(File.separatorChar + "WEB-INF" + File.separatorChar);
			if (webInfPathIndex >= 0) {
				path = path.substring(0, webInfPathIndex);
				return new File(path);
			}
		}
		return null;
	}

	private File getWarFileDocumentRoot() {
		return getArchiveFileDocumentRoot(".war");
	}

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

	private File getCommonDocumentRoot() {
		for (String commonDocRoot : COMMON_DOC_ROOTS) {
			File root = new File(commonDocRoot);
			if (root.exists() && root.isDirectory()) {
				return root.getAbsoluteFile();
			}
		}
		return null;
	}

	private File getCodeSourceArchive() {
		return getCodeSourceArchive(getClass().getProtectionDomain().getCodeSource());
	}

	File getCodeSourceArchive(CodeSource codeSource) {
		try {
			URL location = (codeSource != null) ? codeSource.getLocation() : null;
			if (location == null) {
				return null;
			}
			String path;
			URLConnection connection = location.openConnection();
			if (connection instanceof JarURLConnection) {
				path = ((JarURLConnection) connection).getJarFile().getName();
			}
			else {
				path = location.toURI().getPath();
			}
			if (path.contains("!/")) {
				path = path.substring(0, path.indexOf("!/"));
			}
			return new File(path);
		}
		catch (Exception ex) {
			return null;
		}
	}

	protected final File getValidSessionStoreDir() {
		return getValidSessionStoreDir(true);
	}

	protected final File getValidSessionStoreDir(boolean mkdirs) {
		File dir = getSessionStoreDir();
		if (dir == null) {
			return new ApplicationTemp().getDir("servlet-sessions");
		}
		if (!dir.isAbsolute()) {
			dir = new File(new ApplicationHome().getDir(), dir.getPath());
		}
		if (!dir.exists() && mkdirs) {
			dir.mkdirs();
		}
		Assert.state(!mkdirs || dir.exists(), "Session dir " + dir + " does not exist");
		Assert.state(!dir.isFile(), "Session dir " + dir + " points to a file");
		return dir;
	}

	/**
	 * Returns the absolute temp dir for given servlet container.
	 * @param prefix servlet container name
	 * @return the temp dir for given servlet container.
	 */
	protected File createTempDir(String prefix) {
		try {
			File tempDir = File.createTempFile(prefix + ".", "." + getPort());
			tempDir.delete();
			tempDir.mkdir();
			tempDir.deleteOnExit();
			return tempDir;
		}
		catch (IOException ex) {
			throw new EmbeddedServletContainerException(
					"Unable to create tempDir. java.io.tmpdir is set to "
							+ System.getProperty("java.io.tmpdir"),
					ex);
		}
	}

}
