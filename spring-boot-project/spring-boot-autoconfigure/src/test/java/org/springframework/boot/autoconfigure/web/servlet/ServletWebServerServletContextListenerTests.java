/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebServer}s driving {@link ServletContextListener}s correctly
 *
 * @author Andy Wilkinson
 */
class ServletWebServerServletContextListenerTests {

	@Test
	void registeredServletContextListenerBeanIsCalledByJetty() {
		registeredServletContextListenerBeanIsCalled(JettyConfiguration.class);
	}

	@Test
	void registeredServletContextListenerBeanIsCalledByTomcat() {
		registeredServletContextListenerBeanIsCalled(TomcatConfiguration.class);
	}

	@Test
	void registeredServletContextListenerBeanIsCalledByUndertow() {
		registeredServletContextListenerBeanIsCalled(UndertowConfiguration.class);
	}

	@Test
	void servletContextListenerBeanIsCalledByJetty() {
		servletContextListenerBeanIsCalled(JettyConfiguration.class);
	}

	@Test
	void servletContextListenerBeanIsCalledByTomcat() {
		servletContextListenerBeanIsCalled(TomcatConfiguration.class);
	}

	@Test
	void servletContextListenerBeanIsCalledByUndertow() {
		servletContextListenerBeanIsCalled(UndertowConfiguration.class);
	}

	private void servletContextListenerBeanIsCalled(Class<?> configuration) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
				ServletContextListenerBeanConfiguration.class, configuration);
		ServletContextListener servletContextListener = context.getBean("servletContextListener",
				ServletContextListener.class);
		then(servletContextListener).should().contextInitialized(any(ServletContextEvent.class));
		context.close();
	}

	private void registeredServletContextListenerBeanIsCalled(Class<?> configuration) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
				ServletListenerRegistrationBeanConfiguration.class, configuration);
		ServletContextListener servletContextListener = (ServletContextListener) context
				.getBean("registration", ServletListenerRegistrationBean.class).getListener();
		then(servletContextListener).should().contextInitialized(any(ServletContextEvent.class));
		context.close();
	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatConfiguration {

		@Bean
		ServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JettyConfiguration {

		@Bean
		ServletWebServerFactory webServerFactory() {
			return new JettyServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UndertowConfiguration {

		@Bean
		ServletWebServerFactory webServerFactory() {
			return new UndertowServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServletContextListenerBeanConfiguration {

		@Bean
		ServletContextListener servletContextListener() {
			return mock(ServletContextListener.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServletListenerRegistrationBeanConfiguration {

		@Bean
		ServletListenerRegistrationBean<ServletContextListener> registration() {
			return new ServletListenerRegistrationBean<>(mock(ServletContextListener.class));
		}

	}

}
