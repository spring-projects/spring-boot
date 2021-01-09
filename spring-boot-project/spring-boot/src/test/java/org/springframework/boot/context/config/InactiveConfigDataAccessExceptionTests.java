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

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InactiveConfigDataAccessException}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class InactiveConfigDataAccessExceptionTests {

	private MockPropertySource propertySource = new MockPropertySource();

	private ConfigDataResource resource = new TestConfigDataResource();

	private String propertyName = "spring";

	private Origin origin = new PropertySourceOrigin(this.propertySource, this.propertyName);

	@Test
	void createHasCorrectMessage() {
		InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
				this.resource, this.propertyName, this.origin);
		assertThat(exception).hasMessage("Inactive property source 'mockProperties' imported from location 'test' "
				+ "cannot contain property 'spring' [origin: \"spring\" from property source \"mockProperties\"]");
	}

	@Test
	void createWhenNoLocationHasCorrectMessage() {
		InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource, null,
				this.propertyName, this.origin);
		assertThat(exception).hasMessage("Inactive property source 'mockProperties' "
				+ "cannot contain property 'spring' [origin: \"spring\" from property source \"mockProperties\"]");
	}

	@Test
	void createWhenNoOriginHasCorrectMessage() {
		InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
				this.resource, this.propertyName, null);
		assertThat(exception).hasMessage("Inactive property source 'mockProperties' imported from location 'test' "
				+ "cannot contain property 'spring'");
	}

	@Test
	void getPropertySourceReturnsPropertySource() {
		InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
				this.resource, this.propertyName, this.origin);
		assertThat(exception.getPropertySource()).isSameAs(this.propertySource);
	}

	@Test
	void getLocationReturnsLocation() {
		InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
				this.resource, this.propertyName, this.origin);
		assertThat(exception.getLocation()).isSameAs(this.resource);
	}

	@Test
	void getPropertyNameReturnsPropertyName() {
		InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
				this.resource, this.propertyName, this.origin);
		assertThat(exception.getPropertyName()).isSameAs(this.propertyName);
	}

	@Test
	void getOriginReturnsOrigin() {
		InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
				this.resource, this.propertyName, this.origin);
		assertThat(exception.getOrigin()).isSameAs(this.origin);
	}

	@Test
	void throwIfPropertyFoundWhenSourceIsNullDoesNothing() {
		ConfigDataEnvironmentContributor contributor = mock(ConfigDataEnvironmentContributor.class);
		given(contributor.getConfigurationPropertySource()).willReturn(null);
		InactiveConfigDataAccessException.throwIfPropertyFound(contributor, ConfigurationPropertyName.of("spring"));
	}

	@Test
	void throwIfPropertyFoundWhenPropertyNotFoundDoesNothing() {
		ConfigDataEnvironmentContributor contributor = mock(ConfigDataEnvironmentContributor.class);
		ConfigurationPropertySource configurationPropertySource = ConfigurationPropertySource.from(this.propertySource);
		given(contributor.getConfigurationPropertySource()).willReturn(configurationPropertySource);
		InactiveConfigDataAccessException.throwIfPropertyFound(contributor, ConfigurationPropertyName.of("spring"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void throwIfPropertyFoundWhenPropertyFoundThrowsException() {
		this.propertySource.setProperty("spring", "test");
		ConfigDataEnvironmentContributor contributor = mock(ConfigDataEnvironmentContributor.class);
		ConfigurationPropertySource configurationPropertySource = ConfigurationPropertySource.from(this.propertySource);
		given(contributor.getConfigurationPropertySource()).willReturn(configurationPropertySource);
		given(contributor.getPropertySource()).willReturn((PropertySource) this.propertySource);
		given(contributor.getResource()).willReturn(this.resource);
		assertThatExceptionOfType(InactiveConfigDataAccessException.class)
				.isThrownBy(() -> InactiveConfigDataAccessException.throwIfPropertyFound(contributor,
						ConfigurationPropertyName.of("spring")))
				.withMessage("Inactive property source 'mockProperties' imported from location 'test' "
						+ "cannot contain property 'spring' [origin: \"spring\" from property source \"mockProperties\"]");
	}

	private static class TestConfigDataResource extends ConfigDataResource {

		@Override
		public String toString() {
			return "test";
		}

	}

}
