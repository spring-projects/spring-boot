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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.endpoint.web.EndpointMapping;
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
 * cloud foundry to use in a reactive environment.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "management.cloudfoundry", name = "enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class ReactiveCloudFoundryActuatorAutoConfiguration {

	private final ApplicationContext applicationContext;

	ReactiveCloudFoundryActuatorAutoConfiguration(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	public CloudFoundryWebFluxEndpointHandlerMapping cloudFoundryWebFluxEndpointHandlerMapping(
			ParameterMapper parameterMapper, EndpointMediaTypes endpointMediaTypes,
			WebClient.Builder webClientBuilder) {
		WebAnnotationEndpointDiscoverer endpointDiscoverer = new WebAnnotationEndpointDiscoverer(
				this.applicationContext, parameterMapper, endpointMediaTypes,
				EndpointPathResolver.useEndpointId(), null, null);
		ReactiveCloudFoundrySecurityInterceptor securityInterceptor = getSecurityInterceptor(
				webClientBuilder, this.applicationContext.getEnvironment());
		return new CloudFoundryWebFluxEndpointHandlerMapping(
				new EndpointMapping("/cloudfoundryapplication"),
				endpointDiscoverer.discoverEndpoints(), endpointMediaTypes,
				getCorsConfiguration(), securityInterceptor);
	}

	private ReactiveCloudFoundrySecurityInterceptor getSecurityInterceptor(
			WebClient.Builder restTemplateBuilder, Environment environment) {
		ReactiveCloudFoundrySecurityService cloudfoundrySecurityService = getCloudFoundrySecurityService(
				restTemplateBuilder, environment);
		ReactiveTokenValidator tokenValidator = new ReactiveTokenValidator(
				cloudfoundrySecurityService);
		return new ReactiveCloudFoundrySecurityInterceptor(tokenValidator,
				cloudfoundrySecurityService,
				environment.getProperty("vcap.application.application_id"));
	}

	private ReactiveCloudFoundrySecurityService getCloudFoundrySecurityService(
			WebClient.Builder webClientBuilder, Environment environment) {
		String cloudControllerUrl = environment.getProperty("vcap.application.cf_api");
		return (cloudControllerUrl == null ? null
				: new ReactiveCloudFoundrySecurityService(webClientBuilder,
						cloudControllerUrl));
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
