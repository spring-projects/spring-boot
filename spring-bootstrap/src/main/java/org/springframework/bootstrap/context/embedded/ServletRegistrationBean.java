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

package org.springframework.bootstrap.context.embedded;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.util.Assert;

/**
 * A {@link ServletContextInitializer} to register {@link Servlet}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addServlet(String, Servlet)
 * registration} features provided by {@link ServletContext} but with a Spring Bean
 * friendly design.
 * 
 * <p>
 * The {@link #setServlet(Servlet) servlet} must be specified before calling
 * {@link #onStartup}. URL mapping can be configured used {@link #setUrlMappings} or
 * omitted when mapping to '/*'. The servlet name will be deduced if not specified.
 * 
 * @author Phillip Webb
 * @see ServletContextInitializer
 * @see ServletContext#addServlet(String, Servlet)
 */
public class ServletRegistrationBean extends RegistrationBean {

	private static final String[] DEFAULT_MAPPINGS = { "/*" };

	private Servlet servlet;

	private Set<String> urlMappings = new LinkedHashSet<String>();

	private int loadOnStartup = 1;

	private Set<Filter> filters = new LinkedHashSet<Filter>();

	/**
	 * Create a new {@link ServletRegistrationBean} instance.
	 */
	public ServletRegistrationBean() {
	}

	/**
	 * Create a new {@link ServletRegistrationBean} instance with the specified
	 * {@link Servlet} and URL mapping.
	 * @param servlet the servlet being mapped
	 * @param urlMappings the URLs being mapped
	 */
	public ServletRegistrationBean(Servlet servlet, String... urlMappings) {
		setServlet(servlet);
		addUrlMappings(urlMappings);
	}

	/**
	 * Sets the servlet to be registered.
	 */
	public void setServlet(Servlet servlet) {
		Assert.notNull(servlet, "Servlet must not be null");
		this.servlet = servlet;
	}

	/**
	 * Set the URL mappings for the servlet. If not specified the mapping will default to
	 * '/'. This will replace any previously specified mappings.
	 * @param urlMappings the mappings to set
	 * @see #addUrlMappings(String...)
	 */
	public void setUrlMappings(Collection<String> urlMappings) {
		Assert.notNull(urlMappings, "UrlMappings must not be null");
		this.urlMappings = new LinkedHashSet<String>(urlMappings);
	}

	/**
	 * Return a mutable collection of the URL mappings for the servlet.
	 * @return the urlMappings
	 */
	public Collection<String> getUrlMappings() {
		return this.urlMappings;
	}

	/**
	 * Add URL mappings for the servlet.
	 * @param urlMappings the mappings to add
	 * @see #setUrlMappings(Collection)
	 */
	public void addUrlMappings(String... urlMappings) {
		Assert.notNull(urlMappings, "UrlMappings must not be null");
		this.urlMappings.addAll(Arrays.asList(urlMappings));
	}

	/**
	 * Sets the <code>loadOnStartup</code> priority. See
	 * {@link ServletRegistration.Dynamic#setLoadOnStartup} for details.
	 */
	public void setLoadOnStartup(int loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	/**
	 * Sets any Filters that should be registered to this servlet. Any previously
	 * specified Filters will be replaced.
	 * @param filters the Filters to set
	 */
	public void setFilters(Collection<? extends Filter> filters) {
		Assert.notNull(filters, "Filters must not be null");
		this.filters = new LinkedHashSet<Filter>(filters);
	}

	/**
	 * Returns a mutable collection of the Filters being registered with this servlet.
	 */
	public Collection<Filter> getFilters() {
		return this.filters;
	}

	/**
	 * Add Filters that will be registered with this servlet.
	 * @param filters the Filters to add
	 */
	public void addFilters(Filter... filters) {
		Assert.notNull(filters, "Filters must not be null");
		this.filters.addAll(Arrays.asList(filters));
	}

	/**
	 * Returns the servlet name that will be registered.
	 */
	public String getServletName() {
		return getName();
	}

	@Override
	public Object getRegistrationTarget() {
		return this.servlet;
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		Assert.notNull(this.servlet, "Servlet must not be null");
		configure(servletContext.addServlet(getServletName(), this.servlet));
		for (Filter filter : this.filters) {
			FilterRegistrationBean filterRegistration = new FilterRegistrationBean(
					filter, this);
			filterRegistration.setAsyncSupported(isAsyncSupported());
			filterRegistration.onStartup(servletContext);
		}
	}

	/**
	 * Configure registration settings. Subclasses can override this method to perform
	 * additional configuration if required.
	 */
	protected void configure(ServletRegistration.Dynamic registration) {
		super.configure(registration);
		String[] urlMapping = this.urlMappings
				.toArray(new String[this.urlMappings.size()]);
		if (urlMapping.length == 0) {
			urlMapping = DEFAULT_MAPPINGS;
		}
		registration.addMapping(urlMapping);
		registration.setLoadOnStartup(this.loadOnStartup);
	}
}
