/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityFilterProperties}.
 *
 * @author Dave Syer
 * @author Madhura Bhave
 */
class SecurityFilterPropertiesTests {

	private final SecurityFilterProperties properties = new SecurityFilterProperties();

	private Binder binder;

	private final MapConfigurationPropertySource source = new MapConfigurationPropertySource();

	@BeforeEach
	void setUp() {
		this.binder = new Binder(this.source);
	}

	@Test
	void validateDefaultFilterOrderMatchesMetadata() {
		assertThat(this.properties.getOrder()).isEqualTo(-100);
	}

	@Test
	void filterOrderShouldBind() {
		this.source.put("spring.security.filter.order", "55");
		this.binder.bind("spring.security.filter", Bindable.ofInstance(this.properties));
		assertThat(this.properties.getOrder()).isEqualTo(55);
	}

}
