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

package org.springframework.autoconfigure.web;

import javax.servlet.Servlet;

import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.springframework.autoconfigure.EnableAutoConfiguration;
import org.springframework.bootstrap.context.condition.ConditionalOnClass;
import org.springframework.bootstrap.context.condition.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.ServletContextInitializer;
import org.springframework.bootstrap.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for an embedded servlet containers.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmbeddedServletContainerAutoConfiguration {

	/**
	 * Support {@link EmbeddedServletContainerCustomizerBeanPostProcessor} to apply
	 * {@link EmbeddedServletContainerCustomizer}s.
	 */
	@Bean
	@ConditionalOnMissingBean(value = EmbeddedServletContainerCustomizerBeanPostProcessor.class, parentContext = false)
	public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
		return new EmbeddedServletContainerCustomizerBeanPostProcessor();
	}

	/**
	 * Add the {@link DispatcherServlet} unless the user has defined their own
	 * {@link ServletContextInitializer}s.
	 */
	@ConditionalOnClass(DispatcherServlet.class)
	public static class DispatcherServletConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = { ServletContextInitializer.class,
				Servlet.class }, parentContext = false)
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}
	}

	/**
	 * Nested configuration for if Tomcat is being used.
	 */
	@Configuration
	@ConditionalOnClass({ Servlet.class, Tomcat.class })
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, parentContext = false)
	public static class EmbeddedTomcat {

		@Bean
		public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

	}

	/**
	 * Nested configuration if Jetty is being used.
	 */
	@Configuration
	@ConditionalOnClass({ Servlet.class, Server.class, Loader.class })
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, parentContext = false)
	public static class EmbeddedJetty {

		@Bean
		public JettyEmbeddedServletContainerFactory jettyEmbeddedServletContainerFactory() {
			return new JettyEmbeddedServletContainerFactory();
		}

	}

}
