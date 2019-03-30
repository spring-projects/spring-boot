/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.util;

import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues.Type;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestPropertyValues}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class TestPropertyValuesTests {

	private final ConfigurableEnvironment environment = new StandardEnvironment();

	@Test
	public void applyToEnvironmentShouldAttachConfigurationPropertySource() {
		TestPropertyValues.of("foo.bar=baz").applyTo(this.environment);
		PropertySource<?> source = this.environment.getPropertySources()
				.get("configurationProperties");
		assertThat(source).isNotNull();
	}

	@Test
	public void applyToDefaultPropertySource() {
		TestPropertyValues.of("foo.bar=baz", "hello.world=hi").applyTo(this.environment);
		assertThat(this.environment.getProperty("foo.bar")).isEqualTo("baz");
		assertThat(this.environment.getProperty("hello.world")).isEqualTo("hi");
	}

	@Test
	public void applyToSystemPropertySource() {
		TestPropertyValues.of("FOO_BAR=BAZ").applyTo(this.environment,
				Type.SYSTEM_ENVIRONMENT);
		assertThat(this.environment.getProperty("foo.bar")).isEqualTo("BAZ");
		assertThat(this.environment.getPropertySources().contains(
				"test-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
						.isTrue();
	}

	@Test
	public void applyToWithSpecificName() {
		TestPropertyValues.of("foo.bar=baz").applyTo(this.environment, Type.MAP, "other");
		assertThat(this.environment.getPropertySources().get("other")).isNotNull();
		assertThat(this.environment.getProperty("foo.bar")).isEqualTo("baz");
	}

	@Test
	public void applyToExistingNameAndDifferentTypeShouldOverrideExistingOne() {
		TestPropertyValues.of("foo.bar=baz", "hello.world=hi").applyTo(this.environment,
				Type.MAP, "other");
		TestPropertyValues.of("FOO_BAR=BAZ").applyTo(this.environment,
				Type.SYSTEM_ENVIRONMENT, "other");
		assertThat(this.environment.getPropertySources().get("other"))
				.isInstanceOf(SystemEnvironmentPropertySource.class);
		assertThat(this.environment.getProperty("foo.bar")).isEqualTo("BAZ");
		assertThat(this.environment.getProperty("hello.world")).isNull();
	}

	@Test
	public void applyToExistingNameAndSameTypeShouldMerge() {
		TestPropertyValues.of("foo.bar=baz", "hello.world=hi").applyTo(this.environment,
				Type.MAP);
		TestPropertyValues.of("foo.bar=new").applyTo(this.environment, Type.MAP);
		assertThat(this.environment.getProperty("foo.bar")).isEqualTo("new");
		assertThat(this.environment.getProperty("hello.world")).isEqualTo("hi");
	}

	@Test
	public void andShouldChainAndAddSingleKeyValue() {
		TestPropertyValues.of("foo.bar=baz").and("hello.world=hi").and("bling.blah=bing")
				.applyTo(this.environment, Type.MAP);
		assertThat(this.environment.getProperty("foo.bar")).isEqualTo("baz");
		assertThat(this.environment.getProperty("hello.world")).isEqualTo("hi");
		assertThat(this.environment.getProperty("bling.blah")).isEqualTo("bing");
	}

	@Test
	public void applyToSystemPropertiesShouldSetSystemProperties() {
		TestPropertyValues.of("foo=bar").applyToSystemProperties(() -> {
			assertThat(System.getProperty("foo")).isEqualTo("bar");
			return null;
		});
	}

	@Test
	public void applyToSystemPropertiesShouldRestoreSystemProperties() {
		System.setProperty("foo", "bar1");
		System.clearProperty("baz");
		try {
			TestPropertyValues.of("foo=bar2", "baz=bing").applyToSystemProperties(() -> {
				assertThat(System.getProperty("foo")).isEqualTo("bar2");
				assertThat(System.getProperty("baz")).isEqualTo("bing");
				return null;
			});
			assertThat(System.getProperty("foo")).isEqualTo("bar1");
			assertThat(System.getProperties()).doesNotContainKey("baz");
		}
		finally {
			System.clearProperty("foo");
		}
	}

	@Test
	public void applyToSystemPropertiesWhenValueIsNullShouldRemoveProperty() {
		System.setProperty("foo", "bar1");
		try {
			TestPropertyValues.of("foo").applyToSystemProperties(() -> {
				assertThat(System.getProperties()).doesNotContainKey("foo");
				return null;
			});
			assertThat(System.getProperty("foo")).isEqualTo("bar1");
		}
		finally {
			System.clearProperty("foo");
		}
	}

}
