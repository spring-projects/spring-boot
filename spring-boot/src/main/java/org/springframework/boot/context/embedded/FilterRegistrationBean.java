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

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 * A {@link ServletContextInitializer} to register {@link Filter}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addFilter(String, Filter) registration}
 * features provided by {@link ServletContext} but with a Spring Bean friendly design.
 * <p>
 * The {@link #setFilter(Filter) Filter} must be specified before calling
 * {@link #onStartup(ServletContext)}. Registrations can be associated with
 * {@link #setUrlPatterns URL patterns} and/or servlets (either by {@link #setServletNames
 * name} or via a {@link #setServletRegistrationBeans ServletRegistrationBean}s. When no
 * URL pattern or servlets are specified the filter will be associated to '/*'. The filter
 * name will be deduced if not specified.
 *
 * @author Phillip Webb
 * @see ServletContextInitializer
 * @see ServletContext#addFilter(String, Filter)
 * @see DelegatingFilterProxyRegistrationBean
 * @deprecated as of 1.4 in favor of
 * org.springframework.boot.web.servlet.FilterRegistrationBean
 */
@Deprecated
public class FilterRegistrationBean
		extends org.springframework.boot.web.servlet.FilterRegistrationBean
		implements org.springframework.boot.context.embedded.ServletContextInitializer {

	public FilterRegistrationBean() {
		super();
	}

	public FilterRegistrationBean(Filter filter,
			ServletRegistrationBean... servletRegistrationBeans) {
		super(filter, servletRegistrationBeans);
	}

}
