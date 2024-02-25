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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import jakarta.servlet.Servlet;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.servlet.filter.ApplicationContextHeaderFilter;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Servlet-specific management
 * context concerns.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Servlet.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class ServletManagementContextAutoConfiguration {

	/**
     * Creates a new instance of the {@link ManagementContextFactory} for a servlet web application.
     * This factory is used to configure the management context for a servlet-based web server.
     * 
     * @return The {@link ManagementContextFactory} instance for a servlet web application.
     */
    @Bean
	public static ManagementContextFactory servletWebChildContextFactory() {
		return new ManagementContextFactory(WebApplicationType.SERVLET, ServletWebServerFactory.class,
				ServletWebServerFactoryAutoConfiguration.class);
	}

	/**
     * Creates a ManagementServletContext bean that returns the base path of the management endpoints.
     * 
     * @param properties the WebEndpointProperties object used to retrieve the base path
     * @return a ManagementServletContext bean that returns the base path of the management endpoints
     */
    @Bean
	public ManagementServletContext managementServletContext(WebEndpointProperties properties) {
		return properties::getBasePath;
	}

	// Put Servlets and Filters in their own nested class so they don't force early
	// instantiation of ManagementServerProperties.
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "management.server", name = "add-application-context-header", havingValue = "true")
	protected static class ApplicationContextFilterConfiguration {

		/**
         * Creates a new ApplicationContextHeaderFilter instance with the given ApplicationContext.
         * 
         * @param context the ApplicationContext to be used by the filter
         * @return a new ApplicationContextHeaderFilter instance
         */
        @Bean
		public ApplicationContextHeaderFilter applicationContextIdFilter(ApplicationContext context) {
			return new ApplicationContextHeaderFilter(context);
		}

	}

}
