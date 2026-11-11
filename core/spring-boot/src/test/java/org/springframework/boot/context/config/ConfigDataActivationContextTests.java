/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataActivationContext}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataActivationContextTests {

	@Test
	void getCloudPlatformWhenCloudPropertyNotPresentDeducesCloudPlatform() {
		Environment environment = new MockEnvironment();
		Binder binder = Binder.get(environment);
		ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
		assertThat(context.getCloudPlatform()).isNull();
	}

	@Test
	void getCloudPlatformWhenCloudPropertyInEnvironmentDeducesCloudPlatform() {
		MockEnvironment environment = createKubernetesEnvironment();
		Binder binder = Binder.get(environment);
		ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
		assertThat(context.getCloudPlatform()).isEqualTo(CloudPlatform.KUBERNETES);
	}

	@Test
	void getCloudPlatformWhenCloudPropertyHasBeenContributedDuringInitialLoadDeducesCloudPlatform() {
		Environment environment = createKubernetesEnvironment();
		Binder binder = new Binder(
				new MapConfigurationPropertySource(Collections.singletonMap("spring.main.cloud-platform", "HEROKU")));
		ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
		assertThat(context.getCloudPlatform()).isEqualTo(CloudPlatform.HEROKU);
	}

	@Test
	void getProfilesWhenWithoutProfilesReturnsNull() {
		Environment environment = new MockEnvironment();
		Binder binder = Binder.get(environment);
		ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
		assertThat(context.getProfiles()).isNull();
	}

	@Test
	void getProfilesWhenWithProfilesReturnsProfiles() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("a", "b", "c");
		Binder binder = Binder.get(environment);
		ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
		Profiles profiles = new Profiles(environment, binder, null);
		context = context.withProfiles(profiles);
		assertThat(context.getProfiles()).isEqualTo(profiles);
	}

	private MockEnvironment createKubernetesEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("KUBERNETES_SERVICE_HOST", "host");
		map.put("KUBERNETES_SERVICE_PORT", "port");
		PropertySource<?> propertySource = new MapPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, map);
		environment.getPropertySources().addLast(propertySource);
		return environment;
	}

}
