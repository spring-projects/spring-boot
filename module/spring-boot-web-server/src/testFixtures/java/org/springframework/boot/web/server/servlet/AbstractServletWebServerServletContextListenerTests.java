/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Base class for tests for {@link WebServer}s driving {@link ServletContextListener}s
 * correctly.
 *
 * @author Andy Wilkinson
 */
@DirtiesUrlFactories
public abstract class AbstractServletWebServerServletContextListenerTests {

	private final Class<?> webServerConfiguration;

	protected AbstractServletWebServerServletContextListenerTests(Class<?> webServerConfiguration) {
		this.webServerConfiguration = webServerConfiguration;
	}

	@Test
	@ForkedClassPath
	void registeredServletContextListenerBeanIsCalled() {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
				ServletListenerRegistrationBeanConfiguration.class, this.webServerConfiguration);
		ServletContextListener servletContextListener = (ServletContextListener) context
			.getBean("registration", ServletListenerRegistrationBean.class)
			.getListener();
		then(servletContextListener).should().contextInitialized(any(ServletContextEvent.class));
		context.close();
	}

	@Test
	@ForkedClassPath
	void servletContextListenerBeanIsCalled() {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
				ServletContextListenerBeanConfiguration.class, this.webServerConfiguration);
		ServletContextListener servletContextListener = context.getBean("servletContextListener",
				ServletContextListener.class);
		then(servletContextListener).should().contextInitialized(any(ServletContextEvent.class));
		context.close();
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
