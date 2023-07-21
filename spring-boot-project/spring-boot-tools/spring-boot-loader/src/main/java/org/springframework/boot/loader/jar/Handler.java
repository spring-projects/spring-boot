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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * {@link URLStreamHandler} for Spring Boot loader {@link JarFile}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 * @see JarFile#registerUrlProtocolHandler()
 */
public class Handler extends URLStreamHandler {

	// NOTE: in order to be found as a URL protocol handler, this class must be public,
	// must be named Handler and must be in a package ending '.jar'

	private static final String JAR_PROTOCOL = "jar:";

	private static final String FILE_PROTOCOL = "file:";

	private static final String TOMCAT_WARFILE_PROTOCOL = "war:file:";

	private static final String SEPARATOR = "!/";

	private static final Pattern SEPARATOR_PATTERN = Pattern.compile(SEPARATOR, Pattern.LITERAL);

	private static final String CURRENT_DIR = "/./";

	private static final Pattern CURRENT_DIR_PATTERN = Pattern.compile(CURRENT_DIR, Pattern.LITERAL);

	private static final String PARENT_DIR = "/../";

	private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

	private static final String[] FALLBACK_HANDLERS = { "sun.net.www.protocol.jar.Handler" };

	private static URL jarContextUrl;

	private static SoftReference<Map<File, JarFile>> rootFileCache;

	static {
		rootFileCache = new SoftReference<>(null);
	}

	private final JarFile jarFile;

	private URLStreamHandler fallbackHandler;

	public Handler() {
		this(null);
	}

	public Handler(JarFile jarFile) {
		this.jarFile = jarFile;
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		if (this.jarFile != null && isUrlInJarFile(url, this.jarFile)) {
			return JarURLConnection.get(url, this.jarFile);
		}
		try {
			return JarURLConnection.get(url, getRootJarFileFromUrl(url));
		}
		catch (Exception ex) {
			return openFallbackConnection(url, ex);
		}
	}

	private boolean isUrlInJarFile(URL url, JarFile jarFile) throws MalformedURLException {
		// Try the path first to save building a new url string each time
		return url.getPath().startsWith(jarFile.getUrl().getPath())
				&& url.toString().startsWith(jarFile.getUrlString());
	}

