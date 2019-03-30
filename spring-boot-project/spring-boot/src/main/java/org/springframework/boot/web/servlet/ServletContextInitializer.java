/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.WebApplicationInitializer;

/**
 * Interface used to configure a Servlet 3.0+ {@link ServletContext context}
 * programmatically. Unlike {@link WebApplicationInitializer}, classes that implement this
 * interface (and do not implement {@link WebApplicationInitializer}) will <b>not</b> be
 * detected by {@link SpringServletContainerInitializer} and hence will not be
 * automatically bootstrapped by the Servlet container.
 * <p>
 * This interface is primarily designed to allow {@link ServletContextInitializer}s to be
 * managed by Spring and not the Servlet container.
 * <p>
 * For configuration examples see {@link WebApplicationInitializer}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see WebApplicationInitializer
 */
@FunctionalInterface
public interface ServletContextInitializer {

	/**
	 * Configure the given {@link ServletContext} with any servlets, filters, listeners
	 * context-params and attributes necessary for initialization.
	 * @param servletContext the {@code ServletContext} to initialize
	 * @throws ServletException if any call against the given {@code ServletContext}
	 * throws a {@code ServletException}
	 */
	void onStartup(ServletContext servletContext) throws ServletException;

}
