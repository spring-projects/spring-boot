/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import java.util.Collection;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.util.Assert;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link ServletRegistrationBean} for the auto-configured {@link DispatcherServlet}. Both
 * registers the servlet and exposes {@link DispatcherServletPath} information.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class DispatcherServletRegistrationBean extends ServletRegistrationBean<DispatcherServlet>
		implements DispatcherServletPath {

	private final String path;

	/**
	 * Create a new {@link DispatcherServletRegistrationBean} instance for the given
	 * servlet and path.
	 * @param servlet the dispatcher servlet
	 * @param path the dispatcher servlet path
	 */
	public DispatcherServletRegistrationBean(DispatcherServlet servlet, String path) {
		super(servlet);
		Assert.notNull(path, "'path' must not be null");
		this.path = path;
		super.addUrlMappings(getServletUrlMapping());
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public void setUrlMappings(Collection<String> urlMappings) {
		throw new UnsupportedOperationException("URL Mapping cannot be changed on a DispatcherServlet registration");
	}

	@Override
	public void addUrlMappings(String... urlMappings) {
		throw new UnsupportedOperationException("URL Mapping cannot be changed on a DispatcherServlet registration");
	}

}
