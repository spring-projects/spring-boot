/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.autoconfigure;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.bootstrap.actuate.TestUtils;
import org.springframework.bootstrap.actuate.endpoint.health.HealthEndpoint;
import org.springframework.bootstrap.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.bootstrap.autoconfigure.web.ServerPropertiesConfiguration;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainer;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerException;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.ServletContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class ManagementConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testManagementConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MetricRepositoryConfiguration.class,
				TraceFilterConfiguration.class, ServerPropertiesConfiguration.class,
				ActuatorAutoConfiguration.ServerPropertiesConfiguration.class,
				ManagementAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(HealthEndpoint.class));
	}

	@Test
	public void testSuppressManagementConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(this.context, "management.port:0");
		this.context.register(MetricRepositoryConfiguration.class,
				TraceFilterConfiguration.class, ServerPropertiesConfiguration.class,
				ActuatorAutoConfiguration.ServerPropertiesConfiguration.class,
				ManagementAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeanNamesForType(HealthEndpoint.class).length);
	}

	@Test
	public void testManagementConfigurationExtensions() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MetricRepositoryConfiguration.class,
				TraceFilterConfiguration.class, ServerPropertiesConfiguration.class,
				ActuatorAutoConfiguration.ServerPropertiesConfiguration.class,
				ManagementAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, NewEndpoint.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(NewEndpoint.class));
	}

	@Test
	public void testManagementConfigurationExtensionsOrderDependence() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(NewEndpoint.class, MetricRepositoryConfiguration.class,
				TraceFilterConfiguration.class, ServerPropertiesConfiguration.class,
				ActuatorAutoConfiguration.ServerPropertiesConfiguration.class,
				ManagementAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(NewEndpoint.class));
	}

	@Test
	public void testChildContextCreated() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(this.context, "server.port:7000", "management.port:7001");
		this.context.register(ParentContext.class, MetricRepositoryConfiguration.class,
				TraceFilterConfiguration.class, ServerPropertiesConfiguration.class,
				ActuatorAutoConfiguration.ServerPropertiesConfiguration.class,
				ManagementAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, NewEndpoint.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeanNamesForType(HealthEndpoint.class).length);
		assertEquals(0, this.context.getBeanNamesForType(NewEndpoint.class).length);
	}

	@Configuration
	protected static class ParentContext {

		@Bean
		public EmbeddedServletContainerFactory factory() {
			return new EmbeddedServletContainerFactory() {

				@Override
				public EmbeddedServletContainer getEmbdeddedServletContainer(
						ServletContextInitializer... initializers) {
					ServletContext servletContext = new MockServletContext() {
						@Override
						public Dynamic addServlet(String servletName, Servlet servlet) {
							return Mockito.mock(Dynamic.class);
						}

						@Override
						public javax.servlet.FilterRegistration.Dynamic addFilter(
								String filterName, Filter filter) {
							// TODO: remove this when @ConditionalOnBean works
							return Mockito
									.mock(javax.servlet.FilterRegistration.Dynamic.class);
						}
					};
					for (ServletContextInitializer initializer : initializers) {
						try {
							initializer.onStartup(servletContext);
						} catch (ServletException ex) {
							throw new IllegalStateException(ex);
						}
					}
					return new EmbeddedServletContainer() {
						@Override
						public void stop() throws EmbeddedServletContainerException {
						}
					};
				}
			};
		}
	}

	@Controller
	@ConditionalOnManagementContext
	protected static class NewEndpoint {

	}

}
