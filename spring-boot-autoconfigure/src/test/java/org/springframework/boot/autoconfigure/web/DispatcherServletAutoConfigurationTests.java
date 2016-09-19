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

package org.springframework.boot.autoconfigure.web;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DispatcherServletAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Brian Clozel
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
		assertThat(this.context.getBean(DispatcherServlet.class)).isNotNull();
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertThat(registration.getUrlMappings().toString()).isEqualTo("[/]");
	}

	@Test
	public void registrationNonServletBean() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(NonServletConfiguration.class,
				ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(ServletRegistrationBean.class).length)
				.isEqualTo(0);
		assertThat(this.context.getBeanNamesForType(DispatcherServlet.class).length)
				.isEqualTo(0);
	}

	// If a DispatcherServlet instance is registered with a name different
	// from the default one, we're registering one anyway
	@Test
	public void registrationOverrideWithDispatcherServletWrongName() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(CustomDispatcherServletWrongName.class,
				ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertThat(registration.getUrlMappings().toString()).isEqualTo("[/]");
		assertThat(registration.getServletName()).isEqualTo("dispatcherServlet");
		assertThat(this.context.getBeanNamesForType(DispatcherServlet.class).length)
				.isEqualTo(2);
	}

	@Test
	public void registrationOverrideWithAutowiredServlet() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(CustomAutowiredRegistration.class,
				ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertThat(registration.getUrlMappings().toString()).isEqualTo("[/foo]");
		assertThat(registration.getServletName()).isEqualTo("customDispatcher");
		assertThat(this.context.getBeanNamesForType(DispatcherServlet.class).length)
				.isEqualTo(1);
	}

	@Test
	public void servletPath() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "server.servlet_path:/spring");
		this.context.refresh();
		assertThat(this.context.getBean(DispatcherServlet.class)).isNotNull();
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertThat(registration.getUrlMappings().toString()).isEqualTo("[/spring/*]");
		assertThat(registration.getMultipartConfig()).isNull();
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
		assertThat(registration.getMultipartConfig()).isNotNull();
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
		assertThat(dispatcherServlet.getMultipartResolver())
				.isInstanceOf(MockMultipartResolver.class);
	}

	@Test
	public void dispatcherServletDefaultConfig() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		this.context.refresh();
		DispatcherServlet bean = this.context.getBean(DispatcherServlet.class);
		assertThat(bean).extracting("throwExceptionIfNoHandlerFound")
				.containsExactly(false);
		assertThat(bean).extracting("dispatchOptionsRequest").containsExactly(true);
		assertThat(bean).extracting("dispatchTraceRequest").containsExactly(false);
		assertThat(new DirectFieldAccessor(
				this.context.getBean("dispatcherServletRegistration"))
						.getPropertyValue("loadOnStartup")).isEqualTo(-1);
	}

	@Test
	public void dispatcherServletCustomConfig() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ServerPropertiesAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mvc.throw-exception-if-no-handler-found:true",
				"spring.mvc.dispatch-options-request:false",
				"spring.mvc.dispatch-trace-request:true",
				"spring.mvc.servlet.load-on-startup=5");
		this.context.refresh();
		DispatcherServlet bean = this.context.getBean(DispatcherServlet.class);
		assertThat(bean).extracting("throwExceptionIfNoHandlerFound")
				.containsExactly(true);
		assertThat(bean).extracting("dispatchOptionsRequest").containsExactly(false);
		assertThat(bean).extracting("dispatchTraceRequest").containsExactly(true);
		assertThat(new DirectFieldAccessor(
				this.context.getBean("dispatcherServletRegistration"))
						.getPropertyValue("loadOnStartup")).isEqualTo(5);
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
	protected static class CustomDispatcherServletWrongName {

		@Bean
		public DispatcherServlet customDispatcherServlet() {
			return new DispatcherServlet();
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
	protected static class NonServletConfiguration {

		@Bean
		public String dispatcherServlet() {
			return "spring";
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
