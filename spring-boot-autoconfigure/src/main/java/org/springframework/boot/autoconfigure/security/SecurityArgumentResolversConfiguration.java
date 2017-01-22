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

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.method.annotation.CsrfTokenArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Configuration for Spring Security's argument resolvers.
 *
 * @author Vedran Pavic
 * @since 1.4.4
 */
@Configuration
@ConditionalOnClass({ WebMvcConfigurerAdapter.class,
		AuthenticationPrincipalArgumentResolver.class, CsrfTokenArgumentResolver.class })
@ConditionalOnWebApplication
public class SecurityArgumentResolversConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AuthenticationPrincipalArgumentResolver authenticationPrincipalArgumentResolver() {
		return new AuthenticationPrincipalArgumentResolver();
	}

	@Bean
	@ConditionalOnMissingBean
	public CsrfTokenArgumentResolver csrfTokenArgumentResolver() {
		return new CsrfTokenArgumentResolver();
	}

	@Configuration
	@Order(0)
	protected static class SecurityMvcConfiguration extends WebMvcConfigurerAdapter {

		private final AuthenticationPrincipalArgumentResolver authenticationPrincipalArgumentResolver;

		private final CsrfTokenArgumentResolver csrfTokenArgumentResolver;

		public SecurityMvcConfiguration(
				ObjectProvider<AuthenticationPrincipalArgumentResolver> authenticationPrincipalArgumentResolver,
				ObjectProvider<CsrfTokenArgumentResolver> csrfTokenArgumentResolver) {
			this.authenticationPrincipalArgumentResolver =
					authenticationPrincipalArgumentResolver.getObject();
			this.csrfTokenArgumentResolver = csrfTokenArgumentResolver.getObject();
		}

		public void addArgumentResolvers(
				List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.add(this.authenticationPrincipalArgumentResolver);
			argumentResolvers.add(this.csrfTokenArgumentResolver);
		}

	}

}
