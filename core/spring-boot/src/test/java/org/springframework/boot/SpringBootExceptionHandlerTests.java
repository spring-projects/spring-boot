/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringBootExceptionHandler}.
 *
 * @author Henri Tremblay
 * @author Andy Wilkinson
 */
class SpringBootExceptionHandlerTests {

	private final UncaughtExceptionHandler parent = mock(UncaughtExceptionHandler.class);

	private final SpringBootExceptionHandler handler = new SpringBootExceptionHandler(this.parent);

	@Test
	void uncaughtExceptionDoesNotForwardLoggedErrorToParent() {
		Thread thread = Thread.currentThread();
		Exception ex = new Exception();
		this.handler.registerLoggedException(ex);
		this.handler.uncaughtException(thread, ex);
		then(this.parent).shouldHaveNoInteractions();
	}

	@Test
	void uncaughtExceptionForwardsLogConfigurationErrorToParent() {
		Thread thread = Thread.currentThread();
		Exception ex = new Exception("[stuff] Logback configuration error detected [stuff]");
		this.handler.registerLoggedException(ex);
		this.handler.uncaughtException(thread, ex);
		then(this.parent).should().uncaughtException(thread, ex);
	}

	@Test
	void uncaughtExceptionForwardsWrappedLogConfigurationErrorToParent() {
		Thread thread = Thread.currentThread();
		Exception ex = new InvocationTargetException(
				new Exception("[stuff] Logback configuration error detected [stuff]", new Exception()));
		this.handler.registerLoggedException(ex);
		this.handler.uncaughtException(thread, ex);
		then(this.parent).should().uncaughtException(thread, ex);
	}

}
