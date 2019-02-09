/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract base {@link ServletContextInitializer} to register {@link Filter}s in a
 * Servlet 3.0+ container.
 *
 * @param <T> the type of {@link Filter} to register
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 2.0.1
 */
public abstract class AbstractFilterRegistrationBean<T extends Filter>
		extends DynamicRegistrationBean<Dynamic> {

	/**
	 * Filters that wrap the servlet request should be ordered less than or equal to this.
	 * @deprecated since 2.1.0 in favor of
	 * {@code OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER}
	 */
	@Deprecated
	protected static final int REQUEST_WRAPPER_FILTER_MAX_ORDER = 0;

	private static final String[] DEFAULT_URL_MAPPINGS = { "/*" };

	private Set<ServletRegistrationBean<?>> servletRegistrationBeans = new LinkedHashSet<>();

	private Set<String> servletNames = new LinkedHashSet<>();

	private Set<String> urlPatterns = new LinkedHashSet<>();

	private EnumSet<DispatcherType> dispatcherTypes;

	private boolean matchAfter = false;

	/**
	 * Create a new instance to be registered with the specified
	 * {@link ServletRegistrationBean}s.
	 * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
	 */
	AbstractFilterRegistrationBean(
			ServletRegistrationBean<?>... servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans,
				"ServletRegistrationBeans must not be null");
		Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
	}

	/**
	 * Set {@link ServletRegistrationBean}s that the filter will be registered against.
	 * @param servletRegistrationBeans the Servlet registration beans
	 */
	public void setServletRegistrationBeans(
			Collection<? extends ServletRegistrationBean<?>> servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans,
				"ServletRegistrationBeans must not be null");
		this.servletRegistrationBeans = new LinkedHashSet<>(servletRegistrationBeans);
	}

	/**
	 * Return a mutable collection of the {@link ServletRegistrationBean} that the filter
	 * will be registered against. {@link ServletRegistrationBean}s.
	 * @return the Servlet registration beans
	 * @see #setServletNames
	 * @see #setUrlPatterns
	 */
	public Collection<ServletRegistrationBean<?>> getServletRegistrationBeans() {
		return this.servletRegistrationBeans;
	}

	/**
	 * Add {@link ServletRegistrationBean}s for the filter.
	 * @param servletRegistrationBeans the servlet registration beans to add
	 * @see #setServletRegistrationBeans
	 */
	public void addServletRegistrationBeans(
			ServletRegistrationBean<?>... servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans,
				"ServletRegistrationBeans must not be null");
		Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
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
		this.servletNames = new LinkedHashSet<>(servletNames);
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
		this.urlPatterns = new LinkedHashSet<>(urlPatterns);
	}

	/**
	 * Return a mutable collection of URL patterns, as defined in the Servlet
	 * specification, that the filter will be registered against.
	 * @return the URL patterns
	 */
	public Collection<String> getUrlPatterns() {
		return this.urlPatterns;
	}

	/**
	 * Add URL patterns, as defined in the Servlet specification, that the filter will be
	 * registered against.
	 * @param urlPatterns the URL patterns
	 */
	public void addUrlPatterns(String... urlPatterns) {
		Assert.notNull(urlPatterns, "UrlPatterns must not be null");
		Collections.addAll(this.urlPatterns, urlPatterns);
	}

	/**
	 * Convenience method to {@link #setDispatcherTypes(EnumSet) set dispatcher types}
	 * using the specified elements.
	 * @param first the first dispatcher type
	 * @param rest additional dispatcher types
	 */
	public void setDispatcherTypes(DispatcherType first, DispatcherType... rest) {
		this.dispatcherTypes = EnumSet.of(first, rest);
	}

	/**
	 * Sets the dispatcher types that should be used with the registration. If not
	 * specified the types will be deduced based on the value of
	 * {@link #isAsyncSupported()}.
	 * @param dispatcherTypes the dispatcher types
	 */
	public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
		this.dispatcherTypes = dispatcherTypes;
	}

	/**
	 * Set if the filter mappings should be matched after any declared filter mappings of
	 * the ServletContext. Defaults to {@code false} indicating the filters are supposed
	 * to be matched before any declared filter mappings of the ServletContext.
	 * @param matchAfter if filter mappings are matched after
	 */
	public void setMatchAfter(boolean matchAfter) {
		this.matchAfter = matchAfter;
	}

	/**
	 * Return if filter mappings should be matched after any declared Filter mappings of
	 * the ServletContext.
	 * @return if filter mappings are matched after
	 */
	public boolean isMatchAfter() {
		return this.matchAfter;
	}

	@Override
	protected String getDescription() {
		Filter filter = getFilter();
		Assert.notNull(filter, "Filter must not be null");
		return "filter " + getOrDeduceName(filter);
	}

	@Override
	protected Dynamic addRegistration(String description, ServletContext servletContext) {
		Filter filter = getFilter();
		return servletContext.addFilter(getOrDeduceName(filter), filter);
	}

	/**
	 * Configure registration settings. Subclasses can override this method to perform
	 * additional configuration if required.
	 * @param registration the registration
	 */
	@Override
	protected void configure(FilterRegistration.Dynamic registration) {
		super.configure(registration);
		EnumSet<DispatcherType> dispatcherTypes = this.dispatcherTypes;
		if (dispatcherTypes == null) {
			dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
		}
		Set<String> servletNames = new LinkedHashSet<>();
		for (ServletRegistrationBean<?> servletRegistrationBean : this.servletRegistrationBeans) {
			servletNames.add(servletRegistrationBean.getServletName());
		}
		servletNames.addAll(this.servletNames);
		if (servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
			registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter,
					DEFAULT_URL_MAPPINGS);
		}
		else {
			if (!servletNames.isEmpty()) {
				registration.addMappingForServletNames(dispatcherTypes, this.matchAfter,
						StringUtils.toStringArray(servletNames));
			}
			if (!this.urlPatterns.isEmpty()) {
				registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter,
						StringUtils.toStringArray(this.urlPatterns));
			}
		}
	}

	/**
	 * Return the {@link Filter} to be registered.
	 * @return the filter
	 */
	public abstract T getFilter();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getOrDeduceName(this));
		if (this.servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
			builder.append(" urls=").append(Arrays.toString(DEFAULT_URL_MAPPINGS));
		}
		else {
			if (!this.servletNames.isEmpty()) {
				builder.append(" servlets=").append(this.servletNames);
			}
			if (!this.urlPatterns.isEmpty()) {
				builder.append(" urls=").append(this.urlPatterns);
			}
		}
		return builder.toString();
	}

}
