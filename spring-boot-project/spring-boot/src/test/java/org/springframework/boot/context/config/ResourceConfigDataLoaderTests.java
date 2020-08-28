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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceConfigDataLoader}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class ResourceConfigDataLoaderTests {

	private ResourceConfigDataLoader loader = new ResourceConfigDataLoader();

	private ConfigDataLoaderContext loaderContext = mock(ConfigDataLoaderContext.class);

	@Test
	void loadWhenLocationResultsInMultiplePropertySourcesAddsAllToConfigData() throws IOException {
		ResourceConfigDataLocation location = new ResourceConfigDataLocation("application.yml",
				new ClassPathResource("configdata/yaml/application.yml"), null, new YamlPropertySourceLoader());
		ConfigData configData = this.loader.load(this.loaderContext, location);
		assertThat(configData.getPropertySources().size()).isEqualTo(2);
		PropertySource<?> source1 = configData.getPropertySources().get(0);
		PropertySource<?> source2 = configData.getPropertySources().get(1);
		assertThat(source1.getName()).isEqualTo("application.yml (document #0)");
		assertThat(source1.getProperty("foo")).isEqualTo("bar");
		assertThat(source2.getName()).isEqualTo("application.yml (document #1)");
		assertThat(source2.getProperty("hello")).isEqualTo("world");
	}

	@Test
	void loadWhenPropertySourceIsEmptyAddsNothingToConfigData() throws IOException {
		ResourceConfigDataLocation location = new ResourceConfigDataLocation("testproperties.properties",
				new ClassPathResource("config/0-empty/testproperties.properties"), null,
				new PropertiesPropertySourceLoader());
		ConfigData configData = this.loader.load(this.loaderContext, location);
		assertThat(configData.getPropertySources().size()).isEqualTo(0);
	}

}
