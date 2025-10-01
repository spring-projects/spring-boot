/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure.security;

import org.springframework.context.ApplicationContext;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

/**
 * A custom {@link AbstractHttpConfigurer} that disables CSRF protection for gRPC
 * requests.
 * <p>
 * This configurer checks the application context to determine if CSRF protection should
 * be disabled for gRPC requests based on the property
 * {@code spring.grpc.server.security.csrf.enabled}. By default, CSRF protection is
 * disabled unless explicitly enabled in the application properties.
 * </p>
 *
 * @author Dave Syer
 * @since 4.0.0
 * @see AbstractHttpConfigurer
 * @see HttpSecurity
 */
public class GrpcDisableCsrfHttpConfigurer extends AbstractHttpConfigurer<GrpcDisableCsrfHttpConfigurer, HttpSecurity> {

	@Override
	public void init(HttpSecurity http) throws Exception {
		ApplicationContext context = http.getSharedObject(ApplicationContext.class);
		if (context != null && context.getBeanNamesForType(GrpcServiceDiscoverer.class).length == 1
				&& isServletEnabledAndCsrfDisabled(context) && isCsrfConfigurerPresent(http)) {
			http.csrf(this::disable);
		}
	}

	@SuppressWarnings("unchecked")
	private boolean isCsrfConfigurerPresent(HttpSecurity http) {
		return http.getConfigurer(CsrfConfigurer.class) != null;
	}

	private void disable(CsrfConfigurer<HttpSecurity> csrf) {
		csrf.requireCsrfProtectionMatcher(new AndRequestMatcher(CsrfFilter.DEFAULT_CSRF_MATCHER,
				new NegatedRequestMatcher(GrpcServletRequest.all())));
	}

	private boolean isServletEnabledAndCsrfDisabled(ApplicationContext context) {
		return context.getEnvironment().getProperty("spring.grpc.server.servlet.enabled", Boolean.class, true)
				&& !context.getEnvironment()
					.getProperty("spring.grpc.server.security.csrf.enabled", Boolean.class, false);
	}

}
