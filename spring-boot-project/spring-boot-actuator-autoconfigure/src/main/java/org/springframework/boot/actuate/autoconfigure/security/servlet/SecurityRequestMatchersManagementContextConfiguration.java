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

package org.springframework.boot.actuate.autoconfigure.security.servlet;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.AntPathRequestMatcherProvider;
import org.springframework.boot.autoconfigure.security.servlet.RequestMatcherProvider;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link ManagementContextConfiguration} that configures the appropriate
 * {@link RequestMatcherProvider}.
 *
 * @author Madhura Bhave
 * @since 2.1.8
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnClass({ RequestMatcher.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityRequestMatchersManagementContextConfiguration {

	/**
     * MvcRequestMatcherConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(DispatcherServlet.class)
	@ConditionalOnBean(DispatcherServletPath.class)
	public static class MvcRequestMatcherConfiguration {

		/**
         * Creates a RequestMatcherProvider bean if it is missing and the DispatcherServlet class is present.
         * The RequestMatcherProvider bean is responsible for providing request matchers based on the servlet path.
         * 
         * @param servletPath The DispatcherServletPath bean used to get the relative path.
         * @return The created AntPathRequestMatcherProvider bean.
         */
        @Bean
		@ConditionalOnMissingBean
		@ConditionalOnClass(DispatcherServlet.class)
		public RequestMatcherProvider requestMatcherProvider(DispatcherServletPath servletPath) {
			return new AntPathRequestMatcherProvider(servletPath::getRelativePath);
		}

	}

	/**
     * JerseyRequestMatcherConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ResourceConfig.class)
	@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
	@ConditionalOnBean(JerseyApplicationPath.class)
	public static class JerseyRequestMatcherConfiguration {

		/**
         * Creates a RequestMatcherProvider bean.
         * 
         * @param applicationPath the JerseyApplicationPath object representing the application path
         * @return the RequestMatcherProvider object
         */
        @Bean
		public RequestMatcherProvider requestMatcherProvider(JerseyApplicationPath applicationPath) {
			return new AntPathRequestMatcherProvider(applicationPath::getRelativePath);
		}

	}

}
