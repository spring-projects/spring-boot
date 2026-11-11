/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLocationResolver}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLocationResolverTests {

	private final ConfigDataLocationResolver<?> resolver = new TestConfigDataLocationResolver();

	private final ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

	@Test
	void resolveProfileSpecificReturnsEmptyList() {
		ConfigDataLocation location = ConfigDataLocation.of("location");
		MockEnvironment environment = new MockEnvironment();
		Profiles profiles = new Profiles(environment, Binder.get(environment), null);
		assertThat(this.resolver.resolveProfileSpecific(this.context, location, profiles)).isEmpty();
	}

	static class TestConfigDataLocationResolver implements ConfigDataLocationResolver<ConfigDataResource> {

		@Override
		public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
			return true;
		}

		@Override
		public List<ConfigDataResource> resolve(ConfigDataLocationResolverContext context,
				ConfigDataLocation location) {
			return Collections.emptyList();
		}

	}

}
