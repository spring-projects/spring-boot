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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides access to various versions.
 *
 * @author Andy Wilkinson
 */
class Versions {

	private final Map<String, String> versions;

	Versions() {
		this.versions = loadVersions();
	}

	private static Map<String, String> loadVersions() {
		try (InputStream input = Versions.class.getClassLoader().getResourceAsStream("extracted-versions.properties")) {
			Properties properties = new Properties();
			properties.load(input);
			Map<String, String> versions = new HashMap<>();
			properties.forEach((key, value) -> versions.put((String) key, (String) value));
			return versions;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	String get(String name) {
		return this.versions.get(name);
	}

	Map<String, String> asMap() {
		return Collections.unmodifiableMap(this.versions);
	}

}
