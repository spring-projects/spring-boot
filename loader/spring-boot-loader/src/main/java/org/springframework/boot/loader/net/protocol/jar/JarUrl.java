/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;

/**
 * Utility class with factory methods that can be used to create JAR URLs.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public final class JarUrl {

	private JarUrl() {
	}

	/**
	 * Create a new jar URL.
	 * @param file the jar file
	 * @return a jar file URL
	 */
	public static URL create(File file) {
		return create(file, (String) null);
	}

	/**
	 * Create a new jar URL.
	 * @param file the jar file
	 * @param nestedEntry the nested entry or {@code null}
	 * @return a jar file URL
	 */
	public static URL create(File file, JarEntry nestedEntry) {
		return create(file, (nestedEntry != null) ? nestedEntry.getName() : null);
	}

	/**
	 * Create a new jar URL.
	 * @param file the jar file
	 * @param nestedEntryName the nested entry name or {@code null}
	 * @return a jar file URL
	 */
	public static URL create(File file, String nestedEntryName) {
		return create(file, nestedEntryName, null);
	}

	/**
	 * Create a new jar URL.
	 * @param file the jar file
	 * @param nestedEntryName the nested entry name or {@code null}
	 * @param path the path within the jar or nested jar
	 * @return a jar file URL
	 */
	public static URL create(File file, String nestedEntryName, String path) {
		try {
			path = (path != null) ? path : "";
			return new URL(null, "jar:" + getJarReference(file, nestedEntryName) + "!/" + path, Handler.INSTANCE);
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("Unable to create JarFileArchive URL", ex);
		}
	}

	private static String getJarReference(File file, String nestedEntryName) {
		String jarFilePath = file.toURI().getRawPath().replace("!", "%21");
		return (nestedEntryName != null) ? "nested:" + jarFilePath + "/!" + nestedEntryName : "file:" + jarFilePath;
	}

}
