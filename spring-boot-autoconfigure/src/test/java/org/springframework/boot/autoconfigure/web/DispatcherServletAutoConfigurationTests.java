/*
 * Copyright 2012-2014 the original author or authors.
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

import org.junit.Test;
import org.springframework.boot.context.embedded.MultiPartConfigFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link DispatcherServletAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class DispatcherServletAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

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
		assertEquals("[/spring]", registration.getUrlMappings().toString());
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

	@Configuration
	protected static class MultipartConfiguration {

		@Bean
		public MultipartConfigElement multipartConfig() {
			MultiPartConfigFactory factory = new MultiPartConfigFactory();
			factory.setMaxFileSize("128KB");
			factory.setMaxRequestSize("128KB");
			return factory.createMultipartConfig();
		}

	}

}
