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

package org.springframework.boot.context.embedded;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 * A {@link ServletContextInitializer} to register {@link Servlet}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addServlet(String, Servlet)
 * registration} features provided by {@link ServletContext} but with a Spring Bean
 * friendly design.
 * <p>
 * The {@link #setServlet(Servlet) servlet} must be specified before calling
 * {@link #onStartup}. URL mapping can be configured used {@link #setUrlMappings} or
 * omitted when mapping to '/*' (unless
 * {@link #ServletRegistrationBean(Servlet, boolean, String...) alwaysMapUrl} is set to
 * {@code false}). The servlet name will be deduced if not specified.
 *
 * @author Phillip Webb
 * @see ServletContextInitializer
 * @see ServletContext#addServlet(String, Servlet)
 * @deprecated as of 1.4 in favor of org.springframework.boot.web.ServletRegistrationBean
 */
@Deprecated
public class ServletRegistrationBean
		extends org.springframework.boot.web.servlet.ServletRegistrationBean
		implements org.springframework.boot.context.embedded.ServletContextInitializer {

	public ServletRegistrationBean() {
		super();
	}

	public ServletRegistrationBean(Servlet servlet, boolean alwaysMapUrl,
			String... urlMappings) {
		super(servlet, alwaysMapUrl, urlMappings);
	}

	public ServletRegistrationBean(Servlet servlet, String... urlMappings) {
		super(servlet, urlMappings);
	}

}
