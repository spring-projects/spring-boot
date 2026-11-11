/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.web.context.ConfigurableWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link AssertableWebApplicationContext}.
 *
 * @author Phillip Webb
 * @see ApplicationContextAssertProviderTests
 */
class AssertableWebApplicationContextTests {

	@Test
	@SuppressWarnings("resource")
	void getShouldReturnProxy() {
		AssertableWebApplicationContext context = AssertableWebApplicationContext
			.get(() -> mock(ConfigurableWebApplicationContext.class));
		assertThat(context).isInstanceOf(ConfigurableWebApplicationContext.class);
	}

	@Test
	void getWhenHasAdditionalInterfaceShouldReturnProxy() {
		try (ConfigurableWebApplicationContext context = AssertableWebApplicationContext.get(
				() -> mock(ConfigurableWebApplicationContext.class,
						withSettings().extraInterfaces(AdditionalContextInterface.class)),
				AdditionalContextInterface.class)) {
			assertThat(context).isInstanceOf(ConfigurableWebApplicationContext.class)
				.isInstanceOf(AdditionalContextInterface.class);
		}
	}

}
