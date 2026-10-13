/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TldPatterns}.
 *
 * @author Phillip Webb
 */
class TldPatternsTests {

	@Test
	void tomcatSkipAlignsWithTomcatDefaults() throws IOException {
		assertThat(TldPatterns.TOMCAT_SKIP).containsExactlyInAnyOrderElementsOf(getTomcatDefaultJarsToSkip());
	}

	@Test
	void tomcatScanAlignsWithTomcatDefaults() throws IOException {
		assertThat(TldPatterns.TOMCAT_SCAN).containsExactlyInAnyOrderElementsOf(getTomcatDefaultJarsToScan());
	}

	private Set<String> getTomcatDefaultJarsToSkip() throws IOException {
		return getTomcatDefault("tomcat.util.scan.StandardJarScanFilter.jarsToSkip");
	}

	private Set<String> getTomcatDefaultJarsToScan() throws IOException {
		return getTomcatDefault("tomcat.util.scan.StandardJarScanFilter.jarsToScan");
	}

	private Set<String> getTomcatDefault(String key) throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		try (InputStream inputStream = classLoader.getResource("catalina.properties").openStream()) {
			Properties properties = new Properties();
			properties.load(inputStream);
			String jarsToSkip = properties.getProperty(key);
			return StringUtils.commaDelimitedListToSet(jarsToSkip);
		}
	}

}
