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

package org.springframework.boot.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.archive.Archive.EntryFilter;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.FilteredArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.util.SystemPropertyUtils;

/**
 * {@link Launcher} for archives with user-configured classpath and main class via a
 * properties file. This model is often more flexible and more amenable to creating
 * well-behaved OS-level services than a model based on executable jars.
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
 * @author Janne Valkealahti
 */
public class PropertiesLauncher extends Launcher {

	private final Logger logger = Logger.getLogger(Launcher.class.getName());

	/**
	 * Properties key for main class
	 */
	public static final String MAIN = "loader.main";

	/**
	 * Properties key for classpath entries (directories possibly containing jars).
	 * Defaults to "lib/" (relative to {@link #HOME loader home directory}).
	 */
	public static final String PATH = "loader.path";

	/**
	 * Properties key for home directory. This is the location of external configuration
	 * if not on classpath, and also the base path for any relative paths in the
	 * {@link #PATH loader path}. Defaults to current working directory (
	 * <code>${user.home}</code>).
	 */
	public static final String HOME = "loader.home";

	/**
	 * Properties key for name of external configuration file (excluding suffix). Defaults
	 * to "application". Ignored if {@link #CONFIG_LOCATION loader config location} is
	 * provided instead.
	 */
	public static final String CONFIG_NAME = "loader.config.name";

	/**
	 * Properties key for config file location (including optional classpath:, file: or
	 * URL prefix)
	 */
	public static final String CONFIG_LOCATION = "loader.config.location";

	/**
	 * Properties key for boolean flag (default false) which if set will cause the
	 * external configuration properties to be copied to System properties (assuming that
	 * is allowed by Java security).
	 */
	public static final String SET_SYSTEM_PROPERTIES = "loader.system";

	private static final List<String> DEFAULT_PATHS = Arrays.asList("lib/");

	private static final Pattern WORD_SEPARATOR = Pattern.compile("\\W+");

	private final File home;

	private List<String> paths = new ArrayList<String>(DEFAULT_PATHS);

	private final Properties properties = new Properties();

