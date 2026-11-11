/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.util;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ApplicationContextTestUtils}.
 *
 * @author Stephane Nicoll
 */
class ApplicationContextTestUtilsTests {

	@Test
	void closeNull() {
		ApplicationContextTestUtils.closeAll(null);
	}

	@Test
	void closeNonClosableContext() {
		ApplicationContext mock = mock(ApplicationContext.class);
		ApplicationContextTestUtils.closeAll(mock);
	}

	@Test
	void closeContextAndParent() {
		ConfigurableApplicationContext mock = mock(ConfigurableApplicationContext.class);
		ConfigurableApplicationContext parent = mock(ConfigurableApplicationContext.class);
		given(mock.getParent()).willReturn(parent);
		given(parent.getParent()).willReturn(null);
		ApplicationContextTestUtils.closeAll(mock);
		then(mock).should().getParent();
		then(mock).should().close();
		then(parent).should().getParent();
		then(parent).should().close();
	}

}
