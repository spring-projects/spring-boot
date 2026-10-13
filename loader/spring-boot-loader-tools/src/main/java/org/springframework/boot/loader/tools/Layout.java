/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface used to determine the layout for a particular type of archive.
 * Layouts may additionally implement {@link CustomLoaderLayout} if they wish to write
 * custom loader classes.
 *
 * @author Phillip Webb
 * @since 1.0.0
 * @see Layouts
 * @see RepackagingLayout
 * @see CustomLoaderLayout
 */
public interface Layout {

	/**
	 * Returns the launcher class name for this layout.
	 * @return the launcher class name
	 */
	@Nullable String getLauncherClassName();

	/**
	 * Returns the destination path for a given library.
	 * @param libraryName the name of the library (excluding any path)
	 * @param scope the scope of the library
	 * @return the location of the library relative to the root of the archive (should end
	 * with '/') or {@code null} if the library should not be included.
	 */
	@Nullable String getLibraryLocation(String libraryName, @Nullable LibraryScope scope);

	/**
	 * Returns the location of classes within the archive.
	 * @return the classes location
	 */
	String getClassesLocation();

	/**
	 * Returns the location of the classpath index file that should be written or
	 * {@code null} if not index is required. The result should include the filename and
	 * is relative to the root of the jar.
	 * @return the classpath index file location
	 * @since 2.5.0
	 */
	default @Nullable String getClasspathIndexFileLocation() {
		return null;
	}

	/**
	 * Returns the location of the layer index file that should be written or {@code null}
	 * if not index is required. The result should include the filename and is relative to
	 * the root of the jar.
	 * @return the layer index file location
	 * @since 2.5.0
	 */
	default @Nullable String getLayersIndexFileLocation() {
		return null;
	}

	/**
	 * Returns if loader classes should be included to make the archive executable.
	 * @return if the layout is executable
	 */
	boolean isExecutable();

}
