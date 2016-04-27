/*
 * Copyright 2012-2016 the original author or authors.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
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
 * {@link Launcher} for archives with user-configured classpath and main class via a
 * properties file. This model is often more flexible and more amenable to creating
 * well-behaved OS-level services than a model based on executable jars.
 * <p>
 * Looks in various places for a properties file to extract loader settings, defaulting to
 * {@code application.properties} either on the current classpath or in the current
 * working directory. The name of the properties file can be changed by setting a System
 * property {@code loader.config.name} (e.g. {@code -Dloader.config.name=foo} will look
 * for {@code foo.properties}. If that file doesn't exist then tries
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
 */
public class PropertiesLauncher extends Launcher {

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
	 * Properties key for boolean flag (default false) which if set will cause the
	 * external configuration properties to be copied to System properties (assuming that
	 * is allowed by Java security).
	 */
	public static final String SET_SYSTEM_PROPERTIES = "loader.system";

	private static final Pattern WORD_SEPARATOR = Pattern.compile("\\W+");

	private final File home;

	private List<String> paths = new ArrayList<String>();

	private final Properties properties = new Properties();

	private Archive parent;

	public PropertiesLauncher() {
		try {
			this.home = getHomeDirectory();
			initializeProperties(this.home);
			initializePaths();
			this.parent = createArchive();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected File getHomeDirectory() {
		return new File(SystemPropertyUtils
				.resolvePlaceholders(System.getProperty(HOME, "${user.dir}")));
	}

	private void initializeProperties(File home) throws Exception, IOException {
		String config = "classpath:BOOT-INF/classes/"
				+ SystemPropertyUtils.resolvePlaceholders(
						SystemPropertyUtils.getProperty(CONFIG_NAME, "application"))
				+ ".properties";
		config = SystemPropertyUtils.resolvePlaceholders(
				SystemPropertyUtils.getProperty(CONFIG_LOCATION, config));
		InputStream resource = getResource(config);
		if (resource != null) {
			log("Found: " + config);
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
			if (SystemPropertyUtils
					.resolvePlaceholders("${" + SET_SYSTEM_PROPERTIES + ":false}")
					.equals("true")) {
				log("Adding resolved properties to System properties");
				for (Object key : Collections.list(this.properties.propertyNames())) {
					String value = this.properties.getProperty((String) key);
					System.setProperty((String) key, value);
				}
			}
		}
		else {
			log("Not found: " + config);
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
		log("Trying classpath: " + config);
		return getClass().getResourceAsStream(config);
	}

	private InputStream getFileResource(String config) throws Exception {
		File file = new File(config);
		log("Trying file: " + config);
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
			connection.setUseCaches(
					connection.getClass().getSimpleName().startsWith("JNLP"));
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
			this.paths = parsePathsProperty(
					SystemPropertyUtils.resolvePlaceholders(path));
		}
		log("Nested archive paths: " + this.paths);
	}

	private List<String> parsePathsProperty(String commaSeparatedPaths) {
		List<String> paths = new ArrayList<String>();
		for (String path : commaSeparatedPaths.split(",")) {
			path = cleanupPath(path);
			// Empty path (i.e. the archive itself if running from a JAR) is always added
			// to the classpath so no need for it to be explicitly listed
			if (!path.equals("")) {
				paths.add(path);
			}
		}
		if (paths.isEmpty()) {
			paths.add("lib");
		}
		return paths;
	}

	protected String[] getArgs(String... args) throws Exception {
		String loaderArgs = getProperty(ARGS);
		if (loaderArgs != null) {
			String[] defaultArgs = loaderArgs.split("\\s+");
			String[] additionalArgs = args;
			args = new String[defaultArgs.length + additionalArgs.length];
			System.arraycopy(defaultArgs, 0, args, 0, defaultArgs.length);
			System.arraycopy(additionalArgs, 0, args, defaultArgs.length,
					additionalArgs.length);
		}
		return args;
	}

	@Override
	protected String getMainClass() throws Exception {
		String mainClass = getProperty(MAIN, "Start-Class");
		if (mainClass == null) {
			throw new IllegalStateException(
					"No '" + MAIN + "' or 'Start-Class' specified");
		}
		return mainClass;
	}

	@Override
	protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
		ClassLoader loader = super.createClassLoader(archives);
		String customLoaderClassName = getProperty("loader.classLoader");
		if (customLoaderClassName != null) {
			loader = wrapWithCustomClassLoader(loader, customLoaderClassName);
			log("Using custom class loader: " + customLoaderClassName);
		}
		return loader;
	}

	@SuppressWarnings("unchecked")
	private ClassLoader wrapWithCustomClassLoader(ClassLoader parent,
			String loaderClassName) throws Exception {
		Class<ClassLoader> loaderClass = (Class<ClassLoader>) Class
				.forName(loaderClassName, true, parent);

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
			log("Property '" + propertyKey + "' from environment: " + value);
			return value;
		}
		if (this.properties.containsKey(propertyKey)) {
			String value = SystemPropertyUtils
					.resolvePlaceholders(this.properties.getProperty(propertyKey));
			log("Property '" + propertyKey + "' from properties: " + value);
			return value;
		}
		try {
			// Prefer home dir for MANIFEST if there is one
			Manifest manifest = new ExplodedArchive(this.home, false).getManifest();
			if (manifest != null) {
				String value = manifest.getMainAttributes().getValue(manifestKey);
				log("Property '" + manifestKey + "' from home directory manifest: "
						+ value);
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
				log("Property '" + manifestKey + "' from archive manifest: " + value);
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
				if (archive instanceof ExplodedArchive) {
					List<Archive> nested = new ArrayList<Archive>(
							archive.getNestedArchives(new ArchiveEntryFilter()));
					nested.add(0, archive);
					lib.addAll(nested);
				}
				else {
					lib.add(archive);
				}
			}
		}
		addNestedEntries(lib);
		return lib;
	}

