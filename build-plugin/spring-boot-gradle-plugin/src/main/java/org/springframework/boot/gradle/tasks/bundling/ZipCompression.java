/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.tasks.bundling;

import java.util.zip.ZipEntry;

/**
 * An enumeration of supported compression options for an entry in a ZIP archive.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public enum ZipCompression {

	/**
	 * The entry should be {@link ZipEntry#STORED} in the archive.
	 */
	STORED,

	/**
	 * The entry should be {@link ZipEntry#DEFLATED} in the archive.
	 */
	DEFLATED

}
