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
public class ConfigTreeConfigDataLoaderTests {

	private ConfigTreeConfigDataLoader loader = new ConfigTreeConfigDataLoader();

	private ConfigDataLoaderContext loaderContext = mock(ConfigDataLoaderContext.class);

	@TempDir
	Path directory;

	@Test
	void loadReturnsConfigDataWithPropertySource() throws IOException {
		File file = this.directory.resolve("hello").toFile();
		file.getParentFile().mkdirs();
		FileCopyUtils.copy("world".getBytes(StandardCharsets.UTF_8), file);
		ConfigTreeConfigDataLocation location = new ConfigTreeConfigDataLocation(this.directory.toString());
		ConfigData configData = this.loader.load(this.loaderContext, location);
		assertThat(configData.getPropertySources().size()).isEqualTo(1);
		PropertySource<?> source = configData.getPropertySources().get(0);
		assertThat(source.getName()).isEqualTo("Config tree '" + this.directory.toString() + "'");
		assertThat(source.getProperty("hello").toString()).isEqualTo("world");
	}

	@Test
	void loadWhenPathDoesNotExistThrowsException() {
		File missing = this.directory.resolve("missing").toFile();
		ConfigTreeConfigDataLocation location = new ConfigTreeConfigDataLocation(missing.toString());
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
				.isThrownBy(() -> this.loader.load(this.loaderContext, location));
	}

}
