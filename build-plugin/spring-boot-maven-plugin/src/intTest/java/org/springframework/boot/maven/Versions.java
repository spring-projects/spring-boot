/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

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

	@Nullable String get(String name) {
		return this.versions.get(name);
	}

	Map<String, String> asMap() {
		return Collections.unmodifiableMap(this.versions);
	}

}