	private URLConnection openFallbackConnection(URL url, Exception reason) throws IOException {
		try {
			URLConnection connection = openFallbackTomcatConnection(url);
			connection = (connection != null) ? connection : openFallbackContextConnection(url);
			return (connection != null) ? connection : openFallbackHandlerConnection(url);
		}
		catch (Exception ex) {
			if (reason instanceof IOException ioException) {
				log(false, "Unable to open fallback handler", ex);
				throw ioException;
			}
			log(true, "Unable to open fallback handler", ex);
			if (reason instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException(reason);
		}
	}

	/**
	 * Attempt to open a Tomcat formatted 'jar:war:file:...' URL. This method allows us to
	 * use our own nested JAR support to open the content rather than the logic in
	 * {@code sun.net.www.protocol.jar.URLJarFile} which will extract the nested jar to
	 * the temp folder to that its content can be accessed.
	 * @param url the URL to open
	 * @return a {@link URLConnection} or {@code null}
	 */
	private URLConnection openFallbackTomcatConnection(URL url) {
		String file = url.getFile();
		if (isTomcatWarUrl(file)) {
			file = file.substring(TOMCAT_WARFILE_PROTOCOL.length());
			file = file.replaceFirst("\\*/", "!/");
			try {
				URLConnection connection = openConnection(new URL("jar:file:" + file));
				connection.getInputStream().close();
				return connection;
			}
			catch (IOException ex) {
			}
		}
		return null;
	}

	private boolean isTomcatWarUrl(String file) {
		if (file.startsWith(TOMCAT_WARFILE_PROTOCOL) || !file.contains("*/")) {
			try {
				URLConnection connection = new URL(file).openConnection();
				if (connection.getClass().getName().startsWith("org.apache.catalina")) {
					return true;
				}
			}
			catch (Exception ex) {
			}
		}
		return false;
	}

	/**
	 * Attempt to open a fallback connection by using a context URL captured before the
	 * jar handler was replaced with our own version. Since this method doesn't use
	 * reflection it won't trigger "illegal reflective access operation has occurred"
	 * warnings on Java 13+.
	 * @param url the URL to open
	 * @return a {@link URLConnection} or {@code null}
	 */
	private URLConnection openFallbackContextConnection(URL url) {
		try {
			if (jarContextUrl != null) {
				return new URL(jarContextUrl, url.toExternalForm()).openConnection();
			}
		}
		catch (Exception ex) {
		}
		return null;
	}

	/**
	 * Attempt to open a fallback connection by using reflection to access Java's default
	 * jar {@link URLStreamHandler}.
	 * @param url the URL to open
	 * @return the {@link URLConnection}
	 * @throws Exception if not connection could be opened
	 */
	private URLConnection openFallbackHandlerConnection(URL url) throws Exception {
		URLStreamHandler fallbackHandler = getFallbackHandler();
		return new URL(null, url.toExternalForm(), fallbackHandler).openConnection();
	}

	private URLStreamHandler getFallbackHandler() {
		if (this.fallbackHandler != null) {
			return this.fallbackHandler;
		}
		for (String handlerClassName : FALLBACK_HANDLERS) {
			try {
				Class<?> handlerClass = Class.forName(handlerClassName);
				this.fallbackHandler = (URLStreamHandler) handlerClass.getDeclaredConstructor().newInstance();
				return this.fallbackHandler;
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		throw new IllegalStateException("Unable to find fallback handler");
	}

	private void log(boolean warning, String message, Exception cause) {
		try {
			Level level = warning ? Level.WARNING : Level.FINEST;
			Logger.getLogger(getClass().getName()).log(level, message, cause);
		}
		catch (Exception ex) {
			if (warning) {
				System.err.println("WARNING: " + message);
			}
		}
	}

	@Override
	protected void parseURL(URL context, String spec, int start, int limit) {
		if (spec.regionMatches(true, 0, JAR_PROTOCOL, 0, JAR_PROTOCOL.length())) {
			setFile(context, getFileFromSpec(spec.substring(start, limit)));
		}
		else {
			setFile(context, getFileFromContext(context, spec.substring(start, limit)));
		}
	}

	private String getFileFromSpec(String spec) {
		int separatorIndex = spec.lastIndexOf("!/");
		if (separatorIndex == -1) {
			throw new IllegalArgumentException("No !/ in spec '" + spec + "'");
		}
		try {
			new URL(spec.substring(0, separatorIndex));
			return spec;
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("Invalid spec URL '" + spec + "'", ex);
		}
	}

	private String getFileFromContext(URL context, String spec) {
		String file = context.getFile();
		if (spec.startsWith("/")) {
			return trimToJarRoot(file) + SEPARATOR + spec.substring(1);
		}
		if (file.endsWith("/")) {
			return file + spec;
		}
		int lastSlashIndex = file.lastIndexOf('/');
		if (lastSlashIndex == -1) {
			throw new IllegalArgumentException("No / found in context URL's file '" + file + "'");
		}
		return file.substring(0, lastSlashIndex + 1) + spec;
	}

	private String trimToJarRoot(String file) {
		int lastSeparatorIndex = file.lastIndexOf(SEPARATOR);
		if (lastSeparatorIndex == -1) {
			throw new IllegalArgumentException("No !/ found in context URL's file '" + file + "'");
		}
		return file.substring(0, lastSeparatorIndex);
	}

	private void setFile(URL context, String file) {
		String path = normalize(file);
		String query = null;
		int queryIndex = path.lastIndexOf('?');
		if (queryIndex != -1) {
			query = path.substring(queryIndex + 1);
			path = path.substring(0, queryIndex);
		}
		setURL(context, JAR_PROTOCOL, null, -1, null, null, path, query, context.getRef());
	}

	private String normalize(String file) {
		if (!file.contains(CURRENT_DIR) && !file.contains(PARENT_DIR)) {
			return file;
		}
		int afterLastSeparatorIndex = file.lastIndexOf(SEPARATOR) + SEPARATOR.length();
		String afterSeparator = file.substring(afterLastSeparatorIndex);
		afterSeparator = replaceParentDir(afterSeparator);
		afterSeparator = replaceCurrentDir(afterSeparator);
		return file.substring(0, afterLastSeparatorIndex) + afterSeparator;
	}

	private String replaceParentDir(String file) {
		int parentDirIndex;
		while ((parentDirIndex = file.indexOf(PARENT_DIR)) >= 0) {
			int precedingSlashIndex = file.lastIndexOf('/', parentDirIndex - 1);
			if (precedingSlashIndex >= 0) {
				file = file.substring(0, precedingSlashIndex) + file.substring(parentDirIndex + 3);
			}
			else {
				file = file.substring(parentDirIndex + 4);
			}
		}
		return file;
	}

	private String replaceCurrentDir(String file) {
		return CURRENT_DIR_PATTERN.matcher(file).replaceAll("/");
	}

	@Override
	protected int hashCode(URL u) {
		return hashCode(u.getProtocol(), u.getFile());
	}

	private int hashCode(String protocol, String file) {
		int result = (protocol != null) ? protocol.hashCode() : 0;
		int separatorIndex = file.indexOf(SEPARATOR);
		if (separatorIndex == -1) {
			return result + file.hashCode();
		}
		String source = file.substring(0, separatorIndex);
		String entry = canonicalize(file.substring(separatorIndex + 2));
		try {
			result += new URL(source).hashCode();
		}
		catch (MalformedURLException ex) {
			result += source.hashCode();
		}
		result += entry.hashCode();
		return result;
	}

	@Override
	protected boolean sameFile(URL u1, URL u2) {
		if (!u1.getProtocol().equals("jar") || !u2.getProtocol().equals("jar")) {
			return false;
		}
		int separator1 = u1.getFile().indexOf(SEPARATOR);
		int separator2 = u2.getFile().indexOf(SEPARATOR);
		if (separator1 == -1 || separator2 == -1) {
			return super.sameFile(u1, u2);
		}
		String nested1 = u1.getFile().substring(separator1 + SEPARATOR.length());
		String nested2 = u2.getFile().substring(separator2 + SEPARATOR.length());
		if (!nested1.equals(nested2)) {
			String canonical1 = canonicalize(nested1);
			String canonical2 = canonicalize(nested2);
			if (!canonical1.equals(canonical2)) {
				return false;
			}
		}
		String root1 = u1.getFile().substring(0, separator1);
		String root2 = u2.getFile().substring(0, separator2);
		try {
			return super.sameFile(new URL(root1), new URL(root2));
		}
		catch (MalformedURLException ex) {
			// Continue
		}
		return super.sameFile(u1, u2);
	}

	private String canonicalize(String path) {
		return SEPARATOR_PATTERN.matcher(path).replaceAll("/");
	}

	public JarFile getRootJarFileFromUrl(URL url) throws IOException {
		String spec = url.getFile();
		int separatorIndex = spec.indexOf(SEPARATOR);
		if (separatorIndex == -1) {
			throw new MalformedURLException("Jar URL does not contain !/ separator");
		}
		String name = spec.substring(0, separatorIndex);
		return getRootJarFile(name);
	}

	private JarFile getRootJarFile(String name) throws IOException {
		try {
			if (!name.startsWith(FILE_PROTOCOL)) {
				throw new IllegalStateException("Not a file URL");
			}
			File file = new File(URI.create(name));
			Map<File, JarFile> cache = rootFileCache.get();
			JarFile result = (cache != null) ? cache.get(file) : null;
			if (result == null) {
				result = new JarFile(file);
				addToRootFileCache(file, result);
			}
			return result;
		}
		catch (Exception ex) {
			throw new IOException("Unable to open root Jar file '" + name + "'", ex);
		}
	}

	/**
	 * Add the given {@link JarFile} to the root file cache.
	 * @param sourceFile the source file to add
	 * @param jarFile the jar file.
	 */
	static void addToRootFileCache(File sourceFile, JarFile jarFile) {
		Map<File, JarFile> cache = rootFileCache.get();
		if (cache == null) {
			cache = new ConcurrentHashMap<>();
			rootFileCache = new SoftReference<>(cache);
		}
		cache.put(sourceFile, jarFile);
	}

	/**
	 * If possible, capture a URL that is configured with the original jar handler so that
	 * we can use it as a fallback context later. We can only do this if we know that we
	 * can reset the handlers after.
	 */
	static void captureJarContextUrl() {
		if (canResetCachedUrlHandlers()) {
			String handlers = System.getProperty(PROTOCOL_HANDLER);
			try {
				System.clearProperty(PROTOCOL_HANDLER);
				try {
					resetCachedUrlHandlers();
					jarContextUrl = new URL("jar:file:context.jar!/");
					URLConnection connection = jarContextUrl.openConnection();
					if (connection instanceof JarURLConnection) {
						jarContextUrl = null;
					}
				}
				catch (Exception ex) {
				}
			}
			finally {
				if (handlers == null) {
					System.clearProperty(PROTOCOL_HANDLER);
				}
				else {
					System.setProperty(PROTOCOL_HANDLER, handlers);
				}
			}
			resetCachedUrlHandlers();
		}
	}

	private static boolean canResetCachedUrlHandlers() {
		try {
			resetCachedUrlHandlers();
			return true;
		}
		catch (Error ex) {
			return false;
		}
	}

	private static void resetCachedUrlHandlers() {
		URL.setURLStreamHandlerFactory(null);
	}

	/**
	 * Set if a generic static exception can be thrown when a URL cannot be connected.
	 * This optimization is used during class loading to save creating lots of exceptions
	 * which are then swallowed.
	 * @param useFastConnectionExceptions if fast connection exceptions can be used.
	 */
	public static void setUseFastConnectionExceptions(boolean useFastConnectionExceptions) {
		JarURLConnection.setUseFastExceptions(useFastConnectionExceptions);
	}

}
