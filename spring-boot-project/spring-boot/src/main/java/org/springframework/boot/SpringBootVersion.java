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

package org.springframework.boot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Exposes the Spring Boot version.
 *
 * The version information is read from a file that is stored in the Spring Boot library.
 * If the version information cannot be read from the file, consider using a
 * reflection-based check instead (for example, checking for the presence of a specific
 * Spring Boot method that you intend to call).
 *
 * @author Drummond Dawson
 * @author Hendrig Sellik
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 1.3.0
 */
public final class SpringBootVersion {

	private SpringBootVersion() {
	}

	/**
	 * Return the full version string of the present Spring Boot codebase, or {@code null}
	 * if it cannot be determined.
	 * @return the version of Spring Boot or {@code null}
	 */
	public static String getVersion() {
		InputStream input = SpringBootVersion.class.getClassLoader()
				.getResourceAsStream("META-INF/spring-boot.properties");
		if (input != null) {
			try {
				Properties properties = new Properties();
				properties.load(input);
				return properties.getProperty("version");
			}
			catch (IOException ex) {
				// fall through
			}
		}
		return null;
	}

}
