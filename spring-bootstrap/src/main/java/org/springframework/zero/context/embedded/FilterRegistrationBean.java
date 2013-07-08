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

package org.springframework.zero.context.embedded;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.util.Assert;

/**
 * A {@link ServletContextInitializer} to register {@link Filter}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addFilter(String, Filter) registration}
 * features provided by {@link ServletContext} but with a Spring Bean friendly design.
 * 
 * <p>
 * The {@link #setFilter(Filter) Filter} must be specified before calling
 * {@link #onStartup(ServletContext)}. Registrations can be associated with
 * {@link #setUrlPatterns URL patterns} and/or servlets (either by
 * {@link #setServletNames name} or via a {@link #setServletRegistrationBeans
 * ServletRegistrationBean}s. When no URL pattern or servlets are specified the filter
 * will be associated to '/*'. The filter name will be deduced if not specified.
 * 
 * @author Phillip Webb
 * @see ServletContextInitializer
 * @see ServletContext#addFilter(String, Filter)
 */
public class FilterRegistrationBean extends RegistrationBean {

	static final EnumSet<DispatcherType> ASYNC_DISPATCHER_TYPES = EnumSet.of(
			DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST,
			DispatcherType.ASYNC);

	static final EnumSet<DispatcherType> NON_ASYNC_DISPATCHER_TYPES = EnumSet.of(
			DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST);

	private static final String[] DEFAULT_URL_MAPPINGS = { "/*" };

	private Filter filter;

	private Set<ServletRegistrationBean> servletRegistrationBeans = new LinkedHashSet<ServletRegistrationBean>();

	private Set<String> servletNames = new LinkedHashSet<String>();

	private Set<String> urlPatterns = new LinkedHashSet<String>();

	private EnumSet<DispatcherType> dispatcherTypes;

	private boolean matchAfter = false;

	/**
	 * Create a new {@link FilterRegistrationBean} instance.
	 */
	public FilterRegistrationBean() {
	}

	/**
	 * Create a new {@link FilterRegistrationBean} instance to be registered with the
	 * specified {@link ServletRegistrationBean}s.
	 * @param filter the filter to register
	 * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
	 */
	public FilterRegistrationBean(Filter filter,
			ServletRegistrationBean... servletRegistrationBeans) {
		Assert.notNull(filter, "Filter must not be null");
		Assert.notNull(servletRegistrationBeans,
				"ServletRegistrationBeans must not be null");
		this.filter = filter;
		for (ServletRegistrationBean servletRegistrationBean : servletRegistrationBeans) {
			this.servletRegistrationBeans.add(servletRegistrationBean);
		}
	}

	/**
	 * Set the filter to be registered.
	 */
	public void setFilter(Filter filter) {
		Assert.notNull(filter, "Filter must not be null");
		this.filter = filter;
	}

