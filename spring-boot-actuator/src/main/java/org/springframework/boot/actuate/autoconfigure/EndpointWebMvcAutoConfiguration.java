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

package org.springframework.boot.actuate.autoconfigure;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerAdapter;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.properties.ManagementServerProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.properties.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to enable Spring MVC to handle
 * {@link Endpoint} requests. If the {@link ManagementServerProperties} specifies a
 * different port to {@link ServerProperties} a new child context is created, otherwise it
 * is assumed that endpoint requests will be mapped and handled via an already registered
 * {@link DispatcherServlet}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnWebApplication
@AutoConfigureAfter({ PropertyPlaceholderAutoConfiguration.class,
		EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class,
		ManagementServerPropertiesAutoConfiguration.class })
public class EndpointWebMvcAutoConfiguration implements ApplicationContextAware,
		ApplicationListener<ContextRefreshedEvent> {

	private static final Integer DISABLED_PORT = Integer.valueOf(0);

	private ApplicationContext applicationContext;

	@Autowired(required = false)
	private ManagementServerProperties managementServerProperties = new ManagementServerProperties();

	@Bean
	@ConditionalOnMissingBean
	public EndpointHandlerMapping endpointHandlerMapping() {
		EndpointHandlerMapping mapping = new EndpointHandlerMapping();
		mapping.setDisabled(ManagementServerPort.get(this.applicationContext) != ManagementServerPort.SAME);
		mapping.setPrefix(this.managementServerProperties.getContextPath());
		return mapping;
	}

	@Bean
	@ConditionalOnMissingBean
	public EndpointHandlerAdapter endpointHandlerAdapter() {
		return new EndpointHandlerAdapter();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() == this.applicationContext) {
			if (ManagementServerPort.get(this.applicationContext) == ManagementServerPort.DIFFERENT
					&& this.applicationContext instanceof WebApplicationContext) {
				createChildManagementContext();
			}
		}
	}

	@Bean
	public Filter applicationContextIdFilter(ApplicationContext context) {
		final String id = context.getId();
		return new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request,
					HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
				response.addHeader("X-Application-Context", id);
				filterChain.doFilter(request, response);
			}
		};
	}

	private void createChildManagementContext() {

		final AnnotationConfigEmbeddedWebApplicationContext childContext = new AnnotationConfigEmbeddedWebApplicationContext();
		childContext.setParent(this.applicationContext);
		childContext.setId(this.applicationContext.getId() + ":management");

		// Register the ManagementServerChildContextConfiguration first followed
		// by various specific AutoConfiguration classes. NOTE: The child context
		// is intentionally not completely auto-configured.
		childContext.register(EndpointWebMvcChildContextConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);

		// Ensure close on the parent also closes the child
		if (this.applicationContext instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) this.applicationContext)
					.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
						@Override
						public void onApplicationEvent(ContextClosedEvent event) {
							if (event.getApplicationContext() == EndpointWebMvcAutoConfiguration.this.applicationContext) {
								childContext.close();
							}
						}
					});
		}
		childContext.refresh();
	}

	private enum ManagementServerPort {

		DISABLE, SAME, DIFFERENT;

		public static ManagementServerPort get(ApplicationContext beanFactory) {

			ServerProperties serverProperties;
			try {
				serverProperties = beanFactory.getBean(ServerProperties.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				serverProperties = new ServerProperties();
			}

			ManagementServerProperties managementServerProperties;
			try {
				managementServerProperties = beanFactory
						.getBean(ManagementServerProperties.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				managementServerProperties = new ManagementServerProperties();
			}

			if (DISABLED_PORT.equals(managementServerProperties.getPort())) {
				return DISABLE;
			}
			if (!(beanFactory instanceof GenericWebApplicationContext)) {
				// Current context is no a a webapp
				return DIFFERENT;
			}
			return managementServerProperties.getPort() == null
					|| serverProperties.getPort() == null
					&& managementServerProperties.getPort().equals(8080)
					|| managementServerProperties.getPort().equals(
							serverProperties.getPort()) ? SAME : DIFFERENT;
		}
	};
}
