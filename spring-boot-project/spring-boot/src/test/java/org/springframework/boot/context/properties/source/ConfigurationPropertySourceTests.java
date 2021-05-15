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

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationPropertySource}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourceTests {

	@Test
	void fromCreatesConfigurationPropertySourcesPropertySource() {
		MockPropertySource source = new MockPropertySource();
		source.setProperty("spring", "boot");
		ConfigurationPropertySource adapted = ConfigurationPropertySource.from(source);
		assertThat(adapted.getConfigurationProperty(ConfigurationPropertyName.of("spring")).getValue())
				.isEqualTo("boot");
	}

	@Test
	void fromWhenSourceIsAlreadyConfigurationPropertySourcesPropertySourceReturnsNull() {
		ConfigurationPropertySourcesPropertySource source = mock(ConfigurationPropertySourcesPropertySource.class);
		assertThat(ConfigurationPropertySource.from(source)).isNull();
	}

}
