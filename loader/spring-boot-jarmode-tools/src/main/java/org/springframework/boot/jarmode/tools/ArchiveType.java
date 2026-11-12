/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.File;
import java.util.Locale;
import java.util.jar.Manifest;

import org.springframework.util.StringUtils;

/**
 * The type of archive that is being processed.
 *
 * @author Moritz Halbritter
 * @author Xinwei Zhang
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

	static ArchiveType of(File file, Manifest manifest) {
		if (file.isDirectory()) {
			return StringUtils.hasLength(manifest.getMainAttributes().getValue("Spring-Boot-Lib-Provided")) ? WAR : JAR;
		}
		return of(file);
	}

}
