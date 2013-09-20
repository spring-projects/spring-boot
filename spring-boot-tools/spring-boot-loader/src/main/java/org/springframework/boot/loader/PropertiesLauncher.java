/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.springframework.boot.loader.util.SystemPropertyUtils;

/**
 * {@link AbstractLauncher} for archives with user-configured classpath and main class via
 * a properties file. This model is often more flexible and more amenable to creating
 * well-behaved OS-level services than a model based on executable jars.
 * 
 * <p>
 * Looks in various places for a properties file to extract loader settings, defaulting to
 * <code>application.properties</code> either on the current classpath or in the current
 * working directory. The name of the properties file can be changed by setting a System
 * property <code>loader.config.name</code> (e.g. <code>-Dloader.config.name=foo</code>
 * will look for <code>foo.properties</code>. If that file doesn't exist then tries
 * <code>loader.config.location</code> (with allowed prefixes <code>classpath:</code> and
 * <code>file:</code> or any valid URL). Once that file is located turns it into
 * Properties and extracts optional values (which can also be provided overridden as
 * System properties in case the file doesn't exist):
 * 
 * <ul>
 * <li><code>loader.path</code>: a comma-separated list of directories to append to the
 * classpath (containing file resources and/or nested archives in *.jar or *.zip).
 * Defaults to <code>lib</code> (i.e. a directory in the current working directory)</li>
 * <li><code>loader.main</code>: the main method to delegate execution to once the class
 * loader is set up. No default, but will fall back to looking for a
 * <code>Start-Class</code> in a <code>MANIFEST.MF</code>, if there is one in
 * <code>${loader.home}/META-INF</code>.</li>
 * </ul>
 * 
 * @author Dave Syer
 */
public class PropertiesLauncher implements ArchiveFilter {

	private Logger logger = Logger.getLogger(AbstractLauncher.class.getName());

	/**
	 * Properties key for main class
	 */
	public static final String MAIN = "loader.main";

	/**
	 * Properties key for classpath entries (directories possibly containing jars)
	 */
	public static final String PATH = "loader.path";

	public static final String HOME = "loader.home";

	public static final String CONFIG_NAME = "loader.config.name";

	public static final String CONFIG_LOCATION = "loader.config.location";

	private static final List<String> DEFAULT_PATHS = Arrays.asList("lib/");

	private List<String> paths = new ArrayList<String>(DEFAULT_PATHS);

	private Properties properties = new Properties();

	private LaunchHelper helper = new LaunchHelper();

	public static void main(String[] args) {
		new PropertiesLauncher().launch(args);
	}

