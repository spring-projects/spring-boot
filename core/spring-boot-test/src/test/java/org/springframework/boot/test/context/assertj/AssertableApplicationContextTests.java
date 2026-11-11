/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link AssertableApplicationContext}.
 *
 * @author Phillip Webb
 * @see ApplicationContextAssertProviderTests
 */
class AssertableApplicationContextTests {

	@Test
	@SuppressWarnings("resource")
	void getShouldReturnProxy() {
		AssertableApplicationContext context = AssertableApplicationContext
			.get(() -> mock(ConfigurableApplicationContext.class));
		assertThat(context).isInstanceOf(ConfigurableApplicationContext.class);
	}

	@Test
	void getWhenHasAdditionalInterfaceShouldReturnProxy() {
		try (AssertableApplicationContext context = AssertableApplicationContext.get(
				() -> mock(ConfigurableApplicationContext.class,
						withSettings().extraInterfaces(AdditionalContextInterface.class)),
				AdditionalContextInterface.class)) {
			assertThat(context).isInstanceOf(ConfigurableApplicationContext.class)
				.isInstanceOf(AdditionalContextInterface.class);
		}
	}

}
