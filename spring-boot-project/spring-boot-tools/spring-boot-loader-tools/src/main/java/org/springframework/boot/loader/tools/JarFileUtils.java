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
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Utilities for working with {@link java.util.jar.JarFile}.
 *
 * @author Christoph Dreis
 * @since 2.4.0
 */
public abstract class JarFileUtils {

	/**
	 * Determines if a file has an attribute in the JARs manifest.
	 * @param file the jar file
	 * @param attribute name of the attribute
	 * @param acceptableValues list of acceptable values for the attribute
	 * @return {@code true} if attribute with the given name was found in the JAR
	 * manifest, {@code false} otherwise
	 */
	public static boolean hasManifestAttribute(File file, String attribute, String... acceptableValues) {
		try (JarFile jarFile = new JarFile(file)) {
			Attributes mainAttributes = jarFile.getManifest().getMainAttributes();
			String value = mainAttributes.getValue(new Attributes.Name(attribute));
			if (value == null) {
				return false;
			}
			return acceptableValues.length == 0 || Arrays.asList(acceptableValues).contains(value);
		}
		catch (Exception ignore) {
		}
		return false;
	}

}