	/**
	 * Launch the application. This method is the initial entry point that should be
	 * called by a subclass {@code public static void main(String[] args)} method.
	 * @param args the incoming arguments
	 */
	public void launch(String[] args) {
		try {
			File home = getHomeDirectory();
			initialize(home);
			this.helper.launch(args, getMainClass(home), getLibrary(home, this.paths));
		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public boolean isArchive(Archive.Entry entry) {
		return entry.isDirectory() || isArchive(entry.getName());
	}

	protected File getHomeDirectory() {
		return new File(SystemPropertyUtils.resolvePlaceholders(System.getProperty(HOME,
				"${user.dir}")));
	}

	protected String getMainClass(File home) throws Exception {
		if (System.getProperty(MAIN) != null) {
			return SystemPropertyUtils.resolvePlaceholders(System.getProperty(MAIN));
		}
		if (this.properties.containsKey(MAIN)) {
			return SystemPropertyUtils.resolvePlaceholders(this.properties
					.getProperty(MAIN));
		}
		return this.helper.getMainClass(new ExplodedArchive(home));
	}

	protected void initialize(File home) throws Exception {
		initializeProperties(home);
		initializePaths();
	}

	private boolean isArchive(String name) {
		return name.endsWith(".jar") || name.endsWith(".zip");
	}

	/**
	 * Search the configured paths and look for nested archives.
	 * 
	 * @param home the home directory for this launch
	 * @param paths the directory roots for classpath entries
	 * @return a library of archives that can be used as a classpath
	 * @throws Exception
	 */
	private List<Archive> getLibrary(File home, List<String> paths) throws Exception {
		List<Archive> lib = new ArrayList<Archive>();
		for (String path : paths) {
			String root = cleanupPath(stripFileUrlPrefix(path));
			File file = new File(root);
			if (!root.startsWith("/")) {
				file = new File(home, root);
			}
			if (file.isDirectory()) {
				this.logger.info("Adding classpath entries from " + path);
				Archive archive = new ExplodedArchive(file);
				lib.addAll(this.helper.findNestedArchives(archive, this));
				lib.add(0, archive);
			}
			else {
				this.logger.info("No directory found at " + path);
			}
		}
		return lib;
	}

	private void initializeProperties(File home) throws Exception, IOException {
		String config = SystemPropertyUtils.resolvePlaceholders(System.getProperty(
				CONFIG_NAME, "application")) + ".properties";
		InputStream resource = getClasspathResource(config);
		if (resource == null) {
			resource = getResource(new File(home, config).getAbsolutePath());
		}
		if (resource == null) {
			config = SystemPropertyUtils.resolvePlaceholders(System.getProperty(
					CONFIG_LOCATION, config));
			resource = getResource(config);
		}

		if (resource != null) {
			this.logger.info("Found: " + config);
			try {
				this.properties.load(resource);
			}
			finally {
				resource.close();
			}
		}
		else {
			this.logger.info("Not found: " + config);
		}
	}

	private InputStream getResource(String config) throws Exception {
		if (config.startsWith("classpath:")) {
			return getClasspathResource(config.substring("classpath:".length()));
		}
		config = stripFileUrlPrefix(config);
		if (isUrl(config)) {
			return getURLResource(config);
		}
		return getFileResource(config);
	}

	private String stripFileUrlPrefix(String config) {
		if (config.startsWith("file:")) {
			config = config.substring("file:".length());
			if (config.startsWith("//")) {
				config = config.substring(2);
			}
		}
		return config;
	}

	private boolean isUrl(String config) {
		return config.contains("://");
	}

	private InputStream getClasspathResource(String config) {
		while (config.startsWith("/")) {
			config = config.substring(1);
		}
		config = "/" + config;
		this.logger.fine("Trying classpath: " + config);
		return getClass().getResourceAsStream(config);
	}

	private InputStream getFileResource(String config) throws Exception {
		File file = new File(config);
		this.logger.fine("Trying file: " + config);
		if (file.canRead()) {
			return new FileInputStream(file);
		}
		return null;
	}

	private InputStream getURLResource(String config) throws Exception {
		URL url = new URL(config);
		if (exists(url)) {
			URLConnection con = url.openConnection();
			try {
				return con.getInputStream();
			}
			catch (IOException ex) {
				// Close the HTTP connection (if applicable).
				if (con instanceof HttpURLConnection) {
					((HttpURLConnection) con).disconnect();
				}
				throw ex;
			}
		}
		return null;
	}

	private boolean exists(URL url) throws IOException {
		// Try a URL connection content-length header...
		URLConnection connection = url.openConnection();
		try {
			connection.setUseCaches(connection.getClass().getSimpleName()
					.startsWith("JNLP"));
			if (connection instanceof HttpURLConnection) {
				HttpURLConnection httpConnection = (HttpURLConnection) connection;
				httpConnection.setRequestMethod("HEAD");
				int responseCode = httpConnection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					return true;
				}
				else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
					return false;
				}
			}
			return (connection.getContentLength() >= 0);
		}
		finally {
			if (connection instanceof HttpURLConnection) {
				((HttpURLConnection) connection).disconnect();
			}
		}
	}

	private void initializePaths() throws IOException {
		String path = System.getProperty(PATH);
		if (path == null) {
			path = this.properties.getProperty(PATH);
		}
		if (path != null) {
			this.paths = parsePathsProperty(SystemPropertyUtils.resolvePlaceholders(path));
		}
		this.logger.info("Nested archive paths: " + this.paths);
	}

	private List<String> parsePathsProperty(String commaSeparatedPaths) {
		List<String> paths = new ArrayList<String>();
		for (String path : commaSeparatedPaths.split(",")) {
			path = cleanupPath(path);
			// Empty path is always on the classpath so no need for it to be explicitly
			// listed here
			if (!(path.equals(".") || path.equals(""))) {
				paths.add(path);
			}
		}
		return paths;
	}

	private String cleanupPath(String path) {
		path = path.trim();
		// Always a directory
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		// No need for current dir path
		if (path.startsWith("./")) {
			path = path.substring(2);
		}
		return path;
	}

}
