/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxProperties}
 *
 * @author Brian Clozel
 */
class WebFluxPropertiesTests {

	private final WebFluxProperties properties = new WebFluxProperties();

	@Test
	void shouldPrefixBasePathWithMissingSlash() {
		bind("spring.webflux.base-path", "something");
		assertThat(this.properties.getBasePath()).isEqualTo("/something");
	}

	@Test
	void shouldRemoveTrailingSlashFromBasePath() {
		bind("spring.webflux.base-path", "/something/");
		assertThat(this.properties.getBasePath()).isEqualTo("/something");
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("spring.webflux", Bindable.ofInstance(this.properties));
	}

}
