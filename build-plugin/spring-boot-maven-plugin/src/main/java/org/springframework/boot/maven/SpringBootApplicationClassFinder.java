/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * Find a single Spring Boot Application class match based on directory.
 *
 * @author Stephane Nicoll
 * @see MainClassFinder
 */
abstract class SpringBootApplicationClassFinder {

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	static String findSingleClass(File classesDirectory) throws MojoExecutionException {
		return findSingleClass(List.of(classesDirectory));
	}

	static String findSingleClass(List<File> classesDirectories) throws MojoExecutionException {
		try {
			for (File classesDirectory : classesDirectories) {
				String mainClass = MainClassFinder.findSingleMainClass(classesDirectory,
						SPRING_BOOT_APPLICATION_CLASS_NAME);
				if (mainClass != null) {
					return mainClass;
				}
			}
			throw new MojoExecutionException("Unable to find a suitable main class, please add a 'mainClass' property");
		}
		catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

}
