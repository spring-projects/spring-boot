/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.security;

import java.io.Closeable;
import java.io.IOException;
import java.security.Principal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the slf4j {@link MDC}
 * providing Spring Security authenticate user's name as 'uid'. If there is no
 * authenticated user, the corresponding MDC entry will not be created. This
 * runs immediately after Spring Security's filter chain in order to make these
 * settings available as early as possible to as much logging as possible.
 *
 * @author Bruce Brouwer
 * @since 2.0
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnClass({ AbstractSecurityWebApplicationInitializer.class, MDC.class })
@AutoConfigureAfter(SecurityFilterAutoConfiguration.class)
public class Slf4jMdcSecurityFilterAutoConfiguration {

	private static final String DEFAULT_FILTER_NAME = AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME;

	@Bean
	@ConditionalOnBean(name = DEFAULT_FILTER_NAME)
	public FilterRegistrationBean<Slf4jMdcSecurityFilter> uidSlf4jMdcFilter(SecurityProperties securityProperties) {
		final Slf4jMdcSecurityFilter filter = new Slf4jMdcSecurityFilter();
		final FilterRegistrationBean<Slf4jMdcSecurityFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(securityProperties.getFilter().getOrder() + 1);
		registration.setDispatcherTypes(securityProperties.getFilter().getServletDispatcherTypes());
		return registration;
	}

	public static class Slf4jMdcSecurityFilter implements Filter {
		private static final Log logger = LogFactory.getLog(Slf4jMdcSecurityFilter.class);

		@Override
		public void init(final FilterConfig filterConfig) throws ServletException {
			// nothing to configure
		}

		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
				throws IOException, ServletException {
			try (Closeable uid = putUidMDC(request)) {
				chain.doFilter(request, response);
			}
		}

		private Closeable putUidMDC(ServletRequest request) {
			if (request instanceof HttpServletRequest) {
				try {
					final Principal user = ((HttpServletRequest) request).getUserPrincipal();
					return MDC.putCloseable("uid", user == null ? null : user.getName());
				} catch (Exception e) {
					logger.error("Cannot add user principal details as 'uid' to the MDC", e);
				}
			}
			return null;
		}

		@Override
		public void destroy() {
			// nothing to clean up
		}
	}

}
