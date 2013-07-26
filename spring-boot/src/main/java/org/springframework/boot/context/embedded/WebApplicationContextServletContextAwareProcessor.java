/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

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
 */
public class WebApplicationContextServletContextAwareProcessor extends
		ServletContextAwareProcessor {

	private final ConfigurableWebApplicationContext webApplicationContext;

	public WebApplicationContextServletContextAwareProcessor(
			ConfigurableWebApplicationContext webApplicationContext) {
		Assert.notNull(webApplicationContext, "WebApplicationContext must not be null");
		this.webApplicationContext = webApplicationContext;
	}

	@Override
	protected ServletContext getServletContext() {
		ServletContext servletContext = this.webApplicationContext.getServletContext();
		return (servletContext != null ? servletContext : super.getServletContext());
	}

	@Override
	protected ServletConfig getServletConfig() {
		ServletConfig servletConfig = this.webApplicationContext.getServletConfig();
		return (servletConfig != null ? servletConfig : super.getServletConfig());
	}
}
