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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.config.ConfigData.Option;
import org.springframework.boot.context.config.ConfigData.Options;
import org.springframework.boot.context.config.ConfigData.PropertySourceOptions;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

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
	@Deprecated
	void getDeprecatedOptionsReturnsCopyOfOptions() {
		MapPropertySource source = new MapPropertySource("test", Collections.emptyMap());
		Option[] options = { Option.IGNORE_IMPORTS };
		ConfigData configData = new ConfigData(Collections.singleton(source), options);
		options[0] = null;
		assertThat(configData.getOptions()).containsExactly(Option.IGNORE_IMPORTS);
	}

	@Test
	@Deprecated
	void getDeprecatedOptionsWhenUsingPropertySourceOptionsThrowsException() {
		MapPropertySource source = new MapPropertySource("test", Collections.emptyMap());
		PropertySourceOptions propertySourceOptions = (propertySource) -> Options.NONE;
		ConfigData configData = new ConfigData(Collections.singleton(source), propertySourceOptions);
		assertThatIllegalStateException().isThrownBy(() -> configData.getOptions())
				.withMessage("No global options defined");
	}

	@Test
	void getOptionsWhenOptionsSetAtConstructionAlwaysReturnsSameOptions() {
		MapPropertySource source = new MapPropertySource("test", Collections.emptyMap());
		ConfigData configData = new ConfigData(Collections.singleton(source), Option.IGNORE_IMPORTS);
		assertThat(configData.getOptions(source).asSet()).containsExactly(Option.IGNORE_IMPORTS);
	}

	@Test
	void getOptionsReturnsOptionsFromPropertySourceOptions() {
		MapPropertySource source1 = new MapPropertySource("test", Collections.emptyMap());
		MapPropertySource source2 = new MapPropertySource("test", Collections.emptyMap());
		Options options1 = Options.of(Option.IGNORE_IMPORTS);
		Options options2 = Options.of(Option.IGNORE_PROFILES);
		PropertySourceOptions propertySourceOptions = (source) -> (source != source1) ? options2 : options1;
		ConfigData configData = new ConfigData(Arrays.asList(source1, source2), propertySourceOptions);
		assertThat(configData.getOptions(source1)).isEqualTo(options1);
		assertThat(configData.getOptions(source2)).isEqualTo(options2);
	}

	@Test
	void getOptionsWhenPropertySourceOptionsReturnsNullReturnsNone() {
		MapPropertySource source = new MapPropertySource("test", Collections.emptyMap());
		PropertySourceOptions propertySourceOptions = (propertySource) -> null;
		ConfigData configData = new ConfigData(Collections.singleton(source), propertySourceOptions);
		assertThat(configData.getOptions(source)).isEqualTo(Options.NONE);
	}

	@Test
	void optionsOfCreatesOptions() {
		Options options = Options.of(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
		assertThat(options.asSet()).containsExactly(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
	}

	@Test
	void optionsOfUsesCopyOfOptions() {
		Option[] array = { Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES };
		Options options = Options.of(array);
		array[0] = Option.PROFILE_SPECIFIC;
		assertThat(options.asSet()).containsExactly(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
	}

	@Test
	void optionsNoneReturnsEmptyOptions() {
		assertThat(Options.NONE.asSet()).isEmpty();
	}

	@Test
	void optionsWithoutReturnsNewOptions() {
		Options options = Options.of(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
		Options without = options.without(Option.IGNORE_PROFILES);
		assertThat(options.asSet()).containsExactly(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
		assertThat(without.asSet()).containsExactly(Option.IGNORE_IMPORTS);
	}

	@Test
	void optionsWithReturnsNewOptions() {
		Options options = Options.of(Option.IGNORE_IMPORTS);
		Options with = options.with(Option.IGNORE_PROFILES);
		assertThat(options.asSet()).containsExactly(Option.IGNORE_IMPORTS);
		assertThat(with.asSet()).containsExactly(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
	}

	@Test
	void propertySourceOptionsAlwaysReturnsSameOptionsEachTime() {
		PropertySourceOptions options = PropertySourceOptions.always(Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
		assertThat(options.get(mock(PropertySource.class)).asSet()).containsExactly(Option.IGNORE_IMPORTS,
				Option.IGNORE_PROFILES);
	}

}
