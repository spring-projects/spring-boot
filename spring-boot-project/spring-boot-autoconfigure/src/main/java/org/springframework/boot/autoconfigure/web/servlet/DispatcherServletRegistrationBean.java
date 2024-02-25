/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.Collection;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.util.Assert;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link ServletRegistrationBean} for the auto-configured {@link DispatcherServlet}. Both
 * registers the servlet and exposes {@link DispatcherServletPath} information.
 *
 * @author Phillip Webb
 * @since 2.0.4
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
		Assert.notNull(path, "Path must not be null");
		this.path = path;
		super.addUrlMappings(getServletUrlMapping());
	}

	/**
     * Returns the path of the DispatcherServletRegistrationBean.
     *
     * @return the path of the DispatcherServletRegistrationBean
     */
    @Override
	public String getPath() {
		return this.path;
	}

	/**
     * Sets the URL mappings for the DispatcherServlet registration.
     * 
     * @param urlMappings the collection of URL mappings to be set
     * @throws UnsupportedOperationException if attempting to change the URL mapping on a DispatcherServlet registration
     */
    @Override
	public void setUrlMappings(Collection<String> urlMappings) {
		throw new UnsupportedOperationException("URL Mapping cannot be changed on a DispatcherServlet registration");
	}

	/**
     * Adds URL mappings to the DispatcherServlet registration.
     * 
     * @param urlMappings the URL mappings to be added
     * @throws UnsupportedOperationException if URL mapping cannot be changed on a DispatcherServlet registration
     */
    @Override
	public void addUrlMappings(String... urlMappings) {
		throw new UnsupportedOperationException("URL Mapping cannot be changed on a DispatcherServlet registration");
	}

}
