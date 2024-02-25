/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.loader.launch.Archive.Entry;
import org.springframework.boot.loader.log.DebugLogger;
import org.springframework.boot.loader.net.protocol.jar.JarUrl;

/**
 * {@link Launcher} for archives with user-configured classpath and main class through a
 * properties file.
 * <p>
 * Looks in various places for a properties file to extract loader settings, defaulting to
 * {@code loader.properties} either on the current classpath or in the current working
 * directory. The name of the properties file can be changed by setting a System property
 * {@code loader.config.name} (e.g. {@code -Dloader.config.name=my} will look for
 * {@code my.properties}. If that file doesn't exist then tries
 * {@code loader.config.location} (with allowed prefixes {@code classpath:} and
 * {@code file:} or any valid URL). Once that file is located turns it into Properties and
 * extracts optional values (which can also be provided overridden as System properties in
 * case the file doesn't exist):
 * <ul>
 * <li>{@code loader.path}: a comma-separated list of directories (containing file
 * resources and/or nested archives in *.jar or *.zip or archives) or archives to append
 * to the classpath. {@code BOOT-INF/classes,BOOT-INF/lib} in the application archive are
 * always used</li>
 * <li>{@code loader.main}: the main method to delegate execution to once the class loader
 * is set up. No default, but will fall back to looking for a {@code Start-Class} in a
 * {@code MANIFEST.MF}, if there is one in <code>${loader.home}/META-INF</code>.</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Janne Valkealahti
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.2.0
 */
public class PropertiesLauncher extends Launcher {

	/**
	 * Properties key for main class. As a manifest entry can also be specified as
	 * {@code Start-Class}.
	 */
	public static final String MAIN = "loader.main";

	/**
	 * Properties key for classpath entries (directories possibly containing jars or
	 * jars). Multiple entries can be specified using a comma-separated list. {@code
	 * BOOT-INF/classes,BOOT-INF/lib} in the application archive are always used.
	 */
	public static final String PATH = "loader.path";

	/**
	 * Properties key for home directory. This is the location of external configuration
	 * if not on classpath, and also the base path for any relative paths in the
	 * {@link #PATH loader path}. Defaults to current working directory (
	 * <code>${user.dir}</code>).
	 */
	public static final String HOME = "loader.home";

	/**
	 * Properties key for default command line arguments. These arguments (if present) are
	 * prepended to the main method arguments before launching.
	 */
	public static final String ARGS = "loader.args";

	/**
	 * Properties key for name of external configuration file (excluding suffix). Defaults
	 * to "application". Ignored if {@link #CONFIG_LOCATION loader config location} is
	 * provided instead.
	 */
	public static final String CONFIG_NAME = "loader.config.name";

	/**
	 * Properties key for config file location (including optional classpath:, file: or
	 * URL prefix).
	 */
	public static final String CONFIG_LOCATION = "loader.config.location";

	/**
	 * Properties key for boolean flag (default false) which, if set, will cause the
	 * external configuration properties to be copied to System properties (assuming that
	 * is allowed by Java security).
	 */
	public static final String SET_SYSTEM_PROPERTIES = "loader.system";

	private static final URL[] NO_URLS = new URL[0];

	private static final Pattern WORD_SEPARATOR = Pattern.compile("\\W+");

	private static final String NESTED_ARCHIVE_SEPARATOR = "!" + File.separator;

	private static final String JAR_FILE_PREFIX = "jar:file:";

	private static final DebugLogger debug = DebugLogger.get(PropertiesLauncher.class);

	private final Archive archive;

	private final File homeDirectory;

	private final List<String> paths;

	private final Properties properties = new Properties();

	/**
	 * Constructs a new instance of PropertiesLauncher.
	 * @throws Exception if an error occurs during the construction of the instance
	 */
	public PropertiesLauncher() throws Exception {
		this(Archive.create(Launcher.class));
	}

	/**
	 * Constructs a new PropertiesLauncher with the specified Archive.
	 * @param archive the Archive to be used by the PropertiesLauncher
	 * @throws Exception if an error occurs during initialization
	 */
	PropertiesLauncher(Archive archive) throws Exception {
		this.archive = archive;
		this.homeDirectory = getHomeDirectory();
		initializeProperties();
		this.paths = getPaths();
	}

