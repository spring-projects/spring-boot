/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.system;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Provides access to the application home directory. Attempts to pick a sensible home for
 * both Jar Files, Exploded Archives and directly running applications.
 *
 * @author Phillip Webb
 * @author Raja Kolli
 * @since 2.0.0
 */
public class ApplicationHome {

	private final File source;

	private final File dir;

	/**
	 * Create a new {@link ApplicationHome} instance.
	 */
	public ApplicationHome() {
		this(null);
	}

	/**
	 * Create a new {@link ApplicationHome} instance for the specified source class.
	 * @param sourceClass the source class or {@code null}
	 */
	public ApplicationHome(Class<?> sourceClass) {
		this.source = findSource(sourceClass == null ? getStartClass() : sourceClass);
		this.dir = findHomeDir(this.source);
	}

	private Class<?> getStartClass() {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			return getStartClass(classLoader.getResources("META-INF/MANIFEST.MF"));
		}
		catch (Exception ex) {
			return null;
		}
	}

	private Class<?> getStartClass(Enumeration<URL> manifestResources) {
		while (manifestResources.hasMoreElements()) {
			try (InputStream inputStream = manifestResources.nextElement().openStream()) {
				Manifest manifest = new Manifest(inputStream);
				String startClass = manifest.getMainAttributes().getValue("Start-Class");
				if (startClass != null) {
					return ClassUtils.forName(startClass, getClass().getClassLoader());
				}
			}
			catch (Exception ex) {
			}
		}
		return null;
	}

	private File findSource(Class<?> sourceClass) {
		try {
			ProtectionDomain domain = (sourceClass == null ? null
					: sourceClass.getProtectionDomain());
			CodeSource codeSource = (domain == null ? null : domain.getCodeSource());
			URL location = (codeSource == null ? null : codeSource.getLocation());
			File source = (location == null ? null : findSource(location));
			if (source != null && source.exists() && !isUnitTest()) {
				return source.getAbsoluteFile();
			}
			return null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	private boolean isUnitTest() {
		try {
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			for (int i = stackTrace.length - 1; i >= 0; i--) {
				if (stackTrace[i].getClassName().startsWith("org.junit.")) {
					return true;
				}
			}
		}
		catch (Exception ex) {
		}
		return false;
	}

	private File findSource(URL location) throws IOException {
		URLConnection connection = location.openConnection();
		if (connection instanceof JarURLConnection) {
			return getRootJarFile(((JarURLConnection) connection).getJarFile());
		}
		return new File(location.getPath());
	}

	private File getRootJarFile(JarFile jarFile) {
		String name = jarFile.getName();
		int separator = name.indexOf("!/");
		if (separator > 0) {
			name = name.substring(0, separator);
		}
		return new File(name);
	}

	private File findHomeDir(File source) {
		File homeDir = source;
		homeDir = (homeDir == null ? findDefaultHomeDir() : homeDir);
		if (homeDir.isFile()) {
			homeDir = homeDir.getParentFile();
		}
		homeDir = (homeDir.exists() ? homeDir : new File("."));
		return homeDir.getAbsoluteFile();
	}

	private File findDefaultHomeDir() {
		String userDir = System.getProperty("user.dir");
		return new File(StringUtils.hasLength(userDir) ? userDir : ".");
	}

	/**
	 * Returns the underlying source used to find the home directory. This is usually the
	 * jar file or a directory. Can return {@code null} if the source cannot be
	 * determined.
	 * @return the underlying source or {@code null}
	 */
	public File getSource() {
		return this.source;
	}

	/**
	 * Returns the application home directory.
	 * @return the home directory (never {@code null})
	 */
	public File getDir() {
		return this.dir;
	}

	@Override
	public String toString() {
		return getDir().toString();
	}

}
