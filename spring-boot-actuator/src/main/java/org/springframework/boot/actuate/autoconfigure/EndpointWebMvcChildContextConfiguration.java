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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementWebSecurityAutoConfiguration.ManagementWebSecurityConfigurerAdapter;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.ManagementErrorEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.hateoas.HypermediaHttpMessageConverterConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Configuration triggered from {@link EndpointWebMvcAutoConfiguration} when a new
 * {@link EmbeddedServletContainer} running on a different port is required.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @see EndpointWebMvcAutoConfiguration
 */
@Configuration
@EnableWebMvc
@Import(ManagementContextConfigurationsImportSelector.class)
public class EndpointWebMvcChildContextConfiguration {

	private static Log logger = LogFactory
			.getLog(EndpointWebMvcChildContextConfiguration.class);

	@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
	public DispatcherServlet dispatcherServlet() {
		DispatcherServlet dispatcherServlet = new DispatcherServlet();
		// Ensure the parent configuration does not leak down to us
		dispatcherServlet.setDetectAllHandlerAdapters(false);
		dispatcherServlet.setDetectAllHandlerExceptionResolvers(false);
		dispatcherServlet.setDetectAllHandlerMappings(false);
		dispatcherServlet.setDetectAllViewResolvers(false);
		return dispatcherServlet;
	}

	@Bean(name = DispatcherServlet.HANDLER_MAPPING_BEAN_NAME)
	public CompositeHandlerMapping compositeHandlerMapping() {
		return new CompositeHandlerMapping();
	}

	@Bean(name = DispatcherServlet.HANDLER_ADAPTER_BEAN_NAME)
	public CompositeHandlerAdapter compositeHandlerAdapter() {
		return new CompositeHandlerAdapter();
	}

	@Bean(name = DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME)
	public CompositeHandlerExceptionResolver compositeHandlerExceptionResolver() {
		return new CompositeHandlerExceptionResolver();
	}

	@Bean
	public ServerCustomization serverCustomization() {
		return new ServerCustomization();
	}

	/*
	 * The error controller is present but not mapped as an endpoint in this context
	 * because of the DispatcherServlet having had its HandlerMapping explicitly disabled.
	 * So we expose the same feature but only for machine endpoints.
	 */
	@Bean
	@ConditionalOnBean(ErrorAttributes.class)
	public ManagementErrorEndpoint errorEndpoint(ServerProperties serverProperties,
			ErrorAttributes errorAttributes) {
		return new ManagementErrorEndpoint(serverProperties.getError().getPath(),
				errorAttributes);
	}

	/**
	 * Configuration to add {@link HandlerMapping} for {@link MvcEndpoint}s. See
	 * {@link SecureEndpointHandlerMappingConfiguration} for an extended version that also
	 * configures the security filter.
	 */
	@Configuration
	@ConditionalOnMissingClass("org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter")
	protected static class EndpointHandlerMappingConfiguration {

		@Autowired
		public void handlerMapping(MvcEndpoints endpoints,
				ListableBeanFactory beanFactory, EndpointHandlerMapping mapping) {
			// In a child context we definitely want to see the parent endpoints
			mapping.setDetectHandlerMethodsInAncestorContexts(true);
			postProcessMapping(beanFactory, mapping);
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
	protected static class SecureEndpointHandlerMappingConfiguration
			extends EndpointHandlerMappingConfiguration {

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

	@Configuration
	@ConditionalOnClass({ LinkDiscoverer.class })
	@Import(HypermediaHttpMessageConverterConfiguration.class)
	@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
	static class HypermediaConfiguration {

	}

	static class ServerCustomization
			implements EmbeddedServletContainerCustomizer, Ordered {

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
			container.setErrorPages(Collections.<ErrorPage>emptySet());
			// and add the management-specific bits
			container.setPort(this.managementServerProperties.getPort());
			container.setAddress(this.managementServerProperties.getAddress());
			container.addErrorPages(new ErrorPage(this.server.getError().getPath()));
		}

	}

	static class CompositeHandlerMapping implements HandlerMapping {

		@Autowired
		private ListableBeanFactory beanFactory;

		private List<HandlerMapping> mappings;

		@Override
		public HandlerExecutionChain getHandler(HttpServletRequest request)
				throws Exception {
			if (this.mappings == null) {
				this.mappings = extractMappings();
			}
			for (HandlerMapping mapping : this.mappings) {
				HandlerExecutionChain handler = mapping.getHandler(request);
				if (handler != null) {
					return handler;
				}
			}
			return null;
		}

		private List<HandlerMapping> extractMappings() {
			List<HandlerMapping> list = new ArrayList<HandlerMapping>();
			list.addAll(this.beanFactory.getBeansOfType(HandlerMapping.class).values());
			list.remove(this);
			AnnotationAwareOrderComparator.sort(list);
			return list;
		}

	}

	static class CompositeHandlerAdapter implements HandlerAdapter {

		@Autowired
		private ListableBeanFactory beanFactory;

		private List<HandlerAdapter> adapters;

		private List<HandlerAdapter> extractAdapters() {
			List<HandlerAdapter> list = new ArrayList<HandlerAdapter>();
			list.addAll(this.beanFactory.getBeansOfType(HandlerAdapter.class).values());
			list.remove(this);
			AnnotationAwareOrderComparator.sort(list);
			return list;
		}

		@Override
		public boolean supports(Object handler) {
			if (this.adapters == null) {
				this.adapters = extractAdapters();
			}
			for (HandlerAdapter mapping : this.adapters) {
				if (mapping.supports(handler)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public ModelAndView handle(HttpServletRequest request,
				HttpServletResponse response, Object handler) throws Exception {
			if (this.adapters == null) {
				this.adapters = extractAdapters();
			}
			for (HandlerAdapter mapping : this.adapters) {
				if (mapping.supports(handler)) {
					return mapping.handle(request, response, handler);
				}
			}
			return null;
		}

		@Override
		public long getLastModified(HttpServletRequest request, Object handler) {
			if (this.adapters == null) {
				this.adapters = extractAdapters();
			}
			for (HandlerAdapter mapping : this.adapters) {
				if (mapping.supports(handler)) {
					return mapping.getLastModified(request, handler);
				}
			}
			return 0;
		}

	}

	static class CompositeHandlerExceptionResolver implements HandlerExceptionResolver {

		@Autowired
		private ListableBeanFactory beanFactory;

		private List<HandlerExceptionResolver> resolvers;

		private List<HandlerExceptionResolver> extractResolvers() {
			List<HandlerExceptionResolver> list = new ArrayList<HandlerExceptionResolver>();
			list.addAll(this.beanFactory.getBeansOfType(HandlerExceptionResolver.class)
					.values());
			list.remove(this);
			AnnotationAwareOrderComparator.sort(list);
			return list;
		}

		@Override
		public ModelAndView resolveException(HttpServletRequest request,
				HttpServletResponse response, Object handler, Exception ex) {
			if (this.resolvers == null) {
				this.resolvers = extractResolvers();
			}
			for (HandlerExceptionResolver mapping : this.resolvers) {
				ModelAndView mav = mapping.resolveException(request, response, handler,
						ex);
				if (mav != null) {
					return mav;
				}
			}
			return null;
		}

	}

}
