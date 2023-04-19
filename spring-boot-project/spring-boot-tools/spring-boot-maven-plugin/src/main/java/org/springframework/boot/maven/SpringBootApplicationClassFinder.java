/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;

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
		try {
			String mainClass = MainClassFinder.findSingleMainClass(classesDirectory,
					SPRING_BOOT_APPLICATION_CLASS_NAME);
			if (mainClass != null) {
				return mainClass;
			}
			throw new MojoExecutionException("Unable to find a suitable main class, please add a 'mainClass' property");
		}
		catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

}
