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

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.zero.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.zero.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.zero.context.embedded.config.ExampleEmbeddedWebApplicationConfiguration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AnnotationConfigEmbeddedWebApplicationContext}.
 * 
 * @author Phillip Webb
 */
public class AnnotationConfigEmbeddedWebApplicationContextTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Test
	public void createFromScan() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ExampleEmbeddedWebApplicationConfiguration.class.getPackage().getName());
		verifyContext();
	}

	@Test
	public void createFromConfigClass() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ExampleEmbeddedWebApplicationConfiguration.class);
		verifyContext();
	}

	@Test
	public void registerAndRefresh() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(ExampleEmbeddedWebApplicationConfiguration.class);
		this.context.refresh();
		verifyContext();
	}

	@Test
	public void scanAndRefresh() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.scan(ExampleEmbeddedWebApplicationConfiguration.class.getPackage()
				.getName());
		this.context.refresh();
		verifyContext();
	}

	@Test
	public void createAndInitializeCyclic() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ServletContextAwareEmbeddedConfiguration.class);
		verifyContext();
		// You can't initialize the application context and inject the servlet context
		// because of a cycle - we'd like this to be not null but it never will be
		assertNull(this.context.getBean(ServletContextAwareEmbeddedConfiguration.class)
				.getServletContext());
	}

	@Test
	public void createAndInitializeWithParent() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext parent = new AnnotationConfigEmbeddedWebApplicationContext(
				EmbeddedContainerConfiguration.class);
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(EmbeddedContainerConfiguration.class,
				ServletContextAwareConfiguration.class);
		this.context.setParent(parent);
		this.context.refresh();
		verifyContext();
		assertNotNull(this.context.getBean(ServletContextAwareConfiguration.class)
				.getServletContext());
	}

	private void verifyContext() {
		MockEmbeddedServletContainerFactory containerFactory = this.context
				.getBean(MockEmbeddedServletContainerFactory.class);
		Servlet servlet = this.context.getBean(Servlet.class);
		verify(containerFactory.getServletContext()).addServlet("servlet", servlet);
	}

	@Configuration
	@EnableWebMvc
	public static class ServletContextAwareEmbeddedConfiguration implements
			ServletContextAware {

		private ServletContext servletContext;

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return new MockEmbeddedServletContainerFactory();
		}

		@Bean
		public Servlet servlet() {
			return new MockServlet();
		}

		@Override
		public void setServletContext(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		public ServletContext getServletContext() {
			return this.servletContext;
		}

	}

	@Configuration
	public static class EmbeddedContainerConfiguration {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return new MockEmbeddedServletContainerFactory();
		}

	}

	@Configuration
	@EnableWebMvc
	public static class ServletContextAwareConfiguration implements ServletContextAware {

		private ServletContext servletContext;

		@Bean
		public Servlet servlet() {
			return new MockServlet();
		}

		@Override
		public void setServletContext(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		public ServletContext getServletContext() {
			return this.servletContext;
		}

	}
}
