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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLoader}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLoaderTests {

	private TestConfigDataLoader loader = new TestConfigDataLoader();

	private ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);

	@Test
	void isLoadableAlwaysReturnsTrue() {
		assertThat(this.loader.isLoadable(this.context, new TestConfigDataLocation())).isTrue();
	}

	static class TestConfigDataLoader implements ConfigDataLoader<TestConfigDataLocation> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, TestConfigDataLocation location) throws IOException {
			return null;
		}

	}

	static class TestConfigDataLocation extends ConfigDataLocation {

	}

}
