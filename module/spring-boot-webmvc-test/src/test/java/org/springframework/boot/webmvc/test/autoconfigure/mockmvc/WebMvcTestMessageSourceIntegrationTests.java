/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WebMvcTest @WebMvcTest} and {@link MessageSource}
 * auto-configuration.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
@TestPropertySource(properties = "spring.messages.basename=web-test-messages")
class WebMvcTestMessageSourceIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void messageSourceHasBeenAutoConfigured() {
		assertThat(this.context.getMessage("a", null, Locale.ENGLISH)).isEqualTo("alpha");
	}

}
