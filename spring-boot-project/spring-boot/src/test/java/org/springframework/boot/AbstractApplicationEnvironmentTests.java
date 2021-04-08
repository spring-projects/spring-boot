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

package org.springframework.boot;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for {@link SpringApplication} {@link Environment} tests.
 *
 * @author Phillip Webb
 */
public abstract class AbstractApplicationEnvironmentTests {

	@Test
	void getActiveProfilesDoesNotResolveProperty() {
		StandardEnvironment environment = createEnvironment();
		new MockPropertySource().withProperty("", "");
		environment.getPropertySources().addFirst(
				new MockPropertySource().withProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "test"));
		assertThat(environment.getActiveProfiles()).isEmpty();
	}

	@Test
	void getDefaultProfilesDoesNotResolveProperty() {
		StandardEnvironment environment = createEnvironment();
		new MockPropertySource().withProperty("", "");
		environment.getPropertySources().addFirst(
				new MockPropertySource().withProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, "test"));
		assertThat(environment.getDefaultProfiles()).containsExactly("default");
	}

	@Test
	void propertyResolverIsOptimizedForConfigurationProperties() {
		StandardEnvironment environment = createEnvironment();
		ConfigurablePropertyResolver expected = ConfigurationPropertySources
				.createPropertyResolver(new MutablePropertySources());
		assertThat(environment).extracting("propertyResolver").hasSameClassAs(expected);
	}

	protected abstract StandardEnvironment createEnvironment();

}