	public PropertiesLauncher() {
		if (!isDebug()) {
			this.logger.setLevel(Level.SEVERE);
		}
		try {
			this.home = getHomeDirectory();
			initializeProperties(this.home);
			initializePaths();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private boolean isDebug() {
		String debug = System.getProperty("debug");
		if (debug != null && !"false".equals(debug)) {
			return true;
		}
		debug = System.getProperty("DEBUG");
		if (debug != null && !"false".equals(debug)) {
			return true;
		}
		debug = System.getenv("DEBUG");
		if (debug != null && !"false".equals(debug)) {
			return true;
		}
		return false;
	}

	protected File getHomeDirectory() {
		return new File(SystemPropertyUtils.resolvePlaceholders(System.getProperty(HOME,
				"${user.dir}")));
	}

	private void initializeProperties(File home) throws Exception, IOException {
		String config = "classpath:"
				+ SystemPropertyUtils.resolvePlaceholders(SystemPropertyUtils
						.getProperty(CONFIG_NAME, "application")) + ".properties";
		config = SystemPropertyUtils.resolvePlaceholders(SystemPropertyUtils.getProperty(
				CONFIG_LOCATION, config));
		InputStream resource = getResource(config);

		if (resource != null) {
			this.logger.info("Found: " + config);
			try {
				this.properties.load(resource);
			}
			finally {
				resource.close();
			}
			for (Object key : Collections.list(this.properties.propertyNames())) {
				String text = this.properties.getProperty((String) key);
				String value = SystemPropertyUtils.resolvePlaceholders(this.properties,
						text);
				if (value != null) {
					this.properties.put(key, value);
				}
			}
			if (SystemPropertyUtils.resolvePlaceholders(
					"${" + SET_SYSTEM_PROPERTIES + ":false}").equals("true")) {
				this.logger.info("Adding resolved properties to System properties");
				for (Object key : Collections.list(this.properties.propertyNames())) {
					String value = this.properties.getProperty((String) key);
					System.setProperty((String) key, value);
				}
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
		String path = SystemPropertyUtils.getProperty(PATH);
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

	@Override
	protected String getMainClass() throws Exception {
		String mainClass = getProperty(MAIN, "Start-Class");
		if (mainClass == null) {
			throw new IllegalStateException("No '" + MAIN
					+ "' or 'Start-Class' specified");
		}
		return mainClass;
	}

	@Override
	protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
		ClassLoader loader = super.createClassLoader(archives);
		String customLoaderClassName = getProperty("loader.classLoader");
		if (customLoaderClassName != null) {
			loader = wrapWithCustomClassLoader(loader, customLoaderClassName);
			this.logger.info("Using custom class loader: " + customLoaderClassName);
		}
		return loader;
	}

	@SuppressWarnings("unchecked")
	private ClassLoader wrapWithCustomClassLoader(ClassLoader parent,
			String loaderClassName) throws Exception {

		Class<ClassLoader> loaderClass = (Class<ClassLoader>) Class.forName(
				loaderClassName, true, parent);

		try {
			return loaderClass.getConstructor(ClassLoader.class).newInstance(parent);
		}
		catch (NoSuchMethodException ex) {
			// Ignore and try with URLs
		}

		try {
			return loaderClass.getConstructor(URL[].class, ClassLoader.class)
					.newInstance(new URL[0], parent);
		}
		catch (NoSuchMethodException ex) {
			// Ignore and try without any arguments
		}

		return loaderClass.newInstance();
	}

	private String getProperty(String propertyKey) throws Exception {
		return getProperty(propertyKey, null);
	}

	private String getProperty(String propertyKey, String manifestKey) throws Exception {
		if (manifestKey == null) {
			manifestKey = propertyKey.replace(".", "-");
			manifestKey = toCamelCase(manifestKey);
		}

		String property = SystemPropertyUtils.getProperty(propertyKey);
		if (property != null) {
			String value = SystemPropertyUtils.resolvePlaceholders(property);
			this.logger.fine("Property '" + propertyKey + "' from environment: " + value);
			return value;
		}

		if (this.properties.containsKey(propertyKey)) {
			String value = SystemPropertyUtils.resolvePlaceholders(this.properties
					.getProperty(propertyKey));
			this.logger.fine("Property '" + propertyKey + "' from properties: " + value);
			return value;
		}

		try {
			// Prefer home dir for MANIFEST if there is one
			Manifest manifest = new ExplodedArchive(this.home).getManifest();
			if (manifest != null) {
				String value = manifest.getMainAttributes().getValue(manifestKey);
				this.logger.fine("Property '" + manifestKey
						+ "' from home directory manifest: " + value);
				return value;
			}
		}
		catch (IllegalStateException ex) {
			// Ignore
		}

		// Otherwise try the parent archive
		Manifest manifest = createArchive().getManifest();
		if (manifest != null) {
			String value = manifest.getMainAttributes().getValue(manifestKey);
			if (value != null) {
				this.logger.fine("Property '" + manifestKey + "' from archive manifest: "
						+ value);
				return value;
			}
		}
		return null;
	}

	@Override
	protected List<Archive> getClassPathArchives() throws Exception {
		List<Archive> lib = new ArrayList<Archive>();
		for (String path : this.paths) {
			for (Archive archive : getClassPathArchives(path)) {
				List<Archive> nested = new ArrayList<Archive>(
						archive.getNestedArchives(new ArchiveEntryFilter()));
				nested.add(0, archive);
				lib.addAll(nested);
			}
		}
		addParentClassLoaderEntries(lib);
		return lib;
	}

	private List<Archive> getClassPathArchives(String path) throws Exception {
		String root = cleanupPath(stripFileUrlPrefix(path));
		List<Archive> lib = new ArrayList<Archive>();
		File file = new File(root);
		if (!root.startsWith("/")) {
			file = new File(this.home, root);
		}
		if (file.isDirectory()) {
			this.logger.info("Adding classpath entries from " + file);
			Archive archive = new ExplodedArchive(file);
			lib.add(archive);
		}
		Archive archive = getArchive(file);
		if (archive != null) {
			this.logger.info("Adding classpath entries from nested " + archive.getUrl()
					+ root);
			lib.add(archive);
		}
		Archive nested = getNestedArchive(root);
		if (nested != null) {
			this.logger.info("Adding classpath entries from nested " + nested.getUrl()
					+ root);
			lib.add(nested);
		}
		return lib;
	}

	private Archive getArchive(File file) throws IOException {
		String name = file.getName().toLowerCase();
		if (name.endsWith(".jar") || name.endsWith(".zip")) {
			return new JarFileArchive(file);
		}
		return null;
	}

	private Archive getNestedArchive(final String root) throws Exception {
		Archive parent = createArchive();
		if (root.startsWith("/") || parent.getUrl().equals(this.home.toURI().toURL())) {
			// If home dir is same as parent archive, no need to add it twice.
			return null;
		}
		EntryFilter filter = new PrefixMatchingArchiveFilter(root);
		if (parent.getNestedArchives(filter).isEmpty()) {
			return null;
		}
		// If there are more archives nested in this subdirectory (root) then create a new
		// virtual archive for them, and have it added to the classpath
		return new FilteredArchive(parent, filter);
	}

	private Archive createArchive() throws Exception {
		ProtectionDomain protectionDomain = getClass().getProtectionDomain();
		CodeSource codeSource = protectionDomain.getCodeSource();
		URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
		String path = (location == null ? null : location.getPath());
		if (path == null) {
			throw new IllegalStateException("Unable to determine code source archive");
		}
		File root = new File(path);
		if (!root.exists()) {
			throw new IllegalStateException(
					"Unable to determine code source archive from " + root);
		}
		return (root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root));
	}

	private void addParentClassLoaderEntries(List<Archive> lib) throws IOException,
			URISyntaxException {
		ClassLoader parentClassLoader = getClass().getClassLoader();
		if (parentClassLoader instanceof URLClassLoader) {
			URLClassLoader urlClassLoader = (URLClassLoader) parentClassLoader;
			for (URL url : urlClassLoader.getURLs()) {
				if (url.toString().endsWith(".jar") || url.toString().endsWith(".zip")) {
					lib.add(0, new JarFileArchive(new File(url.toURI())));
				}
				else if (url.toString().endsWith("/*")) {
					String name = url.getFile();
					File dir = new File(name.substring(0, name.length() - 1));
					if (dir.exists()) {
						lib.add(0,
								new ExplodedArchive(new File(name.substring(0,
										name.length() - 1)), false));
					}
				}
				else {
					lib.add(0, new ExplodedArchive(new File(url.getFile())));
				}
			}
		}
	}

	private String cleanupPath(String path) {
		path = path.trim();
		if (path.toLowerCase().endsWith(".jar") || path.toLowerCase().endsWith(".zip")) {
			return path;
		}
		if (path.endsWith("/*")) {
			path = path.substring(0, path.length() - 1);
		}
		else {
			// It's a directory
			if (!path.endsWith("/")) {
				path = path + "/";
			}
		}
		// No need for current dir path
		if (path.startsWith("./")) {
			path = path.substring(2);
		}
		return path;
	}

	public static void main(String[] args) {
		new PropertiesLauncher().launch(args);
	}

	public static String toCamelCase(CharSequence string) {
		if (string == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		Matcher matcher = WORD_SEPARATOR.matcher(string);
		int pos = 0;
		while (matcher.find()) {
			builder.append(capitalize(string.subSequence(pos, matcher.end()).toString()));
			pos = matcher.end();
		}
		builder.append(capitalize(string.subSequence(pos, string.length()).toString()));
		return builder.toString();
	}

	private static Object capitalize(String str) {
		StringBuilder sb = new StringBuilder(str.length());
		sb.append(Character.toUpperCase(str.charAt(0)));
		sb.append(str.substring(1));
		return sb.toString();
	}

	/**
	 * Convenience class for finding nested archives (archive entries that can be
	 * classpath entries).
	 */
	private static final class ArchiveEntryFilter implements EntryFilter {

		private static final AsciiBytes DOT_JAR = new AsciiBytes(".jar");

		private static final AsciiBytes DOT_ZIP = new AsciiBytes(".zip");

		@Override
		public boolean matches(Entry entry) {
			return entry.isDirectory() || entry.getName().endsWith(DOT_JAR)
					|| entry.getName().endsWith(DOT_ZIP);
		}
	}

	/**
	 * Convenience class for finding nested archives that have a prefix in their file path
	 * (e.g. "lib/").
	 */
	private static final class PrefixMatchingArchiveFilter implements EntryFilter {

		private final AsciiBytes prefix;

		private final ArchiveEntryFilter filter = new ArchiveEntryFilter();

		private PrefixMatchingArchiveFilter(String prefix) {
			this.prefix = new AsciiBytes(prefix);
		}

		@Override
		public boolean matches(Entry entry) {
			return entry.getName().startsWith(this.prefix) && this.filter.matches(entry);
		}
	}

}
