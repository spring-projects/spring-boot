/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryWebEndpointDiscoverer;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.info.InfoPropertiesInfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to expose actuator endpoints for
 * Cloud Foundry to use.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@AutoConfiguration(after = { ServletManagementContextAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
		InfoEndpointAutoConfiguration.class })
@ConditionalOnBooleanProperty(name = "management.cloudfoundry.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBean(DispatcherServlet.class)
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class CloudFoundryActuatorAutoConfiguration {

	private static final String BASE_PATH = "/cloudfoundryapplication";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	@ConditionalOnBean({ HealthEndpoint.class, HealthEndpointWebExtension.class })
	public CloudFoundryHealthEndpointWebExtension cloudFoundryHealthEndpointWebExtension(
			HealthEndpointWebExtension healthEndpointWebExtension) {
		return new CloudFoundryHealthEndpointWebExtension(healthEndpointWebExtension);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	@ConditionalOnBean({ InfoEndpoint.class, GitProperties.class })
	public CloudFoundryInfoEndpointWebExtension cloudFoundryInfoEndpointWebExtension(GitProperties properties,
			ObjectProvider<InfoContributor> infoContributors) {
		List<InfoContributor> contributors = infoContributors.orderedStream()
			.map((infoContributor) -> (infoContributor instanceof GitInfoContributor)
					? new GitInfoContributor(properties, InfoPropertiesInfoContributor.Mode.FULL) : infoContributor)
			.toList();
		return new CloudFoundryInfoEndpointWebExtension(new InfoEndpoint(contributors));
	}

	@Bean
	@SuppressWarnings("removal")
	public CloudFoundryWebEndpointServletHandlerMapping cloudFoundryWebEndpointServletHandlerMapping(
			ParameterValueMapper parameterMapper, EndpointMediaTypes endpointMediaTypes,
			RestTemplateBuilder restTemplateBuilder,
			org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier servletEndpointsSupplier,
			org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier controllerEndpointsSupplier,
			ApplicationContext applicationContext) {
		CloudFoundryWebEndpointDiscoverer discoverer = new CloudFoundryWebEndpointDiscoverer(applicationContext,
				parameterMapper, endpointMediaTypes, null, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList());
		CloudFoundrySecurityInterceptor securityInterceptor = getSecurityInterceptor(restTemplateBuilder,
				applicationContext.getEnvironment());
		Collection<ExposableWebEndpoint> webEndpoints = discoverer.getEndpoints();
		List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
		allEndpoints.addAll(webEndpoints);
		allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
		allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
		return new CloudFoundryWebEndpointServletHandlerMapping(new EndpointMapping(BASE_PATH), webEndpoints,
				endpointMediaTypes, getCorsConfiguration(), securityInterceptor, allEndpoints);
	}

	private CloudFoundrySecurityInterceptor getSecurityInterceptor(RestTemplateBuilder restTemplateBuilder,
			Environment environment) {
		CloudFoundrySecurityService cloudfoundrySecurityService = getCloudFoundrySecurityService(restTemplateBuilder,
				environment);
		TokenValidator tokenValidator = new TokenValidator(cloudfoundrySecurityService);
		return new CloudFoundrySecurityInterceptor(tokenValidator, cloudfoundrySecurityService,
				environment.getProperty("vcap.application.application_id"));
	}

	private CloudFoundrySecurityService getCloudFoundrySecurityService(RestTemplateBuilder restTemplateBuilder,
			Environment environment) {
		String cloudControllerUrl = environment.getProperty("vcap.application.cf_api");
		boolean skipSslValidation = environment.getProperty("management.cloudfoundry.skip-ssl-validation",
				Boolean.class, false);
		return (cloudControllerUrl != null)
				? new CloudFoundrySecurityService(restTemplateBuilder, cloudControllerUrl, skipSslValidation) : null;
	}

	private CorsConfiguration getCorsConfiguration() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.addAllowedOrigin(CorsConfiguration.ALL);
		corsConfiguration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
		corsConfiguration
			.setAllowedHeaders(Arrays.asList(HttpHeaders.AUTHORIZATION, "X-Cf-App-Instance", HttpHeaders.CONTENT_TYPE));
		return corsConfiguration;
	}

	/**
	 * {@link WebSecurityConfigurer} to tell Spring Security to permit cloudfoundry
	 * specific paths. The Cloud foundry endpoints are protected by their own security
	 * interceptor.
	 */
	@ConditionalOnClass({ WebSecurityCustomizer.class, WebSecurity.class })
	@Configuration(proxyBeanMethods = false)
	public static class IgnoredCloudFoundryPathsWebSecurityConfiguration {

		private static final int FILTER_CHAIN_ORDER = -1;

		@Bean
		@Order(FILTER_CHAIN_ORDER)
		SecurityFilterChain cloudFoundrySecurityFilterChain(HttpSecurity http,
				CloudFoundryWebEndpointServletHandlerMapping handlerMapping) throws Exception {
			RequestMatcher cloudFoundryRequest = getRequestMatcher(handlerMapping);
			http.csrf((csrf) -> csrf.ignoringRequestMatchers(cloudFoundryRequest));
			http.securityMatchers((matches) -> matches.requestMatchers(cloudFoundryRequest))
				.authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll());
			return http.build();
		}

		private RequestMatcher getRequestMatcher(CloudFoundryWebEndpointServletHandlerMapping handlerMapping) {
			PathMappedEndpoints endpoints = new PathMappedEndpoints(BASE_PATH, handlerMapping::getAllEndpoints);
			List<RequestMatcher> matchers = new ArrayList<>();
			endpoints.getAllPaths().forEach((path) -> matchers.add(pathMatcher(path + "/**")));
			matchers.add(pathMatcher(BASE_PATH));
			matchers.add(pathMatcher(BASE_PATH + "/"));
			return new OrRequestMatcher(matchers);
		}

		private PathPatternRequestMatcher pathMatcher(String path) {
			return PathPatternRequestMatcher.withDefaults().matcher(path);
		}

	}

}
