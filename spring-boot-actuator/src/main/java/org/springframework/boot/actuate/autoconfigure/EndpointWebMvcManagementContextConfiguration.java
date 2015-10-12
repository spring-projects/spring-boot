/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties.Security;
import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer;
import org.springframework.boot.actuate.endpoint.mvc.EnvironmentMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.LogFileMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.actuate.endpoint.mvc.ShutdownMvcEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Configuration to expose {@link Endpoint} instances over Spring MVC.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@ManagementContextConfiguration
@EnableConfigurationProperties({ HealthMvcEndpointProperties.class,
		EndpointCorsProperties.class })
public class EndpointWebMvcManagementContextConfiguration {

	@Autowired
	private HealthMvcEndpointProperties healthMvcEndpointProperties;

	@Autowired
	private ManagementServerProperties managementServerProperties;

	@Autowired
	private EndpointCorsProperties corsProperties;

	@Autowired(required = false)
	private List<EndpointHandlerMappingCustomizer> mappingCustomizers;

	@Bean
	@ConditionalOnMissingBean
	public EndpointHandlerMapping endpointHandlerMapping() {
		Set<? extends MvcEndpoint> endpoints = mvcEndpoints().getEndpoints();
		CorsConfiguration corsConfiguration = getCorsConfiguration(this.corsProperties);
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(endpoints,
				corsConfiguration);
		boolean disabled = this.managementServerProperties.getPort() != null
				&& this.managementServerProperties.getPort() == -1;
		mapping.setDisabled(disabled);
		if (!disabled) {
			mapping.setPrefix(this.managementServerProperties.getContextPath());
		}
		if (this.mappingCustomizers != null) {
			for (EndpointHandlerMappingCustomizer customizer : this.mappingCustomizers) {
				customizer.customize(mapping);
			}
		}
		return mapping;
	}

	private CorsConfiguration getCorsConfiguration(EndpointCorsProperties properties) {
		if (CollectionUtils.isEmpty(properties.getAllowedOrigins())) {
			return null;
		}
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.getAllowedOrigins());
		if (!CollectionUtils.isEmpty(properties.getAllowedHeaders())) {
			configuration.setAllowedHeaders(properties.getAllowedHeaders());
		}
		if (!CollectionUtils.isEmpty(properties.getAllowedMethods())) {
			configuration.setAllowedMethods(properties.getAllowedMethods());
		}
		if (!CollectionUtils.isEmpty(properties.getExposedHeaders())) {
			configuration.setExposedHeaders(properties.getExposedHeaders());
		}
		if (properties.getMaxAge() != null) {
			configuration.setMaxAge(properties.getMaxAge());
		}
		if (properties.getAllowCredentials() != null) {
			configuration.setAllowCredentials(properties.getAllowCredentials());
		}
		return configuration;
	}

	@Bean
	@ConditionalOnMissingBean
	public MvcEndpoints mvcEndpoints() {
		return new MvcEndpoints();
	}

	@Bean
	@ConditionalOnBean(EnvironmentEndpoint.class)
	@ConditionalOnEnabledEndpoint("env")
	public EnvironmentMvcEndpoint environmentMvcEndpoint(EnvironmentEndpoint delegate) {
		return new EnvironmentMvcEndpoint(delegate);
	}

	@Bean
	@ConditionalOnBean(HealthEndpoint.class)
	@ConditionalOnEnabledEndpoint("health")
	public HealthMvcEndpoint healthMvcEndpoint(HealthEndpoint delegate) {
		Security security = this.managementServerProperties.getSecurity();
		boolean secure = (security != null && security.isEnabled());
		HealthMvcEndpoint healthMvcEndpoint = new HealthMvcEndpoint(delegate, secure);
		if (this.healthMvcEndpointProperties.getMapping() != null) {
			healthMvcEndpoint
					.addStatusMapping(this.healthMvcEndpointProperties.getMapping());
		}
		return healthMvcEndpoint;
	}

	@Bean
	@ConditionalOnBean(MetricsEndpoint.class)
	@ConditionalOnEnabledEndpoint("metrics")
	public MetricsMvcEndpoint metricsMvcEndpoint(MetricsEndpoint delegate) {
		return new MetricsMvcEndpoint(delegate);
	}

	@Bean
	@ConditionalOnEnabledEndpoint("logfile")
	@Conditional(LogFileCondition.class)
	public LogFileMvcEndpoint logfileMvcEndpoint() {
		return new LogFileMvcEndpoint();
	}

	@Bean
	@ConditionalOnBean(ShutdownEndpoint.class)
	@ConditionalOnEnabledEndpoint(value = "shutdown", enabledByDefault = false)
	public ShutdownMvcEndpoint shutdownMvcEndpoint(ShutdownEndpoint delegate) {
		return new ShutdownMvcEndpoint(delegate);
	}

	private static class LogFileCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			String config = environment.resolvePlaceholders("${logging.file:}");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match("Found logging.file: " + config);
			}
			config = environment.resolvePlaceholders("${logging.path:}");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match("Found logging.path: " + config);
			}
			return ConditionOutcome.noMatch("Found no log file configuration");
		}

	}

}
