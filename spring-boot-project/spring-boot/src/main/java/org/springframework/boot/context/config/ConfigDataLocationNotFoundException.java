/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.Resource;

/**
 * Exception thrown when a config data location cannot be found.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class ConfigDataLocationNotFoundException extends ConfigDataException {

	private final ConfigDataLocation location;

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param location the location that was not found
	 */
	public ConfigDataLocationNotFoundException(ConfigDataLocation location) {
		this(location, null);
	}

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param location the location that was not found
	 * @param cause the cause of the exception
	 */
	public ConfigDataLocationNotFoundException(ConfigDataLocation location, Throwable cause) {
		this(getMessage(location), location, cause);
	}

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param message the exception message
	 * @param location the location that was not found
	 */
	public ConfigDataLocationNotFoundException(String message, ConfigDataLocation location) {
		this(message, location, null);
	}

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param message the exception message
	 * @param location the location that was not found
	 * @param cause the cause of the exception
	 */
	public ConfigDataLocationNotFoundException(String message, ConfigDataLocation location, Throwable cause) {
		super(message, cause);
		this.location = location;
	}

	/**
	 * Return the location that could not be found.
	 * @return the location that could not be found.
	 */
	public ConfigDataLocation getLocation() {
		return this.location;
	}

	private static String getMessage(ConfigDataLocation location) {
		return "Config data location '" + location + "' does not exist";
	}

	/**
	 * Throw a {@link ConfigDataLocationNotFoundException} if the specified {@link Path}
	 * does not exist.
	 * @param location the location being checked
	 * @param path the path to check
	 */
	public static void throwIfDoesNotExist(ConfigDataLocation location, Path path) {
		throwIfDoesNotExist(location, Files.exists(path));
	}

	/**
	 * Throw a {@link ConfigDataLocationNotFoundException} if the specified {@link File}
	 * does not exist.
	 * @param location the location being checked
	 * @param file the file to check
	 */
	public static void throwIfDoesNotExist(ConfigDataLocation location, File file) {
		throwIfDoesNotExist(location, file.exists());
	}

	/**
	 * Throw a {@link ConfigDataLocationNotFoundException} if the specified
	 * {@link Resource} does not exist.
	 * @param location the location being checked
	 * @param resource the resource to check
	 */
	public static void throwIfDoesNotExist(ConfigDataLocation location, Resource resource) {
		throwIfDoesNotExist(location, resource.exists());
	}

	private static void throwIfDoesNotExist(ConfigDataLocation location, boolean exists) {
		if (!exists) {
			throw new ConfigDataLocationNotFoundException(location);
		}
	}

}
