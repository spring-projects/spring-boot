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

package org.springframework.zero.actuate.fixme;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.zero.actuate.properties.ManagementServerProperties;
import org.springframework.zero.actuate.web.BasicErrorController;
import org.springframework.zero.context.condition.ConditionalOnBean;
import org.springframework.zero.context.condition.ConditionalOnClass;
import org.springframework.zero.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.zero.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.zero.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.zero.context.embedded.ErrorPage;
import org.springframework.zero.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.zero.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

/**
 * Configuration for creating a new container (e.g. tomcat) for the management endpoints.
 * 
 * @author Dave Syer
 */
@Configuration
@EnableWebMvc
@Import(ManagementSecurityConfiguration.class)
public class ManagementServerConfiguration {

	// FIXME delete when security works

	@Bean
	public DispatcherServlet dispatcherServlet() {
		return new DispatcherServlet();
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(
			ApplicationContext context) {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public BasicErrorController errorEndpoint() {
		return new BasicErrorController();
	}

	@Bean
	@ConditionalOnBean(TomcatEmbeddedServletContainerFactory.class)
	public EmbeddedServletContainerFactory tomcatContainer(
			HierarchicalBeanFactory beanFactory) {
		TomcatEmbeddedServletContainerFactory factory = beanFactory
				.getParentBeanFactory().getBean(
						TomcatEmbeddedServletContainerFactory.class);
		return factory.getChildContextFactory("Management");
	}

	@Bean
	@ConditionalOnBean(JettyEmbeddedServletContainerFactory.class)
	public EmbeddedServletContainerFactory jettyContainer() {
		return new JettyEmbeddedServletContainerFactory();
	}

	@Configuration
	protected static class ServerCustomizationConfiguration implements
			EmbeddedServletContainerCustomizer {

		@Value("${endpoints.error.path:/error}")
		private String errorPath = "/error";

		@Autowired
		private ApplicationContext beanFactory;

		@Override
		public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
			ManagementServerProperties configuration = this.beanFactory
					.getBean(ManagementServerProperties.class);
			factory.setPort(configuration.getPort());
			factory.setAddress(configuration.getAddress());
			factory.setContextPath(configuration.getContextPath());
			factory.addErrorPages(new ErrorPage(this.errorPath));
		}

	}

}

@Configuration
@ConditionalOnClass(name = {
		"org.springframework.security.config.annotation.web.EnableWebSecurity",
		"javax.servlet.Filter" })
class ManagementSecurityConfiguration {

	@Bean
	// TODO: enable and get rid of the empty filter when @ConditionalOnBean works
	// @ConditionalOnBean(name = "springSecurityFilterChain")
	public Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
		BeanFactory parent = beanFactory.getParentBeanFactory();
		if (parent != null && parent.containsBean("springSecurityFilterChain")) {
			return parent.getBean("springSecurityFilterChain", Filter.class);
		}
		return new GenericFilterBean() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response,
					FilterChain chain) throws IOException, ServletException {
				chain.doFilter(request, response);
			}
		};
	}

}
