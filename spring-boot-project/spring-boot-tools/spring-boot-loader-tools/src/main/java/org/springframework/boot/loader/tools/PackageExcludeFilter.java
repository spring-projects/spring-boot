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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Callback interface used to filter excluded packages.
 *
 * @author Christoph Dreis
 * @since 2.4.0
 */
@FunctionalInterface
public interface PackageExcludeFilter {

	/**
	 * Default pattern for Spring-Boot JAR names.
	 */
	Pattern SPRING_BOOT_JAR_PATTERN = Pattern.compile("spring-boot-.*\\.jar");

	/**
	 * Default filter.
	 */
	PackageExcludeFilter DEFAULT = (name, file) -> {
		if (SPRING_BOOT_JAR_PATTERN.matcher(name).matches()) {
			return JarFileUtils.hasManifestAttribute(file, "Exclude-From-Packaging");
		}
		return false;
	};

	/**
	 * Checks whether or not a given package should be excluded.
	 * @param name the name of the package
	 * @param file the file of the package
	 * @return {@code true} if the given package is excluded, {@code false} otherwise
	 */
	boolean isExcluded(String name, File file);

}
