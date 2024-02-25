/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.web.servlet.context;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;

/**
 * Variant of {@link ServletContextAwareProcessor} for use with a
 * {@link ConfigurableWebApplicationContext}. Can be used when registering the processor
 * can occur before the {@link ServletContext} or {@link ServletConfig} have been
 * initialized.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class WebApplicationContextServletContextAwareProcessor extends ServletContextAwareProcessor {

	private final ConfigurableWebApplicationContext webApplicationContext;

	/**
     * Constructs a new instance of the {@code WebApplicationContextServletContextAwareProcessor} class.
     * 
     * @param webApplicationContext the {@code ConfigurableWebApplicationContext} to be set as the web application context
     * @throws IllegalArgumentException if the {@code webApplicationContext} is null
     */
    public WebApplicationContextServletContextAwareProcessor(ConfigurableWebApplicationContext webApplicationContext) {
		Assert.notNull(webApplicationContext, "WebApplicationContext must not be null");
		this.webApplicationContext = webApplicationContext;
	}

	/**
     * Returns the ServletContext associated with this WebApplicationContext.
     * If the ServletContext is not available from the WebApplicationContext,
     * the ServletContext from the superclass is returned.
     *
     * @return the ServletContext associated with this WebApplicationContext,
     *         or the ServletContext from the superclass if not available
     */
    @Override
	protected ServletContext getServletContext() {
		ServletContext servletContext = this.webApplicationContext.getServletContext();
		return (servletContext != null) ? servletContext : super.getServletContext();
	}

	/**
     * Returns the servlet configuration for this servlet.
     * 
     * @return the servlet configuration for this servlet, or {@code null} if not available
     */
    @Override
	protected ServletConfig getServletConfig() {
		ServletConfig servletConfig = this.webApplicationContext.getServletConfig();
		return (servletConfig != null) ? servletConfig : super.getServletConfig();
	}

}
