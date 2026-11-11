/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.env.PropertySource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigTreeConfigDataLoader}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigTreeConfigDataLoaderTests {

	private final ConfigTreeConfigDataLoader loader = new ConfigTreeConfigDataLoader();

	private final ConfigDataLoaderContext loaderContext = mock(ConfigDataLoaderContext.class);

	@TempDir
	Path directory;

	@Test
	void loadReturnsConfigDataWithPropertySource() throws IOException {
		File file = this.directory.resolve("hello").toFile();
		file.getParentFile().mkdirs();
		FileCopyUtils.copy("world\n".getBytes(StandardCharsets.UTF_8), file);
		ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource(this.directory.toString());
		ConfigData configData = this.loader.load(this.loaderContext, location);
		assertThat(configData.getPropertySources()).hasSize(1);
		PropertySource<?> source = configData.getPropertySources().get(0);
		assertThat(source.getName()).isEqualTo("Config tree '" + this.directory.toString() + "'");
		assertThat(source.getProperty("hello")).hasToString("world");
	}

	@Test
	void loadWhenPathDoesNotExistThrowsException() {
		File missing = this.directory.resolve("missing").toFile();
		ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource(missing.toString());
		assertThatExceptionOfType(ConfigDataResourceNotFoundException.class)
			.isThrownBy(() -> this.loader.load(this.loaderContext, location));
	}

}
