/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConfigurationPropertyCaching}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertyCachingTests {

	private StandardEnvironment environment;

	private MapPropertySource propertySource;

	@BeforeEach
	void setup() {
		this.environment = new StandardEnvironment();
		this.propertySource = new MapPropertySource("test", Collections.singletonMap("spring", "boot"));
		this.environment.getPropertySources().addLast(this.propertySource);
	}

	@Test
	void getFromEnvironmentReturnsCaching() {
		ConfigurationPropertyCaching caching = ConfigurationPropertyCaching.get(this.environment);
		assertThat(caching).isInstanceOf(ConfigurationPropertySourcesCaching.class);
	}

	@Test
	void getFromEnvironmentForUnderlyingSourceReturnsCaching() {
		ConfigurationPropertyCaching caching = ConfigurationPropertyCaching.get(this.environment, this.propertySource);
		assertThat(caching).isInstanceOf(SoftReferenceConfigurationPropertyCache.class);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void getFromSourcesWhenSourcesIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ConfigurationPropertyCaching.get((Iterable<ConfigurationPropertySource>) null))
			.withMessage("'sources' must not be null");
	}

	@Test
	void getFromSourcesReturnsCachingComposite() {
		List<ConfigurationPropertySource> sources = new ArrayList<>();
		sources.add(SpringConfigurationPropertySource.from(this.propertySource));
		ConfigurationPropertyCaching caching = ConfigurationPropertyCaching.get(sources);
		assertThat(caching).isInstanceOf(ConfigurationPropertySourcesCaching.class);
	}

	@Test
	void getFromSourcesForUnderlyingSourceReturnsCaching() {
		List<ConfigurationPropertySource> sources = new ArrayList<>();
		sources.add(SpringConfigurationPropertySource.from(this.propertySource));
		ConfigurationPropertyCaching caching = ConfigurationPropertyCaching.get(sources, this.propertySource);
		assertThat(caching).isInstanceOf(SoftReferenceConfigurationPropertyCache.class);
	}

	@Test
	void getFromSourcesForUnderlyingSourceWhenCantFindThrowsException() {
		List<ConfigurationPropertySource> sources = new ArrayList<>();
		sources.add(SpringConfigurationPropertySource.from(this.propertySource));
		MapPropertySource anotherPropertySource = new MapPropertySource("test2", Collections.emptyMap());
		assertThatIllegalStateException()
			.isThrownBy(() -> ConfigurationPropertyCaching.get(sources, anotherPropertySource))
			.withMessage("Unable to find cache from configuration property sources");
	}

}
