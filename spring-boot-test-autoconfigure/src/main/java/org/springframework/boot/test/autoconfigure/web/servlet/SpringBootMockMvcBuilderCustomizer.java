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

import java.io.PrintStream;
import java.util.Collection;

import javax.servlet.Filter;

import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.PrintingResultHandler;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
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

	private MockMvcPrint print = MockMvcPrint.DEFAULT;

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
		ResultHandler printHandler = getPrintHandler();
		if (printHandler != null) {
			builder.alwaysDo(printHandler);
		}
	}

	private ResultHandler getPrintHandler() {
		if (this.print == MockMvcPrint.NONE) {
			return null;
		}
		if (this.print == MockMvcPrint.LOG_DEBUG) {
			return MockMvcResultHandlers.log();
		}
		return new SystemResultHandler(this.print);
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

	public void setPrint(MockMvcPrint print) {
		this.print = print;
	}

	public MockMvcPrint getPrint() {
		return this.print;
	}

	/**
	 * {@link PrintingResultHandler} to deal with {@code System.out} and
	 * {@code System.err} printing. The actual {@link PrintStream} used to write the
	 * response is obtained as late as possible in case an {@code OutputCaptureRule} is
	 * being used.
	 */
	private static class SystemResultHandler extends PrintingResultHandler {

		protected SystemResultHandler(MockMvcPrint print) {
			super(new SystemResultValuePrinter(print));
		}

		private static class SystemResultValuePrinter implements ResultValuePrinter {

			private final MockMvcPrint print;

			SystemResultValuePrinter(MockMvcPrint print) {
				this.print = print;
			}

			@Override
			public void printHeading(String heading) {
				getWriter().println();
				getWriter().println(String.format("%s:", heading));
			}

			@Override
			public void printValue(String label, Object value) {
				if (value != null && value.getClass().isArray()) {
					value = CollectionUtils.arrayToList(value);
				}
				getWriter().println(String.format("%17s = %s", label, value));
			}

			private PrintStream getWriter() {
				if (this.print == MockMvcPrint.SYSTEM_ERR) {
					return System.err;
				}
				return System.out;
			}

		}

	}

}
