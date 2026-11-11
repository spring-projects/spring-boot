/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.File;
import java.util.Locale;

/**
 * The type of archive that is being processed.
 *
 * @author Moritz Halbritter
 */
enum ArchiveType {

	/**
	 * A jar archive.
	 */
	JAR,

	/**
	 * A war archive.
	 */
	WAR;

	private static final String WAR_EXTENSION = ".war";

	static ArchiveType of(File file) {
		return (file.getName().toLowerCase(Locale.ROOT).endsWith(WAR_EXTENSION)) ? WAR : JAR;
	}

}