	/**
	 * Set {@link ServletRegistrationBean}s that the filter will be registered against.
	 * @param servletRegistrationBeans the Servlet registration beans
	 */
	public void setServletRegistrationBeans(
			Collection<? extends ServletRegistrationBean> servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans,
				"ServletRegistrationBeans must not be null");
		this.servletRegistrationBeans = new LinkedHashSet<ServletRegistrationBean>(
				servletRegistrationBeans);
	}

	/**
	 * Return a mutable collection of the {@link ServletRegistrationBean} that the filter
	 * will be registered against. {@link ServletRegistrationBean}s.
	 * @return the Servlet registration beans
	 * @see #setServletNames
	 * @see #setUrlPatterns
	 */
	public Collection<ServletRegistrationBean> getServletRegistrationBeans() {
		return this.servletRegistrationBeans;
	}

	/**
	 * Add {@link ServletRegistrationBean}s for the filter.
	 * @param servletRegistrationBeans the servlet registration beans to add
	 * @see #setServletRegistrationBeans
	 */
	public void addServletRegistrationBeans(
			ServletRegistrationBean... servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans,
				"ServletRegistrationBeans must not be null");
		for (ServletRegistrationBean servletRegistrationBean : servletRegistrationBeans) {
			this.servletRegistrationBeans.add(servletRegistrationBean);
		}
	}

	/**
	 * Set servlet names that the filter will be registered against. This will replace any
	 * previously specified servlet names.
	 * @param servletNames the servlet names
	 * @see #setServletRegistrationBeans
	 * @see #setUrlPatterns
	 */
	public void setServletNames(Collection<String> servletNames) {
		Assert.notNull(servletNames, "ServletNames must not be null");
		this.servletNames = new LinkedHashSet<String>(servletNames);
	}

	/**
	 * Return a mutable collection of servlet names that the filter will be registered
	 * against.
	 * @return the servlet names
	 */
	public Collection<String> getServletNames() {
		return this.servletNames;
	}

	/**
	 * Add servlet names for the filter.
	 * @param servletNames the servlet names to add
	 */
	public void addServletNames(String... servletNames) {
		Assert.notNull(servletNames, "ServletNames must not be null");
		this.servletNames.addAll(Arrays.asList(servletNames));
	}

	/**
	 * Set the URL patterns that the filter will be registered against. This will replace
	 * any previously specified URL patterns.
	 * @param urlPatterns the URL patterns
	 * @see #setServletRegistrationBeans
	 * @see #setServletNames
	 */
	public void setUrlPatterns(Collection<String> urlPatterns) {
		Assert.notNull(urlPatterns, "UrlPatterns must not be null");
		this.urlPatterns = new LinkedHashSet<String>(urlPatterns);
	}

	/**
	 * Return a mutable collection of URL patterns that the filter will be registered
	 * against.
	 * @return the URL patterns
	 */
	public Collection<String> getUrlPatterns() {
		return this.urlPatterns;
	}

	/**
	 * Add URL patterns that the filter will be registered against.
	 * @param urlPatterns the URL patterns
	 */
	public void addUrlPatterns(String... urlPatterns) {
		Assert.notNull(urlPatterns, "UrlPatterns must not be null");
		for (String urlPattern : urlPatterns) {
			this.urlPatterns.add(urlPattern);
		}
	}

	/**
	 * Set if the filter mappings should be matched after any declared filter mappings of
	 * the ServletContext. Defaults to {@code false} indicating the filters are supposed
	 * to be matched before any declared filter mappings of the ServletContext.
	 */
	public void setMatchAfter(boolean matchAfter) {
		this.matchAfter = matchAfter;
	}

	/**
	 * Return if filter mappings should be matched after any declared Filter mappings of
	 * the ServletContext.
	 */
	public boolean isMatchAfter() {
		return this.matchAfter;
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		Assert.notNull(this.filter, "Filter must not be null");
		configure(servletContext.addFilter(getName(), this.filter));
	}

	@Override
	public Object getRegistrationTarget() {
		return this.filter;
	}

	/**
	 * Configure registration settings. Subclasses can override this method to perform
	 * additional configuration if required.
	 */
	protected void configure(FilterRegistration.Dynamic registration) {
		super.configure(registration);
		EnumSet<DispatcherType> dispatcherTypes = this.dispatcherTypes;
		if (dispatcherTypes == null) {
			dispatcherTypes = (isAsyncSupported() ? ASYNC_DISPATCHER_TYPES
					: NON_ASYNC_DISPATCHER_TYPES);
		}

		Set<String> servletNames = new LinkedHashSet<String>();
		for (ServletRegistrationBean servletRegistrationBean : this.servletRegistrationBeans) {
			servletNames.add(servletRegistrationBean.getServletName());
		}
		servletNames.addAll(this.servletNames);

		if (servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
			registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter,
					DEFAULT_URL_MAPPINGS);
		}
		else {
			if (servletNames.size() > 0) {
				registration.addMappingForServletNames(dispatcherTypes, this.matchAfter,
						servletNames.toArray(new String[servletNames.size()]));
			}
			if (this.urlPatterns.size() > 0) {
				registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter,
						this.urlPatterns.toArray(new String[this.urlPatterns.size()]));
			}
		}
	}
}
