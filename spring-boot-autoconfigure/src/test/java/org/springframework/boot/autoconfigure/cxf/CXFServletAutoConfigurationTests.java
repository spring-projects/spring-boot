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

import org.junit.Test;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.boot.autoconfigure.cxf.CXFServletAutoConfiguration;
import org.apache.cxf.transport.servlet.CXFServlet;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link CXFServletAutoConfiguration}.
 *
 * @author Elan Thangamani
 */
public class CXFServletAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;
	public static final String DEFAULT_CXF_SERVLET_BEAN_NAME = "cxfServlet";

	@Test
	public void registrationProperties() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(CXFServletAutoConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		ServletRegistrationBean registration = this.context
				.getBean(ServletRegistrationBean.class);
		assertEquals("[/service/*]", registration.getUrlMappings().toString());
	}

	@Test
	public void registrationOverride() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(CustomCXFRegistration.class, CXFServletAutoConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		ServletRegistrationBean registration = this.context.getBean(ServletRegistrationBean.class);
		assertEquals("[/api/*]", registration.getUrlMappings().toString());
		assertEquals("customCXF", registration.getServletName());
		assertEquals(0, this.context.getBeanNamesForType(CXFServlet.class).length);
	}

	
	@Configuration
	protected static class CustomCXFRegistration {

		@Bean(name = DEFAULT_CXF_SERVLET_BEAN_NAME)
		public ServletRegistrationBean cxfServletRegistration() {
			ServletRegistrationBean registration = new ServletRegistrationBean(
					new CXFServlet(), "/api/*");
			registration.setName("customCXF");
			return registration;
		}
	}

	
	
}
