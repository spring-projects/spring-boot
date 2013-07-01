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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.actuate.endpoint.mvc.EndpointHandlerAdapter;
import org.springframework.bootstrap.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.bootstrap.context.annotation.ConditionalOnBean;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainer;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Configuration for triggered from {@link EndpointWebMvcAutoConfiguration} when a new
 * {@link EmbeddedServletContainer} running on a different port is required.
 * 
 * @see EndpointWebMvcAutoConfiguration
 */
@Configuration
public class EndpointWebMvcChildContextConfiguration implements
		EmbeddedServletContainerCustomizer {

	@Autowired
	private ManagementServerProperties managementServerProperties;

	@Override
	public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
		factory.setPort(this.managementServerProperties.getPort());
		factory.setAddress(this.managementServerProperties.getAddress());
		factory.setContextPath(this.managementServerProperties.getContextPath());
	}

	@Bean
	public DispatcherServlet dispatcherServlet() {
		DispatcherServlet dispatcherServlet = new DispatcherServlet();

		// Ensure the parent configuration does not leak down to us
		dispatcherServlet.setDetectAllHandlerAdapters(false);
		dispatcherServlet.setDetectAllHandlerExceptionResolvers(false);
		dispatcherServlet.setDetectAllHandlerMappings(false);
		dispatcherServlet.setDetectAllViewResolvers(false);

		return dispatcherServlet;
	}

	@Bean
	public HandlerMapping handlerMapping() {
		return new EndpointHandlerMapping();
	}

	@Bean
	public HandlerAdapter handlerAdapter() {
		return new EndpointHandlerAdapter();
	}

	@Configuration
	@ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
	public static class EndpointWebMvcChildContextSecurityConfiguration {

		// FIXME reuse of security filter here is not good. What if totally different
		// security config is required. Perhaps we can just drop it on the management
		// port?

		@Bean
		@ConditionalOnBean(name = "springSecurityFilterChain")
		public Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			return parent.getBean("springSecurityFilterChain", Filter.class);
		}

	}

}
