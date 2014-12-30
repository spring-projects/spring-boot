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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.junit.Test;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ServletListenerRegistrationBean;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link EmbeddedServletContainer}s driving {@link ServletContextListener}s
 * correctly
 *
 * @author Andy Wilkinson
 */
public class EmbeddedServletContainerServletContextListenerTests {

	@Test
	public void registeredServletContextListenerBeanIsCalledByJetty() {
		registeredServletContextListenerBeanIsCalled(JettyConfiguration.class);
	}

	@Test
	public void registeredServletContextListenerBeanIsCalledByTomcat() {
		registeredServletContextListenerBeanIsCalled(TomcatConfiguration.class);
	}

	@Test
	public void registeredServletContextListenerBeanIsCalledByUndertow() {
		registeredServletContextListenerBeanIsCalled(UndertowConfiguration.class);
	}

	@Test
	public void servletContextListenerBeanIsCalledByJetty() {
		servletContextListenerBeanIsCalled(JettyConfiguration.class);
	}

	@Test
	public void servletContextListenerBeanIsCalledByTomcat() {
		servletContextListenerBeanIsCalled(TomcatConfiguration.class);
	}

	@Test
	public void servletContextListenerBeanIsCalledByUndertow() {
		servletContextListenerBeanIsCalled(UndertowConfiguration.class);
	}

	private void servletContextListenerBeanIsCalled(Class<?> configuration) {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext(
				ServletContextListenerBeanConfiguration.class, configuration);
		ServletContextListener servletContextListener = context.getBean(
				"servletContextListener", ServletContextListener.class);
		verify(servletContextListener).contextInitialized(any(ServletContextEvent.class));
		context.close();
	}

	private void registeredServletContextListenerBeanIsCalled(Class<?> configuration) {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext(
				ServletListenerRegistrationBeanConfiguration.class, configuration);
		ServletContextListener servletContextListener = (ServletContextListener) context
				.getBean("registration", ServletListenerRegistrationBean.class)
				.getListener();
		verify(servletContextListener).contextInitialized(any(ServletContextEvent.class));
		context.close();
	}

	@Configuration
	static class TomcatConfiguration {

		@Bean
		public EmbeddedServletContainerFactory servletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory(0);
		}

	}

	@Configuration
	static class JettyConfiguration {

		@Bean
		public EmbeddedServletContainerFactory servletContainerFactory() {
			return new JettyEmbeddedServletContainerFactory(0);
		}

	}

	@Configuration
	static class UndertowConfiguration {

		@Bean
		public EmbeddedServletContainerFactory servletContainerFactory() {
			return new UndertowEmbeddedServletContainerFactory(0);
		}

	}

	@Configuration
	static class ServletContextListenerBeanConfiguration {

		@Bean
		public ServletContextListener servletContextListener() {
			return mock(ServletContextListener.class);
		}

	}

	@Configuration
	static class ServletListenerRegistrationBeanConfiguration {

		@Bean
		public ServletListenerRegistrationBean<ServletContextListener> registration() {
			return new ServletListenerRegistrationBean<ServletContextListener>(
					mock(ServletContextListener.class));
		}

	}

}
