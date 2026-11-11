/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart.server;

import java.net.URL;

/**
 * Filter URLs based on a source directory name. Used to match URLs from the running
 * classpath against source directory on a remote system.
 *
 * @author Phillip Webb
 * @since 2.3.0
 * @see DefaultSourceDirectoryUrlFilter
 */
@FunctionalInterface
public interface SourceDirectoryUrlFilter {

	/**
	 * Determine if the specified URL matches a source directory.
	 * @param sourceDirectory the source directory
	 * @param url the URL to check
	 * @return {@code true} if the URL matches
	 */
	boolean isMatch(String sourceDirectory, URL url);

}
