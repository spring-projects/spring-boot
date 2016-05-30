/*
 * Copyright 2012-2016 the original author or authors.
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

/**
 * {@link ApplicationContextInitializer} for setting the servlet context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @deprecated as of 1.4 in favor of
 * org.springframework.boot.web.support.ServletContextApplicationContextInitializer
 */
@Deprecated
public class ServletContextApplicationContextInitializer extends
		org.springframework.boot.web.support.ServletContextApplicationContextInitializer {

	public ServletContextApplicationContextInitializer(ServletContext servletContext,
			boolean addApplicationContextAttribute) {
		super(servletContext, addApplicationContextAttribute);
	}

	public ServletContextApplicationContextInitializer(ServletContext servletContext) {
		super(servletContext);
	}

}
