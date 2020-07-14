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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.config.ConfigDataProperties.Activate;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConfigDataProperties}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataPropertiesTests {

	private static final CloudPlatform NULL_CLOUD_PLATFORM = null;

	private static final Profiles NULL_PROFILES = null;

	private static final List<String> NO_IMPORTS = Collections.emptyList();

	@Test
	void getImportsReturnsImports() {
		List<String> imports = Arrays.asList("one", "two", "three");
		ConfigDataProperties properties = new ConfigDataProperties(imports, null);
		assertThat(properties.getImports()).containsExactly("one", "two", "three");
	}

	@Test
	void getImportsWhenImportsAreNullReturnsEmptyList() {
		ConfigDataProperties properties = new ConfigDataProperties(null, null);
		assertThat(properties.getImports()).isEmpty();
	}

	@Test
	void isActiveWhenNullCloudPlatformAgainstNullCloudPlatform() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, new Activate(null, null));
		ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM, NULL_PROFILES);
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void isActiveWhenNullCloudPlatformAgainstSpecificCloudPlatform() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, new Activate(null, null));
		ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.KUBERNETES, NULL_PROFILES);
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void isActiveWhenSpecificCloudPlatformAgainstNullCloudPlatform() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
				new Activate(CloudPlatform.KUBERNETES, null));
		ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM, NULL_PROFILES);
		assertThat(properties.isActive(context)).isFalse();
	}

	@Test
	void isActiveWhenSpecificCloudPlatformAgainstMatchingSpecificCloudPlatform() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
				new Activate(CloudPlatform.KUBERNETES, null));
		ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.KUBERNETES, NULL_PROFILES);
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void isActiveWhenSpecificCloudPlatformAgainstDifferentSpecificCloudPlatform() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
				new Activate(CloudPlatform.KUBERNETES, null));
		ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.HEROKU, NULL_PROFILES);
		assertThat(properties.isActive(context)).isFalse();
	}

	@Test
	void isActiveWhenNullProfilesAgainstNullProfiles() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, new Activate(null, null));
		ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM, NULL_PROFILES);
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void isActiveWhenNullProfilesAgainstSpecificProfiles() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, new Activate(null, null));
		ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
				createTestProfiles());
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void isActiveWhenSpecificProfilesAgainstNullProfiles() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
				new Activate(null, new String[] { "a" }));
		ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM, null);
		assertThat(properties.isActive(context)).isFalse();
	}

	@Test
	void isActiveWhenSpecificProfilesAgainstMatchingSpecificProfiles() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
				new Activate(null, new String[] { "a" }));
		ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
				createTestProfiles());
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void isActiveWhenSpecificProfilesAgainstMissingSpecificProfiles() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
				new Activate(null, new String[] { "x" }));
		ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
				createTestProfiles());
		assertThat(properties.isActive(context)).isFalse();
	}

	@Test
	void isActiveWhenProfileExpressionAgainstSpecificProfiles() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
				new Activate(null, new String[] { "a | b" }));
		ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
				createTestProfiles());
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void isActiveWhenActivateIsNull() {
		ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, null);
		ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
				createTestProfiles());
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void isActiveAgainstBoundData() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("spring.config.import", "one,two,three");
		source.put("spring.config.activate.on-cloud-platform", "kubernetes");
		source.put("spring.config.activate.on-profiles", "a | b");
		Binder binder = new Binder(source);
		ConfigDataProperties properties = ConfigDataProperties.get(binder);
		ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.KUBERNETES,
				createTestProfiles());
		assertThat(properties.getImports()).containsExactly("one", "two", "three");
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void isActiveWhenBindingToLegacyProperty() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("spring.profiles", "a,b");
		Binder binder = new Binder(source);
		ConfigDataProperties properties = ConfigDataProperties.get(binder);
		ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.KUBERNETES,
				createTestProfiles());
		assertThat(properties.isActive(context)).isTrue();
	}

	@Test
	void getWhenHasLegacyAndNewPropertyThrowsException() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("spring.profiles", "a,b");
		source.put("spring.config.activate.on-profile", "a | b");
		Binder binder = new Binder(source);
		assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
				.isThrownBy(() -> ConfigDataProperties.get(binder));
	}

	private Profiles createTestProfiles() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("a", "b", "c");
		environment.setDefaultProfiles("d", "e", "f");
		Binder binder = Binder.get(environment);
		return new Profiles(environment, binder, null);
	}

}
