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

package org.springframework.boot.test.autoconfigure.web.servlet;

import java.util.Collection;

import javax.servlet.Filter;

import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link MockMvcBuilderCustomizer} for a typical Spring Boot application. Usually applied
 * automatically via {@link AutoConfigureMockMvc @AutoConfigureMockMvc}, but may also be
 * used directly.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class SpringBootMockMvcBuilderCustomizer implements MockMvcBuilderCustomizer {

	private final WebApplicationContext context;

	private boolean addFilters = true;

	private boolean alwaysPrint = true;

	/**
	 * Create a new {@link SpringBootMockMvcBuilderCustomizer} instance.
	 * @param context the source application context
	 */
	public SpringBootMockMvcBuilderCustomizer(WebApplicationContext context) {
		Assert.notNull(context, "Context must not be null");
		this.context = context;
	}

	@Override
	public void customize(ConfigurableMockMvcBuilder<?> builder) {
		if (this.addFilters) {
			addFilters(builder);
		}
		if (this.alwaysPrint) {
			builder.alwaysDo(MockMvcResultHandlers.print(System.out));
		}
	}

	private void addFilters(ConfigurableMockMvcBuilder<?> builder) {
		ServletContextInitializerBeans Initializers = new ServletContextInitializerBeans(
				this.context);
		for (ServletContextInitializer initializer : Initializers) {
			if (initializer instanceof FilterRegistrationBean) {
				addFilter(builder, (FilterRegistrationBean) initializer);
			}
			if (initializer instanceof DelegatingFilterProxyRegistrationBean) {
				addFilter(builder, (DelegatingFilterProxyRegistrationBean) initializer);
			}
		}
	}

	private void addFilter(ConfigurableMockMvcBuilder<?> builder,
			FilterRegistrationBean registration) {
		addFilter(builder, registration.getFilter(), registration.getUrlPatterns());
	}

	private void addFilter(ConfigurableMockMvcBuilder<?> builder,
			DelegatingFilterProxyRegistrationBean registration) {
		addFilter(builder, registration.getFilter(), registration.getUrlPatterns());
	}

	private void addFilter(ConfigurableMockMvcBuilder<?> builder, Filter filter,
			Collection<String> urls) {
		if (urls.isEmpty()) {
			builder.addFilters(filter);
		}
		else {
			builder.addFilter(filter, urls.toArray(new String[urls.size()]));
		}
	}

	public void setAddFilters(boolean addFilters) {
		this.addFilters = addFilters;
	}

	public boolean isAddFilters() {
		return this.addFilters;
	}

	public void setAlwaysPrint(boolean alwaysPrint) {
		this.alwaysPrint = alwaysPrint;
	}

	public boolean isAlwaysPrint() {
		return this.alwaysPrint;
	}

}
