/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.source;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FilteredIterableConfigurationPropertiesSource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class FilteredIterableConfigurationPropertiesSourceTests
		extends FilteredConfigurationPropertiesSourceTests {

	@Test
	public void iteratorShouldFilterNames() throws Exception {
		MockConfigurationPropertySource source = (MockConfigurationPropertySource) createTestSource();
		IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
		assertThat(filtered.iterator()).extracting(ConfigurationPropertyName::toString)
				.containsExactly("a", "b", "c");
	}

	@Override
	protected ConfigurationPropertySource convertSource(
			MockConfigurationPropertySource source) {
		return source;
	}

	@Test
	public void containsDescendantOfShouldUseContents() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar.baz", "1");
		source.put("foo.bar[0]", "1");
		source.put("faf.bar[0]", "1");
		IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("foo")))
				.isEqualTo(ConfigurationPropertyState.PRESENT);
		assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("faf")))
				.isEqualTo(ConfigurationPropertyState.ABSENT);
	}

	private boolean noBrackets(ConfigurationPropertyName name) {
		return name.toString().indexOf("[") == -1;
	}

}
