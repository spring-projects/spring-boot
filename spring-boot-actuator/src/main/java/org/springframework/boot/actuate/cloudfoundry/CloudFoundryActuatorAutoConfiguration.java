/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.actuate.endpoint.mvc.NamedMvcEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.IgnoredRequestCustomizer;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to expose actuator endpoints for
 * cloud foundry to use.
 *
 * @author Madhura Bhave
 * @since 1.5.0
 */
@Configuration
@ConditionalOnProperty(prefix = "management.cloudfoundry", name = "enabled", matchIfMissing = true)
@ConditionalOnBean(MvcEndpoints.class)
@AutoConfigureAfter(EndpointWebMvcAutoConfiguration.class)
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class CloudFoundryActuatorAutoConfiguration {

	@Bean
	public CloudFoundryEndpointHandlerMapping cloudFoundryEndpointHandlerMapping(
			MvcEndpoints mvcEndpoints, RestTemplateBuilder restTemplateBuilder,
			Environment environment) {
		Set<NamedMvcEndpoint> endpoints = new LinkedHashSet<NamedMvcEndpoint>(
				mvcEndpoints.getEndpoints(NamedMvcEndpoint.class));
		HandlerInterceptor securityInterceptor = getSecurityInterceptor(
				restTemplateBuilder, environment);
		CorsConfiguration corsConfiguration = getCorsConfiguration();
		CloudFoundryEndpointHandlerMapping mapping = new CloudFoundryEndpointHandlerMapping(
				endpoints, corsConfiguration, securityInterceptor);
		mapping.setPrefix("/cloudfoundryapplication");
		return mapping;
	}

	private HandlerInterceptor getSecurityInterceptor(
			RestTemplateBuilder restTemplateBuilder, Environment environment) {
		CloudFoundrySecurityService cloudfoundrySecurityService = getCloudFoundrySecurityService(
				restTemplateBuilder, environment);
		TokenValidator tokenValidator = new TokenValidator(cloudfoundrySecurityService);
		HandlerInterceptor securityInterceptor = new CloudFoundrySecurityInterceptor(
				tokenValidator, cloudfoundrySecurityService,
				environment.getProperty("vcap.application.application_id"));
		return securityInterceptor;
	}

	private CloudFoundrySecurityService getCloudFoundrySecurityService(
			RestTemplateBuilder restTemplateBuilder, Environment environment) {
		RelaxedPropertyResolver cloudFoundryProperties = new RelaxedPropertyResolver(
				environment, "management.cloudfoundry.");
		String cloudControllerUrl = environment.getProperty("vcap.application.cf_api");
		boolean skipSslValidation = cloudFoundryProperties
				.getProperty("skip-ssl-validation", Boolean.class, false);
		return (cloudControllerUrl != null) ? new CloudFoundrySecurityService(
				restTemplateBuilder, cloudControllerUrl, skipSslValidation) : null;
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

	/**
	 * Nested configuration for ignored requests if Spring Security is present.
	 */
	@Configuration
	@ConditionalOnClass(WebSecurity.class)
	static class CloudFoundryIgnoredRequestConfiguration {

		@Bean
		public IgnoredRequestCustomizer cloudFoundryIgnoredRequestCustomizer() {
			return new CloudFoundryIgnoredRequestCustomizer();
		}

		private static class CloudFoundryIgnoredRequestCustomizer
				implements IgnoredRequestCustomizer {

			@Override
			public void customize(WebSecurity.IgnoredRequestConfigurer configurer) {
				configurer.requestMatchers(
						new AntPathRequestMatcher("/cloudfoundryapplication/**"));
			}

		}

	}

}
