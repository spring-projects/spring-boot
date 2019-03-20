/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletContextInitializerBeans}.
 *
 * @author Andy Wilkinson
 */
public class ServletContextInitializerBeansTests {

	private ConfigurableApplicationContext context;

	@Test
	public void servletThatImplementsServletContextInitializerIsOnlyRegisteredOnce() {
		load(ServletConfiguration.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory());
		assertThat(initializerBeans.size()).isEqualTo(1);
		assertThat(initializerBeans.iterator()).hasOnlyElementsOfType(TestServlet.class);
	}

	@Test
	public void filterThatImplementsServletContextInitializerIsOnlyRegisteredOnce() {
		load(FilterConfiguration.class);
		ServletContextInitializerBeans initializerBeans = new ServletContextInitializerBeans(
				this.context.getBeanFactory());
		assertThat(initializerBeans.size()).isEqualTo(1);
		assertThat(initializerBeans.iterator()).hasOnlyElementsOfType(TestFilter.class);
	}

	private void load(Class<?> configuration) {
		this.context = new AnnotationConfigApplicationContext(configuration);
	}

	@Configuration
	static class ServletConfiguration {

		@Bean
		public TestServlet testServlet() {
			return new TestServlet();
		}

	}

	@Configuration
	static class FilterConfiguration {

		@Bean
		public TestFilter testFilter() {
			return new TestFilter();
		}

	}

	static class TestServlet extends HttpServlet implements ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {

		}

	}

	static class TestFilter implements Filter, ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {

		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {

		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {

		}

		@Override
		public void destroy() {

		}

	}

}
