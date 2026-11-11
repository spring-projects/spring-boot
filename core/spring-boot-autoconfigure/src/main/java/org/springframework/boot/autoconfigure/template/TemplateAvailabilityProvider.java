/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.template;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * Indicates the availability of view templates for a particular templating engine such as
 * FreeMarker or Thymeleaf.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@FunctionalInterface
public interface TemplateAvailabilityProvider {

	/**
	 * Returns {@code true} if a template is available for the given {@code view}.
	 * @param view the view name
	 * @param environment the environment
	 * @param classLoader the class loader
	 * @param resourceLoader the resource loader
	 * @return if the template is available
	 */
	boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader,
			ResourceLoader resourceLoader);

}