	private List<Archive> getClassPathArchives(String path) throws Exception {
		String root = cleanupPath(stripFileUrlPrefix(path));
		List<Archive> lib = new ArrayList<Archive>();
		File file = new File(root);
		if (!isAbsolutePath(root)) {
			file = new File(this.home, root);
		}
		if (file.isDirectory()) {
			log("Adding classpath entries from " + file);
			Archive archive = new ExplodedArchive(file, false);
			lib.add(archive);
		}
		Archive archive = getArchive(file);
		if (archive != null) {
			log("Adding classpath entries from archive " + archive.getUrl() + root);
			lib.add(archive);
		}
		Archive nested = getNestedArchive(root);
		if (nested != null) {
			log("Adding classpath entries from nested " + nested.getUrl() + root);
			lib.add(nested);
		}
		return lib;
	}

	private boolean isAbsolutePath(String root) {
		// Windows contains ":" others start with "/"
		return root.contains(":") || root.startsWith("/");
	}

	private Archive getArchive(File file) throws IOException {
		String name = file.getName().toLowerCase();
		if (name.endsWith(".jar") || name.endsWith(".zip")) {
			return new JarFileArchive(file);
		}
		return null;
	}

	private Archive getNestedArchive(String root) throws Exception {
		if (root.startsWith("/")
				|| this.parent.getUrl().equals(this.home.toURI().toURL())) {
			// If home dir is same as parent archive, no need to add it twice.
			return null;
		}
		EntryFilter filter = new PrefixMatchingArchiveFilter(root);
		if (this.parent.getNestedArchives(filter).isEmpty()) {
			return null;
		}
		// If there are more archives nested in this subdirectory (root) then create a new
		// virtual archive for them, and have it added to the classpath
		return new FilteredArchive(this.parent, filter);
	}

	private void addNestedEntries(List<Archive> lib) {
		// The parent archive might have "BOOT-INF/lib/" and "BOOT-INF/classes/"
		// directories, meaning we are running from an executable JAR. We add nested
		// entries from there with low priority (i.e. at end).
		try {
			lib.addAll(this.parent.getNestedArchives(new EntryFilter() {

				@Override
				public boolean matches(Entry entry) {
					if (entry.isDirectory()) {
						return entry.getName().startsWith(JarLauncher.BOOT_INF_CLASSES);
					}
					return entry.getName().startsWith(JarLauncher.BOOT_INF_LIB);
				}

			}));
		}
		catch (IOException ex) {
			// Ignore
		}
	}

	private String cleanupPath(String path) {
		path = path.trim();
		// No need for current dir path
		if (path.startsWith("./")) {
			path = path.substring(2);
		}
		if (path.toLowerCase().endsWith(".jar") || path.toLowerCase().endsWith(".zip")) {
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

	public static void main(String[] args) throws Exception {
		PropertiesLauncher launcher = new PropertiesLauncher();
		args = launcher.getArgs(args);
		launcher.launch(args);
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

	private void log(String message) {
		if (Boolean.getBoolean(DEBUG)) {
			// We shouldn't use java.util.logging because of classpath issues so we
			// just sysout log messages when "loader.debug" is true
			System.out.println(message);
		}
	}

	/**
	 * Convenience class for finding nested archives that have a prefix in their file path
	 * (e.g. "lib/").
	 */
	private static final class PrefixMatchingArchiveFilter implements EntryFilter {

		private final String prefix;

		private final ArchiveEntryFilter filter = new ArchiveEntryFilter();

		private PrefixMatchingArchiveFilter(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public boolean matches(Entry entry) {
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

		@Override
		public boolean matches(Entry entry) {
			return entry.getName().endsWith(DOT_JAR) || entry.getName().endsWith(DOT_ZIP);
		}

	}

	/**
	 * Decorator to apply an {@link Archive.EntryFilter} to an existing {@link Archive}.
	 */
	private static class FilteredArchive implements Archive {

		private final Archive parent;

		private final EntryFilter filter;

		FilteredArchive(Archive parent, EntryFilter filter) {
			this.parent = parent;
			this.filter = filter;
		}

		@Override
		public URL getUrl() throws MalformedURLException {
			return this.parent.getUrl();
		}

		@Override
		public Manifest getManifest() throws IOException {
			return this.parent.getManifest();
		}

		@Override
		public Iterator<Entry> iterator() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Archive> getNestedArchives(final EntryFilter filter)
				throws IOException {
			return this.parent.getNestedArchives(new EntryFilter() {
				@Override
				public boolean matches(Entry entry) {
					return FilteredArchive.this.filter.matches(entry)
							&& filter.matches(entry);
				}
			});
		}

	}
}
