/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServletWebServerFactoryAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class ServletWebServerFactoryAutoConfigurationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	@Test
	public void createFromConfigClass() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				BaseConfiguration.class);
		verifyContext();
	}

	@Test
	public void contextAlreadyHasDispatcherServletWithDefaultName() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				DispatcherServletConfiguration.class, BaseConfiguration.class);
		verifyContext();
	}

	@Test
	public void contextAlreadyHasDispatcherServlet() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				SpringServletConfiguration.class, BaseConfiguration.class);
		verifyContext();
		assertThat(this.context.getBeanNamesForType(DispatcherServlet.class).length)
				.isEqualTo(2);
	}

	@Test
	public void contextAlreadyHasNonDispatcherServlet() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				NonSpringServletConfiguration.class, BaseConfiguration.class);
		verifyContext(); // the non default servlet is still registered
		assertThat(this.context.getBeanNamesForType(DispatcherServlet.class).length)
				.isEqualTo(0);
	}

	@Test
	public void contextAlreadyHasNonServlet() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				NonServletConfiguration.class, BaseConfiguration.class);
		assertThat(this.context.getBeanNamesForType(DispatcherServlet.class).length)
				.isEqualTo(0);
		assertThat(this.context.getBeanNamesForType(Servlet.class).length).isEqualTo(0);
	}

	@Test
	public void contextAlreadyHasDispatcherServletAndRegistration() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				DispatcherServletWithRegistrationConfiguration.class,
				BaseConfiguration.class);
		verifyContext();
		assertThat(this.context.getBeanNamesForType(DispatcherServlet.class).length)
				.isEqualTo(1);
	}

	@Test
	public void webServerHasNoServletContext() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				EnsureWebServerHasNoServletContext.class, BaseConfiguration.class);
		verifyContext();
	}

	@Test
	public void customizeWebServerFactoryThroughCallback() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				CallbackEmbeddedServerFactoryCustomizer.class, BaseConfiguration.class);
		verifyContext();
		assertThat(getWebServerFactory().getPort()).isEqualTo(9000);
	}

	@Test
	public void initParametersAreConfiguredOnTheServletContext() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		TestPropertyValues
				.of("server.servlet.context-parameters.a:alpha",
						"server.servlet.context-parameters.b:bravo")
				.applyTo(this.context);
		this.context.register(BaseConfiguration.class);
		this.context.refresh();

		ServletContext servletContext = this.context.getServletContext();
		assertThat(servletContext.getInitParameter("a")).isEqualTo("alpha");
		assertThat(servletContext.getInitParameter("b")).isEqualTo("bravo");
	}

	private void verifyContext() {
		MockServletWebServerFactory factory = getWebServerFactory();
		Servlet servlet = this.context.getBean(
				DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME,
				Servlet.class);
		verify(factory.getServletContext()).addServlet("dispatcherServlet", servlet);
	}

	private MockServletWebServerFactory getWebServerFactory() {
		return this.context.getBean(MockServletWebServerFactory.class);
	}

	@Configuration
	@Import({ WebServerConfiguration.class,
			ServletWebServerFactoryAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class })
	protected static class BaseConfiguration {

	}

	@Configuration
	@ConditionalOnExpression("true")
	public static class WebServerConfiguration {

		@Bean
		public ServletWebServerFactory webServerFactory() {
			return new MockServletWebServerFactory();
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
						HttpServletResponse response) {
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

		@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
		public ServletRegistrationBean<DispatcherServlet> dispatcherRegistration() {
			return new ServletRegistrationBean<>(dispatcherServlet(), "/app/*");
		}

	}

	@Component
	public static class EnsureWebServerHasNoServletContext implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof ConfigurableServletWebServerFactory) {
				MockServletWebServerFactory webServerFactory = (MockServletWebServerFactory) bean;
				assertThat(webServerFactory.getServletContext()).isNull();
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			return bean;
		}

	}

	@Component
	public static class CallbackEmbeddedServerFactoryCustomizer
			implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

		@Override
		public void customize(ConfigurableServletWebServerFactory serverFactory) {
			serverFactory.setPort(9000);
		}

	}

}
