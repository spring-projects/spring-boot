/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.context.reactive.ConfigurableReactiveWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link AssertableReactiveWebApplicationContext}.
 *
 * @author Phillip Webb
 * @see ApplicationContextAssertProviderTests
 */
class AssertableReactiveWebApplicationContextTests {

	@Test
	@SuppressWarnings("resource")
	void getShouldReturnProxy() {
		AssertableReactiveWebApplicationContext context = AssertableReactiveWebApplicationContext
			.get(() -> mock(ConfigurableReactiveWebApplicationContext.class));
		assertThat(context).isInstanceOf(ConfigurableReactiveWebApplicationContext.class);
	}

	@Test
	void getWhenHasAdditionalInterfaceShouldReturnProxy() {
		try (AssertableReactiveWebApplicationContext context = AssertableReactiveWebApplicationContext.get(
				() -> mock(ConfigurableReactiveWebApplicationContext.class,
						withSettings().extraInterfaces(AdditionalContextInterface.class)),
				AdditionalContextInterface.class)) {
			assertThat(context).isInstanceOf(ConfigurableReactiveWebApplicationContext.class)
				.isInstanceOf(AdditionalContextInterface.class);
		}
	}

}