	/**
	 * Returns the home directory as a File object.
	 * @return the home directory as a File object
	 * @throws Exception if an error occurs while retrieving the home directory
	 */
	protected File getHomeDirectory() throws Exception {
		return new File(getPropertyWithDefault(HOME, "${user.dir}"));
	}

	/**
	 * Initializes the properties for the PropertiesLauncher.
	 * @throws Exception if an error occurs during initialization
	 */
	private void initializeProperties() throws Exception {
		List<String> configs = new ArrayList<>();
		if (getProperty(CONFIG_LOCATION) != null) {
			configs.add(getProperty(CONFIG_LOCATION));
		}
		else {
			String[] names = getPropertyWithDefault(CONFIG_NAME, "loader").split(",");
			for (String name : names) {
				String propertiesFile = name + ".properties";
				configs.add("file:" + this.homeDirectory + "/" + propertiesFile);
				configs.add("classpath:" + propertiesFile);
				configs.add("classpath:BOOT-INF/classes/" + propertiesFile);
			}
		}
		for (String config : configs) {
			try (InputStream resource = getResource(config)) {
				if (resource == null) {
					debug.log("Not found: %s", config);
					continue;
				}
				debug.log("Found: %s", config);
				loadResource(resource);
				return; // Load the first one we find
			}
		}
	}

	/**
	 * Retrieves the resource specified by the given configuration string.
	 * @param config the configuration string specifying the resource
	 * @return an InputStream representing the resource
	 * @throws Exception if an error occurs while retrieving the resource
	 */
	private InputStream getResource(String config) throws Exception {
		if (config.startsWith("classpath:")) {
			return getClasspathResource(config.substring("classpath:".length()));
		}
		config = handleUrl(config);
		if (isUrl(config)) {
			return getURLResource(config);
		}
		return getFileResource(config);
	}

	/**
	 * Retrieves an input stream for a resource located in the classpath.
	 * @param config the path of the resource to retrieve
	 * @return an input stream for the specified resource, or null if the resource is not
	 * found
	 */
	private InputStream getClasspathResource(String config) {
		config = stripLeadingSlashes(config);
		config = "/" + config;
		debug.log("Trying classpath: %s", config);
		return getClass().getResourceAsStream(config);
	}

	/**
	 * Handles the given URL path.
	 * @param path the URL path to be handled
	 * @return the processed URL path
	 */
	private String handleUrl(String path) {
		if (path.startsWith("jar:file:") || path.startsWith("file:")) {
			path = URLDecoder.decode(path, StandardCharsets.UTF_8);
			if (path.startsWith("file:")) {
				path = path.substring("file:".length());
				if (path.startsWith("//")) {
					path = path.substring(2);
				}
			}
		}
		return path;
	}

	/**
	 * Checks if the given configuration string is a valid URL.
	 * @param config the configuration string to be checked
	 * @return true if the configuration string is a valid URL, false otherwise
	 */
	private boolean isUrl(String config) {
		return config.contains("://");
	}

	/**
	 * Retrieves the resource from the specified URL.
	 * @param config the URL of the resource to retrieve
	 * @return an InputStream representing the resource
	 * @throws Exception if an error occurs while retrieving the resource
	 */
	private InputStream getURLResource(String config) throws Exception {
		URL url = new URL(config);
		if (exists(url)) {
			URLConnection connection = url.openConnection();
			try {
				return connection.getInputStream();
			}
			catch (IOException ex) {
				disconnect(connection);
				throw ex;
			}
		}
		return null;
	}

