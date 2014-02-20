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

package org.springframework.boot.autoconfigure.web;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link EmbeddedServletContainerAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class EmbeddedServletContainerAutoConfigurationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Test
	public void createFromConfigClass() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				BaseConfiguration.class);
		verifyContext();
	}

	@Test
	public void contextAlreadyHasDispatcherServletWithDefaultName() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				DispatcherServletConfiguration.class, BaseConfiguration.class);
		verifyContext();
	}

	@Test
	public void contextAlreadyHasDispatcherServlet() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				SpringServletConfiguration.class, BaseConfiguration.class);
		verifyContext();
		assertEquals(2, this.context.getBeanNamesForType(DispatcherServlet.class).length);
	}

	@Test
	public void contextAlreadyHasNonDispatcherServlet() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				NonSpringServletConfiguration.class, BaseConfiguration.class);
		verifyContext(); // the non default servlet is still registered
		assertEquals(0, this.context.getBeanNamesForType(DispatcherServlet.class).length);
	}

	@Test
	public void contextAlreadyHasNonServlet() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				NonServletConfiguration.class, BaseConfiguration.class);
		assertEquals(0, this.context.getBeanNamesForType(DispatcherServlet.class).length);
		assertEquals(0, this.context.getBeanNamesForType(Servlet.class).length);
	}

	@Test
	public void contextAlreadyHasDispatcherServletAndRegistration() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				DispatcherServletWithRegistrationConfiguration.class,
				BaseConfiguration.class);
		verifyContext();
		assertEquals(1, this.context.getBeanNamesForType(DispatcherServlet.class).length);
	}

	@Test
	public void containerHasNoServletContext() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				EnsureContainerHasNoServletContext.class, BaseConfiguration.class);
		verifyContext();
	}

	@Test
	public void customizeContainerThroughCallback() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				CallbackEmbeddedContainerCustomizer.class, BaseConfiguration.class);
		verifyContext();
		assertEquals(9000, getContainerFactory().getPort());
	}

	private void verifyContext() {
		MockEmbeddedServletContainerFactory containerFactory = getContainerFactory();
		Servlet servlet = this.context.getBean(
				DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME,
				Servlet.class);
		verify(containerFactory.getServletContext()).addServlet("dispatcherServlet",
				servlet);
	}

	private MockEmbeddedServletContainerFactory getContainerFactory() {
		return this.context.getBean(MockEmbeddedServletContainerFactory.class);
	}

	@Configuration
	@Import({ EmbeddedContainerConfiguration.class,
			EmbeddedServletContainerAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class })
	protected static class BaseConfiguration {

	}

	@Configuration
	@ConditionalOnExpression("true")
	public static class EmbeddedContainerConfiguration {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return new MockEmbeddedServletContainerFactory();
		}

	}

	@Configuration
	public static class DispatcherServletConfiguration {

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

	}

	@Configuration
	public static class SpringServletConfiguration {

		@Bean
		public DispatcherServlet springServlet() {
			return new DispatcherServlet();
		}

	}

	@Configuration
	public static class NonSpringServletConfiguration {

		@Bean
		public FrameworkServlet dispatcherServlet() {
			return new FrameworkServlet() {
				@Override
				protected void doService(HttpServletRequest request,
						HttpServletResponse response) throws Exception {
				}
			};
		}

	}

	@Configuration
	public static class NonServletConfiguration {

		@Bean
		public String dispatcherServlet() {
			return "foo";
		}

	}

	@Configuration
	public static class DispatcherServletWithRegistrationConfiguration {

		@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public ServletRegistrationBean dispatcherRegistration() {
			return new ServletRegistrationBean(dispatcherServlet(), "/app/*");
		}

	}

	@Component
	public static class EnsureContainerHasNoServletContext implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof ConfigurableEmbeddedServletContainerFactory) {
				MockEmbeddedServletContainerFactory containerFactory = (MockEmbeddedServletContainerFactory) bean;
				assertNull(containerFactory.getServletContext());
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			return bean;
		}

	}

	@Component
	public static class CallbackEmbeddedContainerCustomizer implements
			EmbeddedServletContainerCustomizer {
		@Override
		public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
			factory.setPort(9000);
		}
	}

}
