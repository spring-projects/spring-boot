/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.servlet.support;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.Ordered;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link ApplicationContextInitializer} for setting the servlet context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ServletContextApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableWebApplicationContext>, Ordered {

	private int order = Ordered.HIGHEST_PRECEDENCE;

	private final ServletContext servletContext;

	private final boolean addApplicationContextAttribute;

	/**
	 * Create a new {@link ServletContextApplicationContextInitializer} instance.
	 * @param servletContext the servlet that should be ultimately set.
	 */
	public ServletContextApplicationContextInitializer(ServletContext servletContext) {
		this(servletContext, false);
	}

	/**
	 * Create a new {@link ServletContextApplicationContextInitializer} instance.
	 * @param servletContext the servlet that should be ultimately set.
	 * @param addApplicationContextAttribute if the {@link ApplicationContext} should be
	 * stored as an attribute in the {@link ServletContext}
	 * @since 1.3.4
	 */
	public ServletContextApplicationContextInitializer(ServletContext servletContext,
			boolean addApplicationContextAttribute) {
		this.servletContext = servletContext;
		this.addApplicationContextAttribute = addApplicationContextAttribute;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void initialize(ConfigurableWebApplicationContext applicationContext) {
		applicationContext.setServletContext(this.servletContext);
		if (this.addApplicationContextAttribute) {
			this.servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
					applicationContext);
		}

	}

}