	/**
	 * Checks if a given URL exists by making a HEAD request and checking the response
	 * code.
	 * @param url the URL to check
	 * @return true if the URL exists, false otherwise
	 * @throws IOException if an I/O error occurs while making the request
	 */
	private boolean exists(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		try {
			connection.setUseCaches(connection.getClass().getSimpleName().startsWith("JNLP"));
			if (connection instanceof HttpURLConnection httpConnection) {
				httpConnection.setRequestMethod("HEAD");
				int responseCode = httpConnection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					return true;
				}
				if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
					return false;
				}
			}
			return (connection.getContentLength() >= 0);
		}
		finally {
			disconnect(connection);
		}
	}

	/**
	 * Disconnects the given URL connection.
	 * @param connection the URL connection to disconnect
	 */
	private void disconnect(URLConnection connection) {
		if (connection instanceof HttpURLConnection httpConnection) {
			httpConnection.disconnect();
		}
	}

	/**
	 * Retrieves the input stream of a file resource based on the provided configuration.
	 * @param config the path to the file resource
	 * @return the input stream of the file resource
	 * @throws Exception if an error occurs while retrieving the file resource
	 */
	private InputStream getFileResource(String config) throws Exception {
		File file = new File(config);
		debug.log("Trying file: %s", config);
		return (!file.canRead()) ? null : new FileInputStream(file);
	}

	/**
	 * Loads a resource from an input stream and populates the properties object.
	 * @param resource the input stream of the resource to be loaded
	 * @throws Exception if an error occurs while loading the resource
	 */
	private void loadResource(InputStream resource) throws Exception {
		this.properties.load(resource);
		resolvePropertyPlaceholders();
		if ("true".equalsIgnoreCase(getProperty(SET_SYSTEM_PROPERTIES))) {
			addToSystemProperties();
		}
	}

	/**
	 * Resolves property placeholders in the properties object.
	 *
	 * This method iterates over each property in the properties object and resolves any
	 * placeholders present in the property value. The resolved value is then updated in
	 * the properties object.
	 * @throws NullPointerException if the properties object is null
	 */
	private void resolvePropertyPlaceholders() {
		for (String name : this.properties.stringPropertyNames()) {
			String value = this.properties.getProperty(name);
			String resolved = SystemPropertyUtils.resolvePlaceholders(this.properties, value);
			if (resolved != null) {
				this.properties.put(name, resolved);
			}
		}
	}

	/**
	 * Adds the resolved properties to the System properties.
	 *
	 * This method iterates over the properties stored in the instance variable
	 * 'properties' and adds each property to the System properties using the
	 * System.setProperty() method.
	 * @throws NullPointerException if the 'properties' instance variable is null.
	 *
	 * @see PropertiesLauncher
	 */
	private void addToSystemProperties() {
		debug.log("Adding resolved properties to System properties");
		for (String name : this.properties.stringPropertyNames()) {
			String value = this.properties.getProperty(name);
			System.setProperty(name, value);
		}
	}

	/**
	 * Retrieves the paths from the property file.
	 * @return a list of paths
	 * @throws Exception if an error occurs while retrieving the paths
	 */
	private List<String> getPaths() throws Exception {
		String path = getProperty(PATH);
		List<String> paths = (path != null) ? parsePathsProperty(path) : Collections.emptyList();
		debug.log("Nested archive paths: %s", this.paths);
		return paths;
	}

	/**
	 * Parses the comma-separated paths property and returns a list of paths.
	 * @param commaSeparatedPaths the comma-separated paths property
	 * @return a list of paths
	 */
	private List<String> parsePathsProperty(String commaSeparatedPaths) {
		List<String> paths = new ArrayList<>();
		for (String path : commaSeparatedPaths.split(",")) {
			path = cleanupPath(path);
			// "" means the user wants root of archive but not current directory
			path = (path.isEmpty()) ? "/" : path;
			paths.add(path);
		}
		if (paths.isEmpty()) {
			paths.add("lib");
		}
		return paths;
	}

	/**
	 * Cleans up the given path by removing leading and trailing whitespaces. If the path
	 * starts with "./", it removes the "./" prefix. If the path is an archive, it returns
	 * the path as is. If the path ends with "/*", it removes the "/*" suffix. If the path
	 * is a directory and does not end with "/", it appends "/" to the path.
	 * @param path the path to be cleaned up
	 * @return the cleaned up path
	 */
	private String cleanupPath(String path) {
		path = path.trim();
		// No need for current dir path
		if (path.startsWith("./")) {
			path = path.substring(2);
		}
		if (isArchive(path)) {
			return path;
		}
		if (path.endsWith("/*")) {
			return path.substring(0, path.length() - 1);
		}
		// It's a directory
		return (!path.endsWith("/") && !path.equals(".")) ? path + "/" : path;
	}

	/**
	 * Creates a custom class loader based on the provided URLs. If a loader class name is
	 * specified in the system properties, it wraps the class loader with the specified
	 * custom class loader. Otherwise, it creates a class loader using the default
	 * implementation.
	 * @param urls The collection of URLs to be used by the class loader.
	 * @return The created class loader.
	 * @throws Exception If an error occurs while creating the class loader.
	 */
	@Override
	protected ClassLoader createClassLoader(Collection<URL> urls) throws Exception {
		String loaderClassName = getProperty("loader.classLoader");
		if (loaderClassName == null) {
			return super.createClassLoader(urls);
		}
		ClassLoader parent = getClass().getClassLoader();
		ClassLoader classLoader = new LaunchedClassLoader(false, urls.toArray(new URL[0]), parent);
		debug.log("Classpath for custom loader: %s", urls);
		classLoader = wrapWithCustomClassLoader(classLoader, loaderClassName);
		debug.log("Using custom class loader: %s", loaderClassName);
		return classLoader;
	}

	/**
	 * Wraps the given parent class loader with a custom class loader specified by the
	 * provided loader class name.
	 * @param parent The parent class loader to be wrapped.
	 * @param loaderClassName The fully qualified name of the custom class loader.
	 * @return The wrapped custom class loader.
	 * @throws Exception If an error occurs while creating the custom class loader.
	 * @throws IllegalStateException If the custom class loader cannot be created.
	 */
	private ClassLoader wrapWithCustomClassLoader(ClassLoader parent, String loaderClassName) throws Exception {
		Instantiator<ClassLoader> instantiator = new Instantiator<>(parent, loaderClassName);
		ClassLoader loader = instantiator.declaredConstructor(ClassLoader.class).newInstance(parent);
		loader = (loader != null) ? loader
				: instantiator.declaredConstructor(URL[].class, ClassLoader.class).newInstance(NO_URLS, parent);
		loader = (loader != null) ? loader : instantiator.constructWithoutParameters();
		if (loader != null) {
			return loader;
		}
		throw new IllegalStateException("Unable to create class loader for " + loaderClassName);
	}

	/**
	 * Returns the archive for the PropertiesLauncher. Since we don't have a single
	 * archive and are not exploded, this method returns null.
	 * @return null - since there is no single archive and not exploded.
	 */
	@Override
	protected Archive getArchive() {
		return null; // We don't have a single archive and are not exploded.
	}

	/**
	 * Returns the main class to be executed.
	 * @return the main class
	 * @throws Exception if no 'Main' or 'Start-Class' property is specified
	 */
	@Override
	protected String getMainClass() throws Exception {
		String mainClass = getProperty(MAIN, "Start-Class");
		if (mainClass == null) {
			throw new IllegalStateException("No '%s' or 'Start-Class' specified".formatted(MAIN));
		}
		return mainClass;
	}

	/**
	 * Retrieves the command line arguments for the application.
	 * @param args the additional command line arguments passed to the method
	 * @return an array of command line arguments
	 * @throws Exception if an error occurs while retrieving the arguments
	 */
	protected String[] getArgs(String... args) throws Exception {
		String loaderArgs = getProperty(ARGS);
		return (loaderArgs != null) ? merge(loaderArgs.split("\\s+"), args) : args;
	}

	/**
	 * Merges two arrays of strings into a single array.
	 * @param a1 the first array of strings
	 * @param a2 the second array of strings
	 * @return the merged array of strings
	 */
	private String[] merge(String[] a1, String[] a2) {
		String[] result = new String[a1.length + a2.length];
		System.arraycopy(a1, 0, result, 0, a1.length);
		System.arraycopy(a2, 0, result, a1.length, a2.length);
		return result;
	}

	/**
	 * Retrieves the value of the specified property.
	 * @param name the name of the property to retrieve
	 * @return the value of the property
	 * @throws Exception if an error occurs while retrieving the property
	 */
	private String getProperty(String name) throws Exception {
		return getProperty(name, null, null);
	}

	/**
	 * Retrieves the value of a property from the manifest file.
	 * @param name the name of the property to retrieve
	 * @param manifestKey the key in the manifest file associated with the property
	 * @return the value of the property, or null if not found
	 * @throws Exception if an error occurs while retrieving the property
	 */
	private String getProperty(String name, String manifestKey) throws Exception {
		return getProperty(name, manifestKey, null);
	}

	/**
	 * Retrieves the value of the property with the given name from the properties file.
	 * If the property is not found, the default value is returned.
	 * @param name the name of the property to retrieve
	 * @param defaultValue the default value to return if the property is not found
	 * @return the value of the property if found, otherwise the default value
	 * @throws Exception if an error occurs while retrieving the property
	 */
	private String getPropertyWithDefault(String name, String defaultValue) throws Exception {
		return getProperty(name, null, defaultValue);
	}

	/**
	 * Retrieves the value of a property based on the given name, manifest key, and
	 * default value.
	 * @param name the name of the property
	 * @param manifestKey the key in the manifest file associated with the property
	 * @param defaultValue the default value to be returned if the property is not found
	 * @return the resolved value of the property
	 * @throws Exception if an error occurs while retrieving the property
	 */
	private String getProperty(String name, String manifestKey, String defaultValue) throws Exception {
		manifestKey = (manifestKey != null) ? manifestKey : toCamelCase(name.replace('.', '-'));
		String value = SystemPropertyUtils.getProperty(name);
		if (value != null) {
			return getResolvedProperty(name, manifestKey, value, "environment");
		}
		if (this.properties.containsKey(name)) {
			value = this.properties.getProperty(name);
			return getResolvedProperty(name, manifestKey, value, "properties");
		}
		// Prefer home dir for MANIFEST if there is one
		if (this.homeDirectory != null) {
			try {
				try (ExplodedArchive explodedArchive = new ExplodedArchive(this.homeDirectory)) {
					value = getManifestValue(explodedArchive, manifestKey);
					if (value != null) {
						return getResolvedProperty(name, manifestKey, value, "home directory manifest");
					}
				}
			}
			catch (IllegalStateException ex) {
				// Ignore
			}
		}
		// Otherwise try the root archive
		value = getManifestValue(this.archive, manifestKey);
		if (value != null) {
			return getResolvedProperty(name, manifestKey, value, "manifest");
		}
		return SystemPropertyUtils.resolvePlaceholders(this.properties, defaultValue);
	}

	/**
	 * Retrieves the value associated with the specified manifest key from the given
	 * archive.
	 * @param archive The archive from which to retrieve the manifest.
	 * @param manifestKey The key of the value to retrieve from the manifest.
	 * @return The value associated with the specified manifest key, or null if the
	 * manifest or the key is not found.
	 * @throws Exception If an error occurs while retrieving the manifest or the value.
	 */
	String getManifestValue(Archive archive, String manifestKey) throws Exception {
		Manifest manifest = archive.getManifest();
		return (manifest != null) ? manifest.getMainAttributes().getValue(manifestKey) : null;
	}

	/**
	 * Resolves the value of a property by replacing any placeholders with their
	 * corresponding values from the properties map.
	 * @param name the name of the property
	 * @param manifestKey the key in the manifest file (optional)
	 * @param value the value of the property
	 * @param from the source of the property value
	 * @return the resolved value of the property
	 */
	private String getResolvedProperty(String name, String manifestKey, String value, String from) {
		value = SystemPropertyUtils.resolvePlaceholders(this.properties, value);
		String altName = (manifestKey != null && !manifestKey.equals(name)) ? "[%s] ".formatted(manifestKey) : "";
		debug.log("Property '%s'%s from %s: %s", name, altName, from, value);
		return value;

	}

	/**
	 * Closes the archive if it is not null.
	 * @throws Exception if an error occurs while closing the archive.
	 */
	void close() throws Exception {
		if (this.archive != null) {
			this.archive.close();
		}
	}

	/**
	 * Converts a given string to camel case.
	 * @param string the string to be converted
	 * @return the camel case representation of the given string
	 */
	public static String toCamelCase(CharSequence string) {
		if (string == null) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		Matcher matcher = WORD_SEPARATOR.matcher(string);
		int pos = 0;
		while (matcher.find()) {
			result.append(capitalize(string.subSequence(pos, matcher.end()).toString()));
			pos = matcher.end();
		}
		result.append(capitalize(string.subSequence(pos, string.length()).toString()));
		return result.toString();
	}

	/**
	 * Capitalizes the first letter of a given string.
	 * @param str the string to be capitalized
	 * @return the capitalized string
	 */
	private static String capitalize(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	/**
	 * Returns a set of URLs representing the classpath.
	 * @return the set of URLs representing the classpath
	 * @throws Exception if an error occurs while retrieving the classpath URLs
	 */
	@Override
	protected Set<URL> getClassPathUrls() throws Exception {
		Set<URL> urls = new LinkedHashSet<>();
		for (String path : getPaths()) {
			path = cleanupPath(handleUrl(path));
			urls.addAll(getClassPathUrlsForPath(path));
		}
		urls.addAll(getClassPathUrlsForRoot());
		debug.log("Using class path URLs %s", urls);
		return urls;
	}

	/**
	 * Retrieves the classpath URLs for a given path.
	 * @param path The path to retrieve classpath URLs from.
	 * @return A set of classpath URLs.
	 * @throws Exception If an error occurs while retrieving the classpath URLs.
	 */
	private Set<URL> getClassPathUrlsForPath(String path) throws Exception {
		File file = (!isAbsolutePath(path)) ? new File(this.homeDirectory, path) : new File(path);
		Set<URL> urls = new LinkedHashSet<>();
		if (!"/".equals(path)) {
			if (file.isDirectory()) {
				try (ExplodedArchive explodedArchive = new ExplodedArchive(file)) {
					debug.log("Adding classpath entries from directory %s", file);
					urls.add(file.toURI().toURL());
					urls.addAll(explodedArchive.getClassPathUrls(this::isArchive));
				}
			}
		}
		if (!file.getPath().contains(NESTED_ARCHIVE_SEPARATOR) && isArchive(file.getName())) {
			debug.log("Adding classpath entries from jar/zip archive %s", path);
			urls.add(file.toURI().toURL());
		}
		Set<URL> nested = getClassPathUrlsForNested(path);
		if (!nested.isEmpty()) {
			debug.log("Adding classpath entries from nested %s", path);
			urls.addAll(nested);
		}
		return urls;
	}

	/**
	 * Returns a set of URLs representing the classpath for a nested path.
	 * @param path the nested path
	 * @return a set of URLs representing the classpath for the nested path
	 * @throws Exception if an error occurs while retrieving the classpath URLs
	 */
	private Set<URL> getClassPathUrlsForNested(String path) throws Exception {
		boolean isJustArchive = isArchive(path);
		if (!path.equals("/") && path.startsWith("/")
				|| (this.archive.isExploded() && this.archive.getRootDirectory().equals(this.homeDirectory))) {
			return Collections.emptySet();
		}
		File file = null;
		if (isJustArchive) {
			File candidate = new File(this.homeDirectory, path);
			if (candidate.exists()) {
				file = candidate;
				path = "";
			}
		}
		int separatorIndex = path.indexOf('!');
		if (separatorIndex != -1) {
			file = (!path.startsWith(JAR_FILE_PREFIX)) ? new File(this.homeDirectory, path.substring(0, separatorIndex))
					: new File(path.substring(JAR_FILE_PREFIX.length(), separatorIndex));
			path = path.substring(separatorIndex + 1);
			path = stripLeadingSlashes(path);
		}
		if (path.equals("/") || path.equals("./") || path.equals(".")) {
			// The prefix for nested jars is actually empty if it's at the root
			path = "";
		}
		Archive archive = (file != null) ? new JarFileArchive(file) : this.archive;
		try {
			Set<URL> urls = new LinkedHashSet<>(archive.getClassPathUrls(includeByPrefix(path)));
			if (!isJustArchive && file != null && path.isEmpty()) {
				urls.add(JarUrl.create(file));
			}
			return urls;
		}
		finally {
			if (archive != this.archive) {
				archive.close();
			}
		}
	}

	/**
	 * Retrieves the classpath URLs for the root archive.
	 * @return A set of URLs representing the classpath entries from the root archive.
	 * @throws IOException if an I/O error occurs while retrieving the classpath URLs.
	 */
	private Set<URL> getClassPathUrlsForRoot() throws IOException {
		debug.log("Adding classpath entries from root archive %s", this.archive);
		return this.archive.getClassPathUrls(JarLauncher::isLibraryFileOrClassesDirectory);
	}

	/**
	 * Returns a Predicate that includes entries based on the given prefix. The Predicate
	 * includes entries that are directories and have the same name as the prefix, or
	 * entries that are archives and have a name that starts with the prefix.
	 * @param prefix the prefix to filter entries by
	 * @return a Predicate that includes entries based on the given prefix
	 */
	private Predicate<Entry> includeByPrefix(String prefix) {
		return (entry) -> (entry.isDirectory() && entry.name().equals(prefix))
				|| (isArchive(entry) && entry.name().startsWith(prefix));
	}

	/**
	 * Determines if the given entry is an archive.
	 * @param entry the entry to check
	 * @return true if the entry is an archive, false otherwise
	 */
	private boolean isArchive(Entry entry) {
		return isArchive(entry.name());
	}

	/**
	 * Checks if the given file name is an archive file.
	 * @param name the name of the file to be checked
	 * @return true if the file is an archive file, false otherwise
	 */
	private boolean isArchive(String name) {
		name = name.toLowerCase(Locale.ENGLISH);
		return name.endsWith(".jar") || name.endsWith(".zip");
	}

	/**
	 * Checks if the given root path is an absolute path.
	 * @param root the root path to be checked
	 * @return true if the root path is an absolute path, false otherwise
	 */
	private boolean isAbsolutePath(String root) {
		// Windows contains ":" others start with "/"
		return root.contains(":") || root.startsWith("/");
	}

	/**
	 * Removes leading slashes from the given string.
	 * @param string the string to remove leading slashes from
	 * @return the string without leading slashes
	 */
	private String stripLeadingSlashes(String string) {
		while (string.startsWith("/")) {
			string = string.substring(1);
		}
		return string;
	}

	/**
	 * The main method of the PropertiesLauncher class.
	 * @param args the command line arguments passed to the main method
	 * @throws Exception if an error occurs during the execution of the main method
	 */
	public static void main(String[] args) throws Exception {
		PropertiesLauncher launcher = new PropertiesLauncher();
		args = launcher.getArgs(args);
		launcher.launch(args);
	}

	/**
	 * Utility to help instantiate objects.
	 */
	private record Instantiator<T>(ClassLoader parent, Class<?> type) {

		/**
		 * Constructs a new instance of the PropertiesLauncher class with the specified
		 * parent ClassLoader and className.
		 * @param parent the parent ClassLoader to be used for loading the class
		 * @param className the fully qualified name of the class to be instantiated
		 * @throws ClassNotFoundException if the class with the specified className cannot
		 * be found
		 */
		Instantiator(ClassLoader parent, String className) throws ClassNotFoundException {
			this(parent, Class.forName(className, true, parent));
		}

		/**
		 * Constructs a new instance of the PropertiesLauncher class without any
		 * parameters.
		 * @return a new instance of the PropertiesLauncher class
		 * @throws Exception if an error occurs during the construction of the instance
		 */
		T constructWithoutParameters() throws Exception {
			return declaredConstructor().newInstance();
		}

		/**
		 * Returns a new instance of the Using class with the specified parameter types.
		 * @param parameterTypes the parameter types for the constructor of the Using
		 * class
		 * @return a new instance of the Using class
		 */
		Using<T> declaredConstructor(Class<?>... parameterTypes) {
			return new Using<>(this, parameterTypes);
		}

		private record Using<T>(Instantiator<T> instantiator, Class<?>... parameterTypes) {

			/**
			 * Creates a new instance of the specified type using the provided
			 * initialization arguments.
			 * @param initargs the initialization arguments for the new instance
			 * @return a new instance of the specified type
			 * @throws Exception if an error occurs during the instantiation process
			 */
			@SuppressWarnings("unchecked")
			T newInstance(Object... initargs) throws Exception {
				try {
					Constructor<?> constructor = this.instantiator.type().getDeclaredConstructor(this.parameterTypes);
					constructor.setAccessible(true);
					return (T) constructor.newInstance(initargs);
				}
				catch (NoSuchMethodException ex) {
					return null;
				}
			}

		}
	}

}
