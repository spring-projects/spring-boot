/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration.ManagementWebSecurityConfigurerAdapter;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer;
import org.springframework.boot.actuate.endpoint.mvc.ManagementErrorEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Configuration triggered from {@link EndpointWebMvcAutoConfiguration} when a new
 * {@link EmbeddedServletContainer} running on a different port is required.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @see EndpointWebMvcAutoConfiguration
 */
@Configuration
public class EndpointWebMvcChildContextConfiguration {

	private static Log logger = LogFactory
			.getLog(EndpointWebMvcChildContextConfiguration.class);

	@Value("${error.path:/error}")
	private String errorPath = "/error";

	@Configuration
	protected static class ServerCustomization implements
			EmbeddedServletContainerCustomizer, Ordered {

		@Value("${error.path:/error}")
		private String errorPath = "/error";

		@Autowired
		private ListableBeanFactory beanFactory;

		// This needs to be lazily initialized because EmbeddedServletContainerCustomizer
		// instances get their callback very early in the context lifecycle.
		private ManagementServerProperties managementServerProperties;

		private ServerProperties server;

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public void customize(ConfigurableEmbeddedServletContainer container) {
			if (this.managementServerProperties == null) {
				this.managementServerProperties = BeanFactoryUtils
						.beanOfTypeIncludingAncestors(this.beanFactory,
								ManagementServerProperties.class);
				this.server = BeanFactoryUtils.beanOfTypeIncludingAncestors(
						this.beanFactory, ServerProperties.class);
			}
			// Customize as per the parent context first (so e.g. the access logs go to
			// the same place)
			this.server.customize(container);
			// Then reset the error pages
			container.setErrorPages(Collections.<ErrorPage> emptySet());
			// and add the management-specific bits
			container.setPort(this.managementServerProperties.getPort());
			container.setAddress(this.managementServerProperties.getAddress());
			container.setContextPath(this.managementServerProperties.getContextPath());
			container.addErrorPages(new ErrorPage(this.errorPath));
		}

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
	public HandlerAdapter handlerAdapter(HttpMessageConverters converters) {
		RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
		adapter.setMessageConverters(converters.getConverters());
		return adapter;
	}

	/*
	 * The error controller is present but not mapped as an endpoint in this context
	 * because of the DispatcherServlet having had it's HandlerMapping explicitly
	 * disabled. So we expose the same feature but only for machine endpoints.
	 */
	@Bean
	public ManagementErrorEndpoint errorEndpoint(final ErrorAttributes errorAttributes) {
		return new ManagementErrorEndpoint(this.errorPath, errorAttributes);
	}

	/**
	 * Configuration to add {@link HandlerMapping} for {@link MvcEndpoint}s. See
	 * {@link SecureEndpointHandlerMappingConfiguration} for an extended version that also
	 * configures the security filter.
	 */
	@Configuration
	@ConditionalOnMissingClass("org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter")
	protected static class EndpointHandlerMappingConfiguration {

		@Autowired(required = false)
		private List<EndpointHandlerMappingCustomizer> mappingCustomizers;

		@Bean
		public HandlerMapping handlerMapping(MvcEndpoints endpoints,
				ListableBeanFactory beanFactory) {
			Set<MvcEndpoint> set = new HashSet<MvcEndpoint>(endpoints.getEndpoints());
			set.addAll(beanFactory.getBeansOfType(MvcEndpoint.class).values());
			EndpointHandlerMapping mapping = new EndpointHandlerMapping(set);
			// In a child context we definitely want to see the parent endpoints
			mapping.setDetectHandlerMethodsInAncestorContexts(true);
			postProcessMapping(beanFactory, mapping);
			if (this.mappingCustomizers != null) {
				for (EndpointHandlerMappingCustomizer customizer : this.mappingCustomizers) {
					customizer.customize(mapping);
				}
			}
			return mapping;
		}

		/**
		 * Hook to allow additional post processing of {@link EndpointHandlerMapping}.
		 * @param beanFactory the source bean factory
		 * @param mapping the mapping to customize
		 */
		protected void postProcessMapping(ListableBeanFactory beanFactory,
				EndpointHandlerMapping mapping) {
		}

	}

	/**
	 * Extension of {@link EndpointHandlerMappingConfiguration} that also configures the
	 * security filter.
	 */
	@Configuration
	@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
	protected static class SecureEndpointHandlerMappingConfiguration extends
			EndpointHandlerMappingConfiguration {

		@Override
		protected void postProcessMapping(ListableBeanFactory beanFactory,
				EndpointHandlerMapping mapping) {
			// The parent context has the security filter, so we need to get it injected
			// with our EndpointHandlerMapping if we can.
			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
					ManagementWebSecurityConfigurerAdapter.class).length == 1) {
				ManagementWebSecurityConfigurerAdapter bean = beanFactory
						.getBean(ManagementWebSecurityConfigurerAdapter.class);
				bean.setEndpointHandlerMapping(mapping);
			}
			else {
				logger.warn("No single bean of type "
						+ ManagementWebSecurityConfigurerAdapter.class.getSimpleName()
						+ " found (this might make some endpoints inaccessible without authentication)");
			}
		}

	}

	@Configuration
	@ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
	@ConditionalOnBean(name = "springSecurityFilterChain", search = SearchStrategy.PARENTS)
	public static class EndpointWebMvcChildContextSecurityConfiguration {

		@Bean
		public Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			return parent.getBean("springSecurityFilterChain", Filter.class);
		}

	}

}
