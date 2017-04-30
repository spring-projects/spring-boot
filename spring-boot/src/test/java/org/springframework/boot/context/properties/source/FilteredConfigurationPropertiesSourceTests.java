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

import java.util.Objects;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FilteredConfigurationPropertiesSource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class FilteredConfigurationPropertiesSourceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenSourceIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Source must not be null");
		new FilteredConfigurationPropertiesSource(null, Objects::nonNull);
	}

	@Test
	public void createWhenFilterIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Filter must not be null");
		new FilteredConfigurationPropertiesSource(new MockConfigurationPropertySource(),
				null);
	}

	@Test
	public void iteratorShouldFilterNames() throws Exception {
		MockConfigurationPropertySource source = createTestSource();
		ConfigurationPropertySource filtered = source.filter(this::noBrackets);
		assertThat(filtered.iterator()).extracting(ConfigurationPropertyName::toString)
				.containsExactly("a", "b", "c");
	}

	@Test
	public void getValueShouldFilterNames() throws Exception {
		MockConfigurationPropertySource source = createTestSource();
		ConfigurationPropertySource filtered = source.filter(this::noBrackets);
		ConfigurationPropertyName name = ConfigurationPropertyName.of("a");
		assertThat(source.getConfigurationProperty(name).getValue()).isEqualTo("1");
		assertThat(filtered.getConfigurationProperty(name).getValue()).isEqualTo("1");
		ConfigurationPropertyName bracketName = ConfigurationPropertyName.of("a[1]");
		assertThat(source.getConfigurationProperty(bracketName).getValue())
				.isEqualTo("2");
		assertThat(filtered.getConfigurationProperty(bracketName)).isNull();

	}

	private MockConfigurationPropertySource createTestSource() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("a", "1");
		source.put("a[1]", "2");
		source.put("b", "3");
		source.put("b[1]", "4");
		source.put("c", "5");
		return source;
	}

	private boolean noBrackets(ConfigurationPropertyName name) {
		return name.toString().indexOf("[") == -1;
	}

}
