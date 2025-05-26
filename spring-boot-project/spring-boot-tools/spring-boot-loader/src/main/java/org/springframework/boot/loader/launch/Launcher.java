/*
 * Copyright 2012-2024 the original author or authors.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.springframework.boot.loader.launch.Archive.Entry;
import org.springframework.boot.loader.net.protocol.Handlers;

/**
 * Base class for launchers that can start an application with a fully configured
 * classpath.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Scott Frederick
 * @since 3.2.0
 */
public abstract class Launcher {

	private static final String JAR_MODE_RUNNER_CLASS_NAME = JarModeRunner.class.getName();

	protected static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Spring-Boot-Classpath-Index";

	protected static final String DEFAULT_CLASSPATH_INDEX_FILE_NAME = "classpath.idx";

	protected ClassPathIndexFile classPathIndex;

	/**
	 * Launch the application. This method is the initial entry point that should be
	 * called by a subclass {@code public static void main(String[] args)} method.
	 * @param args the incoming arguments
	 * @throws Exception if the application fails to launch
	 */
	protected void launch(String[] args) throws Exception {
		if (!isExploded()) {
			Handlers.register();
		}
		try {
			ClassLoader classLoader = createClassLoader(getClassPathUrls());
			String jarMode = System.getProperty("jarmode");
			String mainClassName = hasLength(jarMode) ? JAR_MODE_RUNNER_CLASS_NAME : getMainClass();
			launch(classLoader, mainClassName, args);
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}

	private boolean hasLength(String jarMode) {
		return (jarMode != null) && !jarMode.isEmpty();
	}

	/**
	 * Create a classloader for the specified archives.
	 * @param urls the classpath URLs
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 */
	protected ClassLoader createClassLoader(Collection<URL> urls) throws Exception {
		return createClassLoader(urls.toArray(new URL[0]));
	}

	private ClassLoader createClassLoader(URL[] urls) {
		ClassLoader parent = getClass().getClassLoader();
		return new LaunchedClassLoader(isExploded(), getArchive(), urls, parent);
	}

	/**
	 * Launch the application given the archive file and a fully configured classloader.
	 * @param classLoader the classloader
	 * @param mainClassName the main class to run
	 * @param args the incoming arguments
	 * @throws Exception if the launch fails
	 */
	protected void launch(ClassLoader classLoader, String mainClassName, String[] args) throws Exception {
		Thread.currentThread().setContextClassLoader(classLoader);
		Class<?> mainClass = Class.forName(mainClassName, false, classLoader);
		Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
		mainMethod.setAccessible(true);
		mainMethod.invoke(null, new Object[] { args });
	}

	/**
	 * Returns if the launcher is running in an exploded mode. If this method returns
	 * {@code true} then only regular JARs are supported and the additional URL and
	 * ClassLoader support infrastructure can be optimized.
	 * @return if the jar is exploded.
	 */
	protected boolean isExploded() {
		Archive archive = getArchive();
		return (archive != null) && archive.isExploded();
	}

	ClassPathIndexFile getClassPathIndex(Archive archive) throws IOException {
		if (!archive.isExploded()) {
			return null; // Regular archives already have a defined order
		}
		String location = getClassPathIndexFileLocation(archive);
		return ClassPathIndexFile.loadIfPossible(archive.getRootDirectory(), location);
	}

	private String getClassPathIndexFileLocation(Archive archive) throws IOException {
		Manifest manifest = archive.getManifest();
		Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
		String location = (attributes != null) ? attributes.getValue(BOOT_CLASSPATH_INDEX_ATTRIBUTE) : null;
		return (location != null) ? location : getEntryPathPrefix() + DEFAULT_CLASSPATH_INDEX_FILE_NAME;
	}

	/**
	 * Return the archive being launched or {@code null} if there is no archive.
	 * @return the launched archive
	 */
	protected abstract Archive getArchive();

	/**
	 * Returns the main class that should be launched.
	 * @return the name of the main class
	 * @throws Exception if the main class cannot be obtained
	 */
	protected abstract String getMainClass() throws Exception;

	/**
	 * Returns the archives that will be used to construct the class path.
	 * @return the class path archives
	 * @throws Exception if the class path archives cannot be obtained
	 */
	protected abstract Set<URL> getClassPathUrls() throws Exception;

	/**
	 * Return the path prefix for relevant entries in the archive.
	 * @return the entry path prefix
	 */
	protected String getEntryPathPrefix() {
		return "BOOT-INF/";
	}

	/**
	 * Determine if the specified entry is a nested item that should be added to the
	 * classpath.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a nested item (jar or directory)
	 */
	protected boolean isIncludedOnClassPath(Archive.Entry entry) {
		return isLibraryFileOrClassesDirectory(entry);
	}

	protected boolean isLibraryFileOrClassesDirectory(Archive.Entry entry) {
		String name = entry.name();
		if (entry.isDirectory()) {
			return name.equals("BOOT-INF/classes/");
		}
		return name.startsWith("BOOT-INF/lib/");
	}

	protected boolean isIncludedOnClassPathAndNotIndexed(Entry entry) {
		if (!isIncludedOnClassPath(entry)) {
			return false;
		}
		return (this.classPathIndex == null) || !this.classPathIndex.containsEntry(entry.name());
	}

}
