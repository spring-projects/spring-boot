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

package org.springframework.boot.loader;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.archive.Archive.EntryFilter;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.util.SystemPropertyUtils;

/**
 * {@link Launcher} for archives with user-configured classpath and main class through a
 * properties file. This model is often more flexible and more amenable to creating
 * well-behaved OS-level services than a model based on executable jars.
 * <p>
 * Looks in various places for a properties file to extract loader settings, defaulting to
 * {@code loader.properties} either on the current classpath or in the current working
 * directory. The name of the properties file can be changed by setting a System property
 * {@code loader.config.name} (e.g. {@code -Dloader.config.name=foo} will look for
 * {@code foo.properties}. If that file doesn't exist then tries
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
 * @since 1.0.0
 */
public class PropertiesLauncher extends Launcher {

	private static final Class<?>[] PARENT_ONLY_PARAMS = new Class<?>[] { ClassLoader.class };

	private static final Class<?>[] URLS_AND_PARENT_PARAMS = new Class<?>[] { URL[].class, ClassLoader.class };

	private static final Class<?>[] NO_PARAMS = new Class<?>[] {};

	private static final URL[] NO_URLS = new URL[0];

	private static final String DEBUG = "loader.debug";

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

	private static final Pattern WORD_SEPARATOR = Pattern.compile("\\W+");

	private static final String NESTED_ARCHIVE_SEPARATOR = "!" + File.separator;

	private final File home;

	private List<String> paths = new ArrayList<>();

	private final Properties properties = new Properties();

	private final Archive parent;

	private volatile ClassPathArchives classPathArchives;

