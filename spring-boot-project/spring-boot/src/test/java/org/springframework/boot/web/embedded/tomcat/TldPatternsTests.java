/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

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
