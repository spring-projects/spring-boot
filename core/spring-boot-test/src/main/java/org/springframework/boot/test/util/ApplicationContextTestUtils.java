/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.util;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Application context related test utilities.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public abstract class ApplicationContextTestUtils {

	/**
	 * Closes this {@link ApplicationContext} and its parent hierarchy if any.
	 * @param context the context to close (can be {@code null})
	 */
	public static void closeAll(@Nullable ApplicationContext context) {
		if (context != null) {
			if (context instanceof ConfigurableApplicationContext configurableContext) {
				configurableContext.close();
			}
			closeAll(context.getParent());
		}
	}

}
