/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.filter.OrderedRequestContextFilter;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Spring MVC
 * infrastructure when a separate management context with a web server running on a
 * different port is required.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@EnableWebMvc
class WebMvcEndpointChildContextConfiguration {

	/*
	 * The error controller is present but not mapped as an endpoint in this context
	 * because of the DispatcherServlet having had its HandlerMapping explicitly disabled.
	 * So we expose the same feature but only for machine endpoints.
	 */
	@Bean
	@ConditionalOnBean(ErrorAttributes.class)
	ManagementErrorEndpoint errorEndpoint(ErrorAttributes errorAttributes, ServerProperties serverProperties) {
		return new ManagementErrorEndpoint(errorAttributes, serverProperties.getError());
	}

	/**
     * Creates a ManagementErrorPageCustomizer bean if an ErrorAttributes bean is present.
     * 
     * @param serverProperties the ServerProperties bean used to customize the error pages
     * @return the ManagementErrorPageCustomizer bean
     */
    @Bean
	@ConditionalOnBean(ErrorAttributes.class)
	ManagementErrorPageCustomizer managementErrorPageCustomizer(ServerProperties serverProperties) {
		return new ManagementErrorPageCustomizer(serverProperties);
	}

	/**
     * Creates a new instance of the {@link DispatcherServlet} with customized settings.
     * 
     * @return The created {@link DispatcherServlet} instance.
     */
    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
	DispatcherServlet dispatcherServlet() {
		DispatcherServlet dispatcherServlet = new DispatcherServlet();
		// Ensure the parent configuration does not leak down to us
		dispatcherServlet.setDetectAllHandlerAdapters(false);
		dispatcherServlet.setDetectAllHandlerExceptionResolvers(false);
		dispatcherServlet.setDetectAllHandlerMappings(false);
		dispatcherServlet.setDetectAllViewResolvers(false);
		return dispatcherServlet;
	}

	/**
     * Creates a new instance of {@link DispatcherServletRegistrationBean} with the provided {@link DispatcherServlet} and root mapping.
     * 
     * @param dispatcherServlet the {@link DispatcherServlet} instance to be registered
     * @return the {@link DispatcherServletRegistrationBean} instance
     */
    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
	DispatcherServletRegistrationBean dispatcherServletRegistrationBean(DispatcherServlet dispatcherServlet) {
		return new DispatcherServletRegistrationBean(dispatcherServlet, "/");
	}

	/**
     * Creates a new instance of CompositeHandlerMapping.
     * 
     * @return the created CompositeHandlerMapping instance
     */
    @Bean(name = DispatcherServlet.HANDLER_MAPPING_BEAN_NAME)
	CompositeHandlerMapping compositeHandlerMapping() {
		return new CompositeHandlerMapping();
	}

	/**
     * Creates a composite handler adapter for the DispatcherServlet.
     * 
     * @param beanFactory the ListableBeanFactory used to retrieve the handler adapters
     * @return the CompositeHandlerAdapter instance
     */
    @Bean(name = DispatcherServlet.HANDLER_ADAPTER_BEAN_NAME)
	CompositeHandlerAdapter compositeHandlerAdapter(ListableBeanFactory beanFactory) {
		return new CompositeHandlerAdapter(beanFactory);
	}

	/**
     * Creates a new instance of CompositeHandlerExceptionResolver.
     * 
     * @return the newly created CompositeHandlerExceptionResolver instance
     */
    @Bean(name = DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME)
	CompositeHandlerExceptionResolver compositeHandlerExceptionResolver() {
		return new CompositeHandlerExceptionResolver();
	}

	/**
     * Creates and returns a new instance of {@link RequestContextFilter} if no bean of type {@link RequestContextListener} or {@link RequestContextFilter} is already present in the application context.
     * 
     * The created {@link RequestContextFilter} is an instance of {@link OrderedRequestContextFilter}.
     * 
     * @return the created {@link RequestContextFilter}
     */
    @Bean
	@ConditionalOnMissingBean({ RequestContextListener.class, RequestContextFilter.class })
	RequestContextFilter requestContextFilter() {
		return new OrderedRequestContextFilter();
	}

	/**
	 * {@link WebServerFactoryCustomizer} to add an {@link ErrorPage} so that the
	 * {@link ManagementErrorEndpoint} can be used.
	 */
	static class ManagementErrorPageCustomizer
			implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, Ordered {

		private final ServerProperties properties;

		/**
         * Constructs a new ManagementErrorPageCustomizer with the specified ServerProperties.
         *
         * @param properties the ServerProperties to be used by the customizer
         */
        ManagementErrorPageCustomizer(ServerProperties properties) {
			this.properties = properties;
		}

		/**
         * Customize the ConfigurableServletWebServerFactory to add error pages.
         * 
         * @param factory the ConfigurableServletWebServerFactory to customize
         */
        @Override
		public void customize(ConfigurableServletWebServerFactory factory) {
			factory.addErrorPages(new ErrorPage(this.properties.getError().getPath()));
		}

		/**
         * Returns the order in which this customizer should be applied.
         * 
         * @return the order value, with lower values indicating higher priority
         */
        @Override
		public int getOrder() {
			return 0;
		}

	}

}
