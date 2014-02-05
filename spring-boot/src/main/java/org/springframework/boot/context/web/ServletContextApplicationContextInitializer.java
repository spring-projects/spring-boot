/*
 * Copyright 2010-2012 the original author or authors.
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

package org.springframework.boot.context.web;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.Ordered;
import org.springframework.web.context.ConfigurableWebApplicationContext;

/**
 * {@link ApplicationContextInitializer} for setting the servlet context.
 * 
 * @author Dave Syer
 */
public class ServletContextApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableWebApplicationContext>, Ordered {

	private int order = Integer.MIN_VALUE;

	private final ServletContext servletContext;

	/**
	 * @param servletContext
	 */
	public ServletContextApplicationContextInitializer(ServletContext servletContext) {
		this.servletContext = servletContext;
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
	}

}
