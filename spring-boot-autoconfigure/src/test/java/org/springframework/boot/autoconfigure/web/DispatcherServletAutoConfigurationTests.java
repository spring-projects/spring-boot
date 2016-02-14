/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DispatcherServletAutoConfiguration}.
 *
 * @author Dave Syer
 */
public class DispatcherServletAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void registrationProperties() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		assertNotNull(this.context.getBean(DispatcherServlet.class));
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertEquals("[/]", registration.getUrlMappings().toString());
	}

	@Test
	public void registrationOverride() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(CustomDispatcherRegistration.class,
				ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertEquals("[/foo]", registration.getUrlMappings().toString());
		assertEquals("customDispatcher", registration.getServletName());
		assertEquals(0, this.context.getBeanNamesForType(DispatcherServlet.class).length);
	}

	// If you override either the dispatcherServlet or its registration you have to
	// provide both...
	@Test(expected = UnsatisfiedDependencyException.class)
	public void registrationOverrideWithAutowiredServlet() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(CustomAutowiredRegistration.class,
				ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertEquals("[/foo]", registration.getUrlMappings().toString());
		assertEquals("customDispatcher", registration.getServletName());
		assertEquals(1, this.context.getBeanNamesForType(DispatcherServlet.class).length);
	}

	@Test
	public void servletPath() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "server.servlet_path:/spring");
		this.context.refresh();
		assertNotNull(this.context.getBean(DispatcherServlet.class));
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertEquals("[/spring/*]", registration.getUrlMappings().toString());
		assertNull(registration.getMultipartConfig());
	}

	@Test
	public void multipartConfig() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(MultipartConfiguration.class,
				ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		this.context.refresh();
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertNotNull(registration.getMultipartConfig());
	}

	@Test
	public void renamesMultipartResolver() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(MultipartResolverConfiguration.class,
				ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		this.context.refresh();
		DispatcherServlet dispatcherServlet = this.context
				.getBean(DispatcherServlet.class);
		dispatcherServlet.onApplicationEvent(new ContextRefreshedEvent(this.context));
		assertThat(dispatcherServlet.getMultipartResolver(),
				instanceOf(MockMultipartResolver.class));
	}

	@Test
	public void dispatcherServletConfig() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mvc.throw-exception-if-no-handler-found:true");
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mvc.dispatch-options-request:true");
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mvc.dispatch-trace-request:true");
		this.context.refresh();
		DispatcherServlet bean = this.context.getBean(DispatcherServlet.class);
		assertEquals(true, new DirectFieldAccessor(bean)
				.getPropertyValue("throwExceptionIfNoHandlerFound"));
		assertEquals(true,
				new DirectFieldAccessor(bean).getPropertyValue("dispatchOptionsRequest"));
		assertEquals(true,
				new DirectFieldAccessor(bean).getPropertyValue("dispatchTraceRequest"));
	}

	@Configuration
	protected static class MultipartConfiguration {

		@Bean
		public MultipartConfigElement multipartConfig() {
			MultipartConfigFactory factory = new MultipartConfigFactory();
			factory.setMaxFileSize("128KB");
			factory.setMaxRequestSize("128KB");
			return factory.createMultipartConfig();
		}

	}

	@Configuration
	protected static class CustomDispatcherRegistration {

		@Bean
		public ServletRegistrationBean dispatcherServletRegistration() {
			ServletRegistrationBean registration = new ServletRegistrationBean(
					new DispatcherServlet(), "/foo");
			registration.setName("customDispatcher");
			return registration;
		}

	}

	@Configuration
	protected static class CustomAutowiredRegistration {

		@Bean
		public ServletRegistrationBean dispatcherServletRegistration(
				DispatcherServlet dispatcherServlet) {
			ServletRegistrationBean registration = new ServletRegistrationBean(
					dispatcherServlet, "/foo");
			registration.setName("customDispatcher");
			return registration;
		}

	}

	@Configuration
	protected static class MultipartResolverConfiguration {

		@Bean
		public MultipartResolver getMultipartResolver() {
			return new MockMultipartResolver();
		}

	}

	private static class MockMultipartResolver implements MultipartResolver {

		@Override
		public boolean isMultipart(HttpServletRequest request) {
			return false;
		}

		@Override
		public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request)
				throws MultipartException {
			return null;
		}

		@Override
		public void cleanupMultipart(MultipartHttpServletRequest request) {
		}

	}

}
