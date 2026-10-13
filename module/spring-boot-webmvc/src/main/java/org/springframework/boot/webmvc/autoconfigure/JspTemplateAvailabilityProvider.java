/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import java.io.File;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for JSP
 * view templates.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 4.0.0
 */
public class JspTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

	@Override
	public boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader,
			ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent("org.apache.jasper.compiler.JspConfig", classLoader)) {
			String resourceName = getResourceName(view, environment);
			if (resourceLoader.getResource(resourceName).exists()) {
				return true;
			}
			return new File("src/main/webapp", resourceName).exists();
		}
		return false;
	}

	private String getResourceName(String view, Environment environment) {
		String prefix = environment.getProperty("spring.mvc.view.prefix", WebMvcAutoConfiguration.DEFAULT_PREFIX);
		String suffix = environment.getProperty("spring.mvc.view.suffix", WebMvcAutoConfiguration.DEFAULT_SUFFIX);
		return prefix + view + suffix;
	}

}
