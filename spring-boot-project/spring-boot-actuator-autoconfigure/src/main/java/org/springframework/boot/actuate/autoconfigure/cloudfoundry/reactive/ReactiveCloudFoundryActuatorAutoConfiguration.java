/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryWebEndpointDiscoverer;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.MatcherSecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to expose actuator endpoints for
 * Cloud Foundry to use in a reactive environment.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "management.cloudfoundry", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(HealthEndpointAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class ReactiveCloudFoundryActuatorAutoConfiguration {

	private final ApplicationContext applicationContext;

	ReactiveCloudFoundryActuatorAutoConfiguration(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	@ConditionalOnBean({ HealthEndpoint.class, ReactiveHealthEndpointWebExtension.class })
	public CloudFoundryReactiveHealthEndpointWebExtension cloudFoundryReactiveHealthEndpointWebExtension(
			ReactiveHealthEndpointWebExtension reactiveHealthEndpointWebExtension) {
		return new CloudFoundryReactiveHealthEndpointWebExtension(
				reactiveHealthEndpointWebExtension);
	}

	@Bean
	public CloudFoundryWebFluxEndpointHandlerMapping cloudFoundryWebFluxEndpointHandlerMapping(
			ParameterValueMapper parameterMapper, EndpointMediaTypes endpointMediaTypes,
			WebClient.Builder webClientBuilder,
			ControllerEndpointsSupplier controllerEndpointsSupplier) {
		CloudFoundryWebEndpointDiscoverer endpointDiscoverer = new CloudFoundryWebEndpointDiscoverer(
				this.applicationContext, parameterMapper, endpointMediaTypes,
				PathMapper.useEndpointId(), Collections.emptyList(),
				Collections.emptyList());
		CloudFoundrySecurityInterceptor securityInterceptor = getSecurityInterceptor(
				webClientBuilder, this.applicationContext.getEnvironment());
		Collection<ExposableWebEndpoint> webEndpoints = endpointDiscoverer.getEndpoints();
		List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
		allEndpoints.addAll(webEndpoints);
		allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
		return new CloudFoundryWebFluxEndpointHandlerMapping(
				new EndpointMapping("/cloudfoundryapplication"), webEndpoints,
				endpointMediaTypes, getCorsConfiguration(), securityInterceptor,
				new EndpointLinksResolver(allEndpoints));
	}

	private CloudFoundrySecurityInterceptor getSecurityInterceptor(
			WebClient.Builder webClientBuilder, Environment environment) {
		ReactiveCloudFoundrySecurityService cloudfoundrySecurityService = getCloudFoundrySecurityService(
				webClientBuilder, environment);
		ReactiveTokenValidator tokenValidator = new ReactiveTokenValidator(
				cloudfoundrySecurityService);
		return new CloudFoundrySecurityInterceptor(tokenValidator,
				cloudfoundrySecurityService,
				environment.getProperty("vcap.application.application_id"));
	}

	private ReactiveCloudFoundrySecurityService getCloudFoundrySecurityService(
			WebClient.Builder webClientBuilder, Environment environment) {
		String cloudControllerUrl = environment.getProperty("vcap.application.cf_api");
		boolean skipSslValidation = environment.getProperty(
				"management.cloudfoundry.skip-ssl-validation", Boolean.class, false);
		return (cloudControllerUrl == null ? null
				: new ReactiveCloudFoundrySecurityService(webClientBuilder,
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

	@Configuration
	@ConditionalOnClass(MatcherSecurityWebFilterChain.class)
	static class IgnoredPathsSecurityConfiguration {

		@Bean
		public WebFilterChainPostProcessor webFilterChainPostProcessor() {
			return new WebFilterChainPostProcessor();
		}

	}

	private static class WebFilterChainPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof WebFilterChainProxy) {
				return postProcess((WebFilterChainProxy) bean);
			}
			return bean;
		}

		private WebFilterChainProxy postProcess(WebFilterChainProxy existing) {
			ServerWebExchangeMatcher cloudFoundryRequestMatcher = ServerWebExchangeMatchers
					.pathMatchers("/cloudfoundryapplication/**");
			WebFilter noOpFilter = (exchange, chain) -> chain.filter(exchange);
			MatcherSecurityWebFilterChain ignoredRequestFilterChain = new MatcherSecurityWebFilterChain(
					cloudFoundryRequestMatcher, Collections.singletonList(noOpFilter));
			MatcherSecurityWebFilterChain allRequestsFilterChain = new MatcherSecurityWebFilterChain(
					ServerWebExchangeMatchers.anyExchange(),
					Collections.singletonList(existing));
			return new WebFilterChainProxy(ignoredRequestFilterChain,
					allRequestsFilterChain);
		}

	}

}
