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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.config.ConfigData.Option;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigData}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataTests {

	@Test
	void createWhenPropertySourcesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigData(null))
				.withMessage("PropertySources must not be null");
	}

	@Test
	void createWhenOptionsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigData(Collections.emptyList(), (Option[]) null))
				.withMessage("Options must not be null");
	}

	@Test
	void getPropertySourcesReturnsCopyOfSources() {
		MapPropertySource source = new MapPropertySource("test", Collections.emptyMap());
		List<MapPropertySource> sources = new ArrayList<>(Collections.singleton(source));
		ConfigData configData = new ConfigData(sources);
		sources.clear();
		assertThat(configData.getPropertySources()).containsExactly(source);
	}

	@Test
	void getOptionsReturnsCopyOfOptions() {
		MapPropertySource source = new MapPropertySource("test", Collections.emptyMap());
		Option[] options = { Option.IGNORE_IMPORTS };
		ConfigData configData = new ConfigData(Collections.singleton(source), options);
		options[0] = null;
		assertThat(configData.getOptions()).containsExactly(Option.IGNORE_IMPORTS);
	}

}
