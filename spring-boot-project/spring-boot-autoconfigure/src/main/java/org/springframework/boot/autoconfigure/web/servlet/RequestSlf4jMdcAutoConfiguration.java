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

package org.springframework.boot.autoconfigure.web.servlet;

import java.io.Closeable;
import java.io.IOException;
import java.security.Principal;
import java.util.EnumSet;
import java.util.function.Function;

import javax.servlet.DispatcherType;
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the slf4j {@link MDC}
 * providing the {@link HttpServletRequest} source (as 'src'), URI (as 'uri')
 * and user name (as 'uid'). If one does not exist for these or is empty, the
 * corresponding MDC entry will not be created. This runs as early as possible
 * in order to make these settings available to as much logging as possible.
 * 
 * @author Bruce Brouwer
 * @since 2.0
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(MDC.class)
public class RequestSlf4jMdcAutoConfiguration {

	@Bean
	public FilterRegistrationBean<RequestSlf4jMdcFilter> requestSlf4jMdcFilter() {
		final RequestSlf4jMdcFilter filter = new RequestSlf4jMdcFilter();
		final FilterRegistrationBean<RequestSlf4jMdcFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.ASYNC));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	public static class RequestSlf4jMdcFilter implements Filter {
		private static final Log logger = LogFactory.getLog(RequestSlf4jMdcFilter.class);

		@Override
		public void init(final FilterConfig filterConfig) throws ServletException {
			// nothing to configure
		}

		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
				throws IOException, ServletException {
			try (Closeable src = putMDC("src", RequestSlf4jMdcFilter::src, request)) {
				try (Closeable uri = putMDC("uri", RequestSlf4jMdcFilter::uri, request)) {
					try (Closeable uid = putMDC("uid", RequestSlf4jMdcFilter::uid, request)) {
						chain.doFilter(request, response);
					}
				}
			}
		}

		private static String src(HttpServletRequest request) {
			final String xff = request.getHeader("X-Forwarded-For");
			return StringUtils.hasLength(xff) ? xff : request.getRemoteHost();
		}

		private static String uri(HttpServletRequest request) {
			return request.getRequestURI();
		}

		private static String uid(HttpServletRequest request) {
			Principal user = request.getUserPrincipal();
			return user == null ? null : user.getName();
		}

		private static Closeable putMDC(String key, Function<HttpServletRequest, String> value,
				ServletRequest request) {
			if (request instanceof HttpServletRequest) {
				try {
					final String val = value.apply((HttpServletRequest) request);
					if (StringUtils.hasLength(val)) {
						return MDC.putCloseable(key, val);
					}
				} catch (Exception e) {
					logger.error("Cannot add '" + key + "' details to the MDC", e);
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
