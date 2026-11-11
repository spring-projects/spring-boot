/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.test.autoconfigure.webmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SpringSecurity with {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
class SecurityWebMvcTestIntegrationTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	void includesWebSecurityConfigurer() {
		assertThat(AssertableApplicationContext.get(() -> this.context))
			.hasSingleBean(ExampleWebSecurityConfigurer.class);
	}

	@Test
	void includesWebSecurityCustomizer() {
		assertThat(AssertableApplicationContext.get(() -> this.context))
			.hasSingleBean(ExampleWebSecurityCustomizer.class);
	}

	@Test
	void includesSecurityFilterChain() {
		assertThat(AssertableApplicationContext.get(() -> this.context)).hasSingleBean(SecurityFilterChain.class);
	}

}
