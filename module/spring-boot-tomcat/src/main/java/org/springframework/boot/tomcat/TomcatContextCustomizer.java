/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.apache.catalina.Context;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;

/**
 * Callback interface that can be used to customize a Tomcat {@link Context}.
 *
 * @author Dave Syer
 * @since 4.0.0
 * @see TomcatServletWebServerFactory
 */
@FunctionalInterface
public interface TomcatContextCustomizer {

	/**
	 * Customize the context.
	 * @param context the context to customize
	 */
	void customize(Context context);

}