	/**
	 * Constructs a new instance of the PropertiesLauncher class.
	 *
	 * This constructor initializes the home directory, properties, paths, and parent
	 * archive of the PropertiesLauncher. If any exception occurs during the
	 * initialization process, an IllegalStateException is thrown with the exception as
	 * the cause.
	 * @throws IllegalStateException if an exception occurs during the initialization
	 * process
	 */
	public PropertiesLauncher() {
		try {
			this.home = getHomeDirectory();
			initializeProperties();
			initializePaths();
			this.parent = createArchive();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Returns the home directory as a File object.
	 * @return the home directory as a File object
	 * @throws IllegalStateException if an exception occurs while retrieving the home
	 * directory
	 */
	protected File getHomeDirectory() {
		try {
			return new File(getPropertyWithDefault(HOME, "${user.dir}"));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
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
				configs.add("file:" + getHomeDirectory() + "/" + name + ".properties");
				configs.add("classpath:" + name + ".properties");
				configs.add("classpath:BOOT-INF/classes/" + name + ".properties");
			}
		}
		for (String config : configs) {
			try (InputStream resource = getResource(config)) {
				if (resource != null) {
					debug("Found: " + config);
					loadResource(resource);
					// Load the first one we find
					return;
				}
				else {
					debug("Not found: " + config);
				}
			}
		}
	}

	/**
	 * Loads the resource from the given input stream and populates the properties.
	 * Resolves placeholders in property values using SystemPropertyUtils. If the
	 * "SET_SYSTEM_PROPERTIES" property is set to "true", adds the resolved properties to
	 * System properties.
	 * @param resource the input stream of the resource to load
	 * @throws Exception if an error occurs while loading the resource or resolving
	 * placeholders
	 */
	private void loadResource(InputStream resource) throws Exception {
		this.properties.load(resource);
		for (Object key : Collections.list(this.properties.propertyNames())) {
			String text = this.properties.getProperty((String) key);
			String value = SystemPropertyUtils.resolvePlaceholders(this.properties, text);
			if (value != null) {
				this.properties.put(key, value);
			}
		}
		if ("true".equals(getProperty(SET_SYSTEM_PROPERTIES))) {
			debug("Adding resolved properties to System properties");
			for (Object key : Collections.list(this.properties.propertyNames())) {
				String value = this.properties.getProperty((String) key);
				System.setProperty((String) key, value);
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
	 * Retrieves an input stream for a resource located in the classpath.
	 * @param config the path of the resource to retrieve
	 * @return an input stream for the specified resource, or null if the resource is not
	 * found
	 */
	private InputStream getClasspathResource(String config) {
		while (config.startsWith("/")) {
			config = config.substring(1);
		}
		config = "/" + config;
		debug("Trying classpath: " + config);
		return getClass().getResourceAsStream(config);
	}

	/**
	 * Retrieves the input stream of a file resource based on the provided configuration.
	 * @param config the path to the file resource
	 * @return the input stream of the file resource, or null if the file cannot be read
	 * @throws Exception if an error occurs while retrieving the file resource
	 */
	private InputStream getFileResource(String config) throws Exception {
		File file = new File(config);
		debug("Trying file: " + config);
		if (file.canRead()) {
			return new FileInputStream(file);
		}
		return null;
	}

	/**
	 * Retrieves the resource from the specified URL.
	 * @param config the URL of the resource to retrieve
	 * @return an InputStream representing the resource content
	 * @throws Exception if an error occurs while retrieving the resource
	 */
	private InputStream getURLResource(String config) throws Exception {
		URL url = new URL(config);
		if (exists(url)) {
			URLConnection con = url.openConnection();
			try {
				return con.getInputStream();
			}
			catch (IOException ex) {
				// Close the HTTP connection (if applicable).
				if (con instanceof HttpURLConnection httpURLConnection) {
					httpURLConnection.disconnect();
				}
				throw ex;
			}
		}
		return null;
	}

	/**
	 * Checks if a given URL exists by making a HEAD request and checking the response
	 * code. If the response code is HTTP_OK, returns true. If the response code is
	 * HTTP_NOT_FOUND, returns false. If the URL connection does not support HTTP, checks
	 * the content length of the connection.
	 * @param url the URL to check for existence
	 * @return true if the URL exists, false otherwise
	 * @throws IOException if an I/O error occurs while making the request
	 */
	private boolean exists(URL url) throws IOException {
		// Try a URL connection content-length header...
		URLConnection connection = url.openConnection();
		try {
			connection.setUseCaches(connection.getClass().getSimpleName().startsWith("JNLP"));
			if (connection instanceof HttpURLConnection httpConnection) {
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
			if (connection instanceof HttpURLConnection httpURLConnection) {
				httpURLConnection.disconnect();
			}
		}
	}

	/**
	 * Initializes the paths for nested archives.
	 * @throws Exception if an error occurs during initialization
	 */
	private void initializePaths() throws Exception {
		String path = getProperty(PATH);
		if (path != null) {
			this.paths = parsePathsProperty(path);
		}
		debug("Nested archive paths: " + this.paths);
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
			path = (path == null || path.isEmpty()) ? "/" : path;
			paths.add(path);
		}
		if (paths.isEmpty()) {
			paths.add("lib");
		}
		return paths;
	}

	/**
	 * Retrieves the command line arguments for the application.
	 * @param args the additional command line arguments provided by the user
	 * @return an array of command line arguments
	 * @throws Exception if an error occurs while retrieving the command line arguments
	 */
	protected String[] getArgs(String... args) throws Exception {
		String loaderArgs = getProperty(ARGS);
		if (loaderArgs != null) {
			String[] defaultArgs = loaderArgs.split("\\s+");
			String[] additionalArgs = args;
			args = new String[defaultArgs.length + additionalArgs.length];
			System.arraycopy(defaultArgs, 0, args, 0, defaultArgs.length);
			System.arraycopy(additionalArgs, 0, args, defaultArgs.length, additionalArgs.length);
		}
		return args;
	}

	/**
	 * Returns the main class to be executed.
	 * @return the main class
	 * @throws Exception if no main class is specified
	 */
	@Override
	protected String getMainClass() throws Exception {
		String mainClass = getProperty(MAIN, "Start-Class");
		if (mainClass == null) {
			throw new IllegalStateException("No '" + MAIN + "' or 'Start-Class' specified");
		}
		return mainClass;
	}

	/**
	 * Creates a custom class loader based on the provided archives. If a custom loader
	 * class name is specified in the system properties, it creates an instance of that
	 * class and wraps it around the default class loader. Otherwise, it uses the default
	 * class loader.
	 * @param archives An iterator of archives containing the URLs to be loaded by the
	 * class loader.
	 * @return The created class loader.
	 * @throws Exception If an error occurs while creating the class loader.
	 */
	@Override
	protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
		String customLoaderClassName = getProperty("loader.classLoader");
		if (customLoaderClassName == null) {
			return super.createClassLoader(archives);
		}
		Set<URL> urls = new LinkedHashSet<>();
		while (archives.hasNext()) {
			urls.add(archives.next().getUrl());
		}
		ClassLoader loader = new LaunchedURLClassLoader(urls.toArray(NO_URLS), getClass().getClassLoader());
		debug("Classpath for custom loader: " + urls);
		loader = wrapWithCustomClassLoader(loader, customLoaderClassName);
		debug("Using custom class loader: " + customLoaderClassName);
		return loader;
	}

	/**
	 * Wraps the given parent class loader with a custom class loader based on the
	 * provided class name.
	 * @param parent the parent class loader
	 * @param className the name of the class to be used as the custom class loader
	 * @return the custom class loader
	 * @throws Exception if an error occurs while creating the custom class loader
	 * @throws IllegalArgumentException if the custom class loader cannot be created for
	 * the given class name
	 */
	@SuppressWarnings("unchecked")
	private ClassLoader wrapWithCustomClassLoader(ClassLoader parent, String className) throws Exception {
		Class<ClassLoader> type = (Class<ClassLoader>) Class.forName(className, true, parent);
		ClassLoader classLoader = newClassLoader(type, PARENT_ONLY_PARAMS, parent);
		if (classLoader == null) {
			classLoader = newClassLoader(type, URLS_AND_PARENT_PARAMS, NO_URLS, parent);
		}
		if (classLoader == null) {
			classLoader = newClassLoader(type, NO_PARAMS);
		}
		if (classLoader == null) {
			throw new IllegalArgumentException("Unable to create class loader for " + className);
		}
		return classLoader;
	}

	/**
	 * Creates a new instance of a ClassLoader using the provided loaderClass,
	 * parameterTypes, and initargs.
	 * @param loaderClass the class of the ClassLoader to be instantiated
	 * @param parameterTypes the types of the parameters required by the ClassLoader
	 * constructor
	 * @param initargs the arguments to be passed to the ClassLoader constructor
	 * @return a new instance of the ClassLoader
	 * @throws Exception if an error occurs during the instantiation of the ClassLoader
	 */
	private ClassLoader newClassLoader(Class<ClassLoader> loaderClass, Class<?>[] parameterTypes, Object... initargs)
			throws Exception {
		try {
			Constructor<ClassLoader> constructor = loaderClass.getDeclaredConstructor(parameterTypes);
			constructor.setAccessible(true);
			return constructor.newInstance(initargs);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * Retrieves the value of the specified property key from the properties file.
	 * @param propertyKey the key of the property to retrieve
	 * @return the value of the property
	 * @throws Exception if an error occurs while retrieving the property
	 */
	private String getProperty(String propertyKey) throws Exception {
		return getProperty(propertyKey, null, null);
	}

	/**
	 * Retrieves the value of a property from the manifest file using the provided
	 * property key and manifest key. If the property key is not found in the manifest
	 * file, the default value is returned.
	 * @param propertyKey the key of the property to retrieve
	 * @param manifestKey the key in the manifest file where the property is located
	 * @return the value of the property if found, otherwise the default value
	 * @throws Exception if an error occurs while retrieving the property
	 */
	private String getProperty(String propertyKey, String manifestKey) throws Exception {
		return getProperty(propertyKey, manifestKey, null);
	}

	/**
	 * Retrieves the value of the specified property key from the properties file. If the
	 * property key is not found, the default value is returned.
	 * @param propertyKey the key of the property to retrieve
	 * @param defaultValue the default value to return if the property key is not found
	 * @return the value of the property key, or the default value if not found
	 * @throws Exception if an error occurs while retrieving the property
	 */
	private String getPropertyWithDefault(String propertyKey, String defaultValue) throws Exception {
		return getProperty(propertyKey, null, defaultValue);
	}

	/**
	 * Retrieves the value of a property based on the provided property key, manifest key,
	 * and default value.
	 * @param propertyKey the key of the property to retrieve
	 * @param manifestKey the key of the property in the manifest file (can be null)
	 * @param defaultValue the default value to return if the property is not found
	 * @return the value of the property, or the default value if not found
	 * @throws Exception if an error occurs while retrieving the property
	 */
	private String getProperty(String propertyKey, String manifestKey, String defaultValue) throws Exception {
		if (manifestKey == null) {
			manifestKey = propertyKey.replace('.', '-');
			manifestKey = toCamelCase(manifestKey);
		}
		String property = SystemPropertyUtils.getProperty(propertyKey);
		if (property != null) {
			String value = SystemPropertyUtils.resolvePlaceholders(this.properties, property);
			debug("Property '" + propertyKey + "' from environment: " + value);
			return value;
		}
		if (this.properties.containsKey(propertyKey)) {
			String value = SystemPropertyUtils.resolvePlaceholders(this.properties,
					this.properties.getProperty(propertyKey));
			debug("Property '" + propertyKey + "' from properties: " + value);
			return value;
		}
		try {
			if (this.home != null) {
				// Prefer home dir for MANIFEST if there is one
				try (ExplodedArchive archive = new ExplodedArchive(this.home, false)) {
					Manifest manifest = archive.getManifest();
					if (manifest != null) {
						String value = manifest.getMainAttributes().getValue(manifestKey);
						if (value != null) {
							debug("Property '" + manifestKey + "' from home directory manifest: " + value);
							return SystemPropertyUtils.resolvePlaceholders(this.properties, value);
						}
					}
				}
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
				debug("Property '" + manifestKey + "' from archive manifest: " + value);
				return SystemPropertyUtils.resolvePlaceholders(this.properties, value);
			}
		}
		return (defaultValue != null) ? SystemPropertyUtils.resolvePlaceholders(this.properties, defaultValue)
				: defaultValue;
	}

	/**
	 * Returns an iterator over the class path archives.
	 * @return an iterator over the class path archives
	 * @throws Exception if an error occurs while retrieving the class path archives
	 */
	@Override
	protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
		ClassPathArchives classPathArchives = this.classPathArchives;
		if (classPathArchives == null) {
			classPathArchives = new ClassPathArchives();
			this.classPathArchives = classPathArchives;
		}
		return classPathArchives.iterator();
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
	 * Converts a string to camel case.
	 * @param string the string to be converted
	 * @return the camel case version of the string
	 */
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

	/**
	 * Capitalizes the first letter of a given string.
	 * @param str the string to be capitalized
	 * @return the capitalized string
	 */
	private static String capitalize(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	/**
	 * Prints the debug message if the DEBUG flag is set to true.
	 * @param message the debug message to be printed
	 */
	private void debug(String message) {
		if (Boolean.getBoolean(DEBUG)) {
			System.out.println(message);
		}
	}

	/**
	 * Cleans up the given path by removing leading and trailing whitespaces. If the path
	 * starts with "./", it removes the "./" prefix. If the path ends with ".jar" or
	 * ".zip", it returns the path as is. If the path ends with "/*", it removes the "/*"
	 * suffix. If the path is a directory and does not end with "/", it appends "/" to the
	 * path.
	 * @param path the path to be cleaned up
	 * @return the cleaned up path
	 */
	private String cleanupPath(String path) {
		path = path.trim();
		// No need for current dir path
		if (path.startsWith("./")) {
			path = path.substring(2);
		}
		String lowerCasePath = path.toLowerCase(Locale.ENGLISH);
		if (lowerCasePath.endsWith(".jar") || lowerCasePath.endsWith(".zip")) {
			return path;
		}
		if (path.endsWith("/*")) {
			path = path.substring(0, path.length() - 1);
		}
		else {
			// It's a directory
			if (!path.endsWith("/") && !path.equals(".")) {
				path = path + "/";
			}
		}
		return path;
	}

	/**
	 * Closes the PropertiesLauncher instance.
	 * @throws Exception if an error occurs while closing the classPathArchives or the
	 * parent instance
	 */
	void close() throws Exception {
		if (this.classPathArchives != null) {
			this.classPathArchives.close();
		}
		if (this.parent != null) {
			this.parent.close();
		}
	}

	/**
	 * An iterable collection of the classpath archives.
	 */
	private class ClassPathArchives implements Iterable<Archive> {

		private final List<Archive> classPathArchives;

		private final List<JarFileArchive> jarFileArchives = new ArrayList<>();

		/**
		 * Constructs a new instance of ClassPathArchives.
		 * @throws Exception if an error occurs during the construction of the
		 * ClassPathArchives instance
		 */
		ClassPathArchives() throws Exception {
			this.classPathArchives = new ArrayList<>();
			for (String path : PropertiesLauncher.this.paths) {
				for (Archive archive : getClassPathArchives(path)) {
					addClassPathArchive(archive);
				}
			}
			addNestedEntries();
		}

		/**
		 * Adds an archive to the class path.
		 * @param archive the archive to be added
		 * @throws IOException if an I/O error occurs
		 */
		private void addClassPathArchive(Archive archive) throws IOException {
			if (!(archive instanceof ExplodedArchive)) {
				this.classPathArchives.add(archive);
				return;
			}
			this.classPathArchives.add(archive);
			this.classPathArchives.addAll(asList(archive.getNestedArchives(null, new ArchiveEntryFilter())));
		}

		/**
		 * Retrieves a list of classpath archives based on the given path.
		 * @param path the path to retrieve classpath archives from
		 * @return a list of classpath archives
		 * @throws Exception if an error occurs while retrieving the classpath archives
		 */
		private List<Archive> getClassPathArchives(String path) throws Exception {
			String root = cleanupPath(handleUrl(path));
			List<Archive> lib = new ArrayList<>();
			File file = new File(root);
			if (!"/".equals(root)) {
				if (!isAbsolutePath(root)) {
					file = new File(PropertiesLauncher.this.home, root);
				}
				if (file.isDirectory()) {
					debug("Adding classpath entries from " + file);
					Archive archive = new ExplodedArchive(file, false);
					lib.add(archive);
				}
			}
			Archive archive = getArchive(file);
			if (archive != null) {
				debug("Adding classpath entries from archive " + archive.getUrl() + root);
				lib.add(archive);
			}
			List<Archive> nestedArchives = getNestedArchives(root);
			if (nestedArchives != null) {
				debug("Adding classpath entries from nested " + root);
				lib.addAll(nestedArchives);
			}
			return lib;
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
		 * Retrieves the archive from the given file.
		 * @param file the file to retrieve the archive from
		 * @return the archive retrieved from the file, or null if the file is a nested
		 * archive path or not a .jar or .zip file
		 * @throws IOException if an I/O error occurs while retrieving the archive
		 */
		private Archive getArchive(File file) throws IOException {
			if (isNestedArchivePath(file)) {
				return null;
			}
			String name = file.getName().toLowerCase(Locale.ENGLISH);
			if (name.endsWith(".jar") || name.endsWith(".zip")) {
				return getJarFileArchive(file);
			}
			return null;
		}

		/**
		 * Checks if the given file path contains a nested archive.
		 * @param file the file to check
		 * @return true if the file path contains a nested archive, false otherwise
		 */
		private boolean isNestedArchivePath(File file) {
			return file.getPath().contains(NESTED_ARCHIVE_SEPARATOR);
		}

		/**
		 * Retrieves the nested archives within the specified path.
		 * @param path The path to retrieve nested archives from.
		 * @return A list of nested archives within the specified path.
		 * @throws Exception If an error occurs while retrieving the nested archives.
		 */
		private List<Archive> getNestedArchives(String path) throws Exception {
			Archive parent = PropertiesLauncher.this.parent;
			String root = path;
			if (!root.equals("/") && root.startsWith("/")
					|| parent.getUrl().toURI().equals(PropertiesLauncher.this.home.toURI())) {
				// If home dir is same as parent archive, no need to add it twice.
				return null;
			}
			int index = root.indexOf('!');
			if (index != -1) {
				File file = new File(PropertiesLauncher.this.home, root.substring(0, index));
				if (root.startsWith("jar:file:")) {
					file = new File(root.substring("jar:file:".length(), index));
				}
				parent = getJarFileArchive(file);
				root = root.substring(index + 1);
				while (root.startsWith("/")) {
					root = root.substring(1);
				}
			}
			if (root.endsWith(".jar")) {
				File file = new File(PropertiesLauncher.this.home, root);
				if (file.exists()) {
					parent = getJarFileArchive(file);
					root = "";
				}
			}
			if (root.equals("/") || root.equals("./") || root.equals(".")) {
				// The prefix for nested jars is actually empty if it's at the root
				root = "";
			}
			EntryFilter filter = new PrefixMatchingArchiveFilter(root);
			List<Archive> archives = asList(parent.getNestedArchives(null, filter));
			if ((root == null || root.isEmpty() || ".".equals(root)) && !path.endsWith(".jar")
					&& parent != PropertiesLauncher.this.parent) {
				// You can't find the root with an entry filter so it has to be added
				// explicitly. But don't add the root of the parent archive.
				archives.add(parent);
			}
			return archives;
		}

		/**
		 * Adds nested entries from the parent archive with low priority (i.e. at the
		 * end). The parent archive might have "BOOT-INF/lib/" and "BOOT-INF/classes/"
		 * directories, indicating that the application is running from an executable JAR.
		 * @throws IOException if an I/O error occurs while accessing the nested archives
		 */
		private void addNestedEntries() {
			// The parent archive might have "BOOT-INF/lib/" and "BOOT-INF/classes/"
			// directories, meaning we are running from an executable JAR. We add nested
			// entries from there with low priority (i.e. at end).
			try {
				Iterator<Archive> archives = PropertiesLauncher.this.parent.getNestedArchives(null,
						JarLauncher.NESTED_ARCHIVE_ENTRY_FILTER);
				while (archives.hasNext()) {
					this.classPathArchives.add(archives.next());
				}
			}
			catch (IOException ex) {
				// Ignore
			}
		}

		/**
		 * Converts an iterator of Archive objects into a List of Archive objects.
		 * @param iterator the iterator of Archive objects to convert
		 * @return a List of Archive objects
		 */
		private List<Archive> asList(Iterator<Archive> iterator) {
			List<Archive> list = new ArrayList<>();
			while (iterator.hasNext()) {
				list.add(iterator.next());
			}
			return list;
		}

		/**
		 * Creates a new JarFileArchive from the given file and adds it to the list of
		 * jarFileArchives.
		 * @param file the file representing the jar archive
		 * @return the created JarFileArchive
		 * @throws IOException if an I/O error occurs while creating the JarFileArchive
		 */
		private JarFileArchive getJarFileArchive(File file) throws IOException {
			JarFileArchive archive = new JarFileArchive(file);
			this.jarFileArchives.add(archive);
			return archive;
		}

		/**
		 * Returns an iterator over the elements in this ClassPathArchives object.
		 * @return an iterator over the elements in this ClassPathArchives object
		 */
		@Override
		public Iterator<Archive> iterator() {
			return this.classPathArchives.iterator();
		}

		/**
		 * Closes all the JarFileArchives in the ClassPathArchives.
		 * @throws IOException if an I/O error occurs while closing the JarFileArchives
		 */
		void close() throws IOException {
			for (JarFileArchive archive : this.jarFileArchives) {
				archive.close();
			}
		}

	}

	/**
	 * Convenience class for finding nested archives that have a prefix in their file path
	 * (e.g. "lib/").
	 */
	private static final class PrefixMatchingArchiveFilter implements EntryFilter {

		private final String prefix;

		private final ArchiveEntryFilter filter = new ArchiveEntryFilter();

		/**
		 * Constructs a new PrefixMatchingArchiveFilter with the specified prefix.
		 * @param prefix the prefix to match against archive entries
		 */
		private PrefixMatchingArchiveFilter(String prefix) {
			this.prefix = prefix;
		}

		/**
		 * Determines if the given entry matches the specified prefix.
		 * @param entry the entry to be checked
		 * @return true if the entry matches the prefix, false otherwise
		 */
		@Override
		public boolean matches(Entry entry) {
			if (entry.isDirectory()) {
				return entry.getName().equals(this.prefix);
			}
			return entry.getName().startsWith(this.prefix) && this.filter.matches(entry);
		}

	}

	/**
	 * Convenience class for finding nested archives (archive entries that can be
	 * classpath entries).
	 */
	private static final class ArchiveEntryFilter implements EntryFilter {

		private static final String DOT_JAR = ".jar";

		private static final String DOT_ZIP = ".zip";

		/**
		 * Determines if the given entry matches the filter criteria.
		 * @param entry the entry to be checked
		 * @return true if the entry's name ends with ".jar" or ".zip", false otherwise
		 */
		@Override
		public boolean matches(Entry entry) {
			return entry.getName().endsWith(DOT_JAR) || entry.getName().endsWith(DOT_ZIP);
		}

	}

}
