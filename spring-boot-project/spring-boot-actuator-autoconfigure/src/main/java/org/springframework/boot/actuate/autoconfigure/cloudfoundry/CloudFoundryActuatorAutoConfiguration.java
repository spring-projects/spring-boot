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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.util.Arrays;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointProvider;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.WebEndpointOperation;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to expose actuator endpoints for
 * cloud foundry to use.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "management.cloudfoundry", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(ServletManagementContextAutoConfiguration.class)
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class CloudFoundryActuatorAutoConfiguration {

	/**
	 * Configuration for MVC endpoints on Cloud Foundry.
	 */
	@Configuration
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	@ConditionalOnClass(DispatcherServlet.class)
	@ConditionalOnBean(DispatcherServlet.class)
	static class MvcWebEndpointConfiguration {

		@Bean
		public CloudFoundryWebEndpointServletHandlerMapping cloudFoundryWebEndpointServletHandlerMapping(
				EndpointProvider<WebEndpointOperation> provider, Environment environment,
				RestTemplateBuilder builder) {
			return new CloudFoundryWebEndpointServletHandlerMapping(
					new EndpointMapping("/cloudfoundryapplication"),
					provider.getEndpoints(), getCorsConfiguration(),
					getSecurityInterceptor(builder, environment));
		}

		private CloudFoundrySecurityInterceptor getSecurityInterceptor(
				RestTemplateBuilder restTemplateBuilder, Environment environment) {
			CloudFoundrySecurityService cloudfoundrySecurityService = getCloudFoundrySecurityService(
					restTemplateBuilder, environment);
			TokenValidator tokenValidator = new TokenValidator(
					cloudfoundrySecurityService);
			return new CloudFoundrySecurityInterceptor(tokenValidator,
					cloudfoundrySecurityService,
					environment.getProperty("vcap.application.application_id"));
		}

		private CloudFoundrySecurityService getCloudFoundrySecurityService(
				RestTemplateBuilder restTemplateBuilder, Environment environment) {
			String cloudControllerUrl = environment
					.getProperty("vcap.application.cf_api");
			boolean skipSslValidation = environment.getProperty(
					"management.cloudfoundry.skip-ssl-validation", Boolean.class, false);
			return (cloudControllerUrl == null ? null
					: new CloudFoundrySecurityService(restTemplateBuilder,
							cloudControllerUrl, skipSslValidation));
		}

		private CorsConfiguration getCorsConfiguration() {
			CorsConfiguration corsConfiguration = new CorsConfiguration();
			corsConfiguration.addAllowedOrigin(CorsConfiguration.ALL);
			corsConfiguration.setAllowedMethods(
					Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
			corsConfiguration.setAllowedHeaders(
					Arrays.asList("Authorization", "X-Cf-App-Instance", "Content-Type"));
			return corsConfiguration;
		}

	}

	/**
	 * {@link WebSecurityConfigurer} to tell Spring Security to ignore cloudfoundry
	 * specific paths. The Cloud foundry endpoints are protected by their own security
	 * interceptor.
	 */
	@ConditionalOnClass(WebSecurity.class)
	@Order(SecurityProperties.IGNORED_ORDER)
	@Configuration
	public static class IgnoredPathsWebSecurityConfigurer
			implements WebSecurityConfigurer<WebSecurity> {

		@Override
		public void init(WebSecurity builder) throws Exception {
			builder.ignoring().requestMatchers(
					new AntPathRequestMatcher("/cloudfoundryapplication/**"));
		}

		@Override
		public void configure(WebSecurity builder) throws Exception {

		}

	}

}
