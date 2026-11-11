/*
 * Copyright 2012-present the original author or authors.
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
		environment.getPropertySources()
			.addFirst(new MockPropertySource().withProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "test"));
		assertThat(environment.getActiveProfiles()).isEmpty();
	}

	@Test
	void getDefaultProfilesDoesNotResolveProperty() {
		StandardEnvironment environment = createEnvironment();
		new MockPropertySource().withProperty("", "");
		environment.getPropertySources()
			.addFirst(
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
