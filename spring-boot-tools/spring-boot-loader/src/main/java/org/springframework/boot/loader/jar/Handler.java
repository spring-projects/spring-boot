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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link URLStreamHandler} for Spring Boot loader {@link JarFile}s.
 * 
 * @author Phillip Webb
 * @see JarFile#registerUrlProtocolHandler()
 */
public class Handler extends URLStreamHandler {

	// NOTE: in order to be found as a URL protocol hander, this class must be public,
	// must be named Handler and must be in a package ending '.jar'

	private static final String FILE_PROTOCOL = "file:";

	private static final String SEPARATOR = "!/";

	private static final String[] FALLBACK_HANDLERS = { "sun.net.www.protocol.jar.Handler" };

	private static final Method OPEN_CONNECTION_METHOD;
	static {
		Method method = null;
		try {
			method = URLStreamHandler.class
					.getDeclaredMethod("openConnection", URL.class);
		}
		catch (Exception ex) {
		}
		OPEN_CONNECTION_METHOD = method;
	}

	private static SoftReference<Map<File, JarFile>> rootFileCache;
	static {
		rootFileCache = new SoftReference<Map<File, JarFile>>(null);
	}

	private final Logger logger = Logger.getLogger(getClass().getName());

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
		if (this.jarFile != null) {
			return new JarURLConnection(url, this.jarFile);
		}
		try {
			return new JarURLConnection(url, getRootJarFileFromUrl(url));
		}
		catch (Exception ex) {
			return openFallbackConnection(url, ex);
		}
	}

	private URLConnection openFallbackConnection(URL url, Exception reason)
			throws IOException {
		try {
			return openConnection(getFallbackHandler(), url);
		}
		catch (Exception ex) {
			if (reason instanceof IOException) {
				this.logger.log(Level.FINEST, "Unable to open fallback handler", ex);
				throw (IOException) reason;
			}
			this.logger.log(Level.WARNING, "Unable to open fallback handler", ex);
			if (reason instanceof RuntimeException) {
				throw (RuntimeException) reason;
			}
			throw new IllegalStateException(reason);
		}
	}

	private URLStreamHandler getFallbackHandler() {
		if (this.fallbackHandler != null) {
			return this.fallbackHandler;
		}
		for (String handlerClassName : FALLBACK_HANDLERS) {
			try {
				Class<?> handlerClass = Class.forName(handlerClassName);
				this.fallbackHandler = (URLStreamHandler) handlerClass.newInstance();
				return this.fallbackHandler;
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		throw new IllegalStateException("Unable to find fallback handler");
	}

	private URLConnection openConnection(URLStreamHandler handler, URL url)
			throws Exception {
		if (OPEN_CONNECTION_METHOD == null) {
			throw new IllegalStateException(
					"Unable to invoke fallback open connection method");
		}
		OPEN_CONNECTION_METHOD.setAccessible(true);
		return (URLConnection) OPEN_CONNECTION_METHOD.invoke(handler, url);
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
			String path = name.substring(FILE_PROTOCOL.length());
			File file = new File(URLDecoder.decode(path, "UTF-8"));
			Map<File, JarFile> cache = rootFileCache.get();
			JarFile jarFile = (cache == null ? null : cache.get(file));
			if (jarFile == null) {
				jarFile = new JarFile(file);
				addToRootFileCache(file, jarFile);
			}
			return jarFile;
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
			cache = new ConcurrentHashMap<File, JarFile>();
			rootFileCache = new SoftReference<Map<File, JarFile>>(cache);
		}
		cache.put(sourceFile, jarFile);
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
