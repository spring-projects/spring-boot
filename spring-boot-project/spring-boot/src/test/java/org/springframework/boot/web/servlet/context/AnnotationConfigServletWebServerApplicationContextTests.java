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

package org.springframework.boot.web.servlet.context;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.config.ExampleServletWebServerApplicationConfiguration;
import org.springframework.boot.web.servlet.mock.MockServlet;
import org.springframework.boot.web.servlet.server.MockServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AnnotationConfigServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class AnnotationConfigServletWebServerApplicationContextTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	@AfterEach
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void createFromScan() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				ExampleServletWebServerApplicationConfiguration.class.getPackage().getName());
		verifyContext();
	}

	@Test
	void sessionScopeAvailable() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				ExampleServletWebServerApplicationConfiguration.class, SessionScopedComponent.class);
		verifyContext();
	}

	@Test
	void sessionScopeAvailableToServlet() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				ExampleServletWebServerApplicationConfiguration.class, ExampleServletWithAutowired.class,
				SessionScopedComponent.class);
		Servlet servlet = this.context.getBean(ExampleServletWithAutowired.class);
		assertThat(servlet).isNotNull();
	}

	@Test
	void createFromConfigClass() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				ExampleServletWebServerApplicationConfiguration.class);
		verifyContext();
	}

	@Test
	void registerAndRefresh() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(ExampleServletWebServerApplicationConfiguration.class);
		this.context.refresh();
		verifyContext();
	}

	@Test
	void multipleRegistersAndRefresh() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(WebServerConfiguration.class);
		this.context.register(ServletContextAwareConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(Servlet.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(ServletWebServerFactory.class)).hasSize(1);
	}

	@Test
	void scanAndRefresh() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.scan(ExampleServletWebServerApplicationConfiguration.class.getPackage().getName());
		this.context.refresh();
		verifyContext();
	}

	@Test
	void createAndInitializeCyclic() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				ServletContextAwareEmbeddedConfiguration.class);
		verifyContext();
		// You can't initialize the application context and inject the servlet context
		// because of a cycle - we'd like this to be not null but it never will be
		assertThat(this.context.getBean(ServletContextAwareEmbeddedConfiguration.class).getServletContext()).isNull();
	}

	@Test
	void createAndInitializeWithParent() {
		AnnotationConfigServletWebServerApplicationContext parent = new AnnotationConfigServletWebServerApplicationContext(
				WebServerConfiguration.class);
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(WebServerConfiguration.class, ServletContextAwareConfiguration.class);
		this.context.setParent(parent);
		this.context.refresh();
		verifyContext();
		assertThat(this.context.getBean(ServletContextAwareConfiguration.class).getServletContext()).isNotNull();
	}

	private void verifyContext() {
		MockServletWebServerFactory factory = this.context.getBean(MockServletWebServerFactory.class);
		Servlet servlet = this.context.getBean(Servlet.class);
		verify(factory.getServletContext()).addServlet("servlet", servlet);
	}

	@Component
	@SuppressWarnings("serial")
	protected static class ExampleServletWithAutowired extends GenericServlet {

		@Autowired
		private SessionScopedComponent component;

		@Override
		public void service(ServletRequest req, ServletResponse res) {
			assertThat(this.component).isNotNull();
		}

	}

	@Component
	@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
	protected static class SessionScopedComponent {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	public static class ServletContextAwareEmbeddedConfiguration implements ServletContextAware {

		private ServletContext servletContext;

		@Bean
		public ServletWebServerFactory webServerFactory() {
			return new MockServletWebServerFactory();
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

	@Configuration(proxyBeanMethods = false)
	public static class WebServerConfiguration {

		@Bean
		public ServletWebServerFactory webServerFactory() {
			return new MockServletWebServerFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
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
