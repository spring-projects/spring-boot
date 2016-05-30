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

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * A {@link ServletContextInitializer} to register {@link DelegatingFilterProxy}s in a
 * Servlet 3.0+ container. Similar to the {@link ServletContext#addFilter(String, Filter)
 * registration} features provided by {@link ServletContext} but with a Spring Bean
 * friendly design.
 * <p>
 * The bean name of the actual delegate {@link Filter} should be specified using the
 * {@code targetBeanName} constructor argument. Unlike the {@link FilterRegistrationBean},
 * referenced filters are not instantiated early. In fact, if the delegate filter bean is
 * marked {@code @Lazy} it won't be instantiated at all until the filter is called.
 * <p>
 * Registrations can be associated with {@link #setUrlPatterns URL patterns} and/or
 * servlets (either by {@link #setServletNames name} or via a
 * {@link #setServletRegistrationBeans ServletRegistrationBean}s. When no URL pattern or
 * servlets are specified the filter will be associated to '/*'. The targetBeanName will
 * be used as the filter name if not otherwise specified.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ServletContextInitializer
 * @see ServletContext#addFilter(String, Filter)
 * @see FilterRegistrationBean
 * @see DelegatingFilterProxy
 * @deprecated as of 1.4 in favor of
 * org.springframework.boot.web.DelegatingFilterProxyRegistrationBean
 */
@Deprecated
public class DelegatingFilterProxyRegistrationBean
		extends org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean
		implements org.springframework.boot.context.embedded.ServletContextInitializer {

	public DelegatingFilterProxyRegistrationBean(String targetBeanName,
			ServletRegistrationBean... servletRegistrationBeans) {
		super(targetBeanName, servletRegistrationBeans);
	}

}
