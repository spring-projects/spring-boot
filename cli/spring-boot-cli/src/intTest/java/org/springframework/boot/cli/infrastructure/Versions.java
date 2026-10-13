/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.infrastructure;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Provides access to the current Boot version by referring to {@code gradle.properties}.
 *
 * @author Andy Wilkinson
 */
final class Versions {

	private Versions() {
	}

	static String getBootVersion() {
		Properties gradleProperties = new Properties();
		try (FileInputStream input = new FileInputStream("../../gradle.properties")) {
			gradleProperties.load(input);
			return gradleProperties.getProperty("version");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
