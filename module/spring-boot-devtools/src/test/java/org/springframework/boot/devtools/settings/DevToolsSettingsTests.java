/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.settings;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DevToolsSettings}.
 *
 * @author Phillip Webb
 */
class DevToolsSettingsTests {

	private static final String ROOT = DevToolsSettingsTests.class.getPackage().getName().replace('.', '/') + "/";

	@Test
	void includePatterns() throws Exception {
		DevToolsSettings settings = DevToolsSettings.load(ROOT + "spring-devtools-include.properties");
		assertThat(settings.isRestartInclude(new URL("file://test/a"))).isTrue();
		assertThat(settings.isRestartInclude(new URL("file://test/b"))).isTrue();
		assertThat(settings.isRestartInclude(new URL("file://test/c"))).isFalse();
	}

	@Test
	void excludePatterns() throws Exception {
		DevToolsSettings settings = DevToolsSettings.load(ROOT + "spring-devtools-exclude.properties");
		assertThat(settings.isRestartExclude(new URL("file://test/a"))).isTrue();
		assertThat(settings.isRestartExclude(new URL("file://test/b"))).isTrue();
		assertThat(settings.isRestartExclude(new URL("file://test/c"))).isFalse();
	}

	@Test
	void defaultIncludePatterns(@TempDir File tempDir) throws Exception {
		DevToolsSettings settings = DevToolsSettings.get();
		assertThat(settings.isRestartExclude(makeUrl(tempDir, "spring-boot"))).isTrue();
		assertThat(settings.isRestartExclude(makeUrl(tempDir, "spring-boot-autoconfigure"))).isTrue();
		assertThat(settings.isRestartExclude(makeUrl(tempDir, "spring-boot-actuator"))).isTrue();
		assertThat(settings.isRestartExclude(makeUrl(tempDir, "spring-boot-starter"))).isTrue();
		assertThat(settings.isRestartExclude(makeUrl(tempDir, "spring-boot-starter-some-thing"))).isTrue();
	}

	@Test
	void propertyDefaults() {
		DevToolsSettings settings = DevToolsSettings.load(ROOT + "spring-devtools-defaults.properties");
		Map<String, Object> propertyDefaults = settings.getPropertyDefaults();
		assertThat(propertyDefaults)
			.containsExactlyInAnyOrderEntriesOf(Map.of("com.example.a", "true", "com.example.b", "17"));
	}

	private URL makeUrl(File file, String name) throws IOException {
		file = new File(file, name);
		file = new File(file, "build");
		file = new File(file, "classes");
		file.mkdirs();
		return file.toURI().toURL();
	}

}
