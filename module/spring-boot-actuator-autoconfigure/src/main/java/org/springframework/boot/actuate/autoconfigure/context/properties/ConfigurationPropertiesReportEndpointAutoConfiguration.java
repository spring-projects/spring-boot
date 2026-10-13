/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.context.properties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpointWebExtension;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * {@link ConfigurationPropertiesReportEndpoint}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(ConfigurationPropertiesReportEndpoint.class)
@EnableConfigurationProperties(ConfigurationPropertiesReportEndpointProperties.class)
public final class ConfigurationPropertiesReportEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint(
			ConfigurationPropertiesReportEndpointProperties properties,
			ObjectProvider<SanitizingFunction> sanitizingFunctions) {
		return new ConfigurationPropertiesReportEndpoint(sanitizingFunctions.orderedStream().toList(),
				properties.getShowValues());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(ConfigurationPropertiesReportEndpoint.class)
	@ConditionalOnAvailableEndpoint(exposure = EndpointExposure.WEB)
	ConfigurationPropertiesReportEndpointWebExtension configurationPropertiesReportEndpointWebExtension(
			ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint,
			ConfigurationPropertiesReportEndpointProperties properties) {
		return new ConfigurationPropertiesReportEndpointWebExtension(configurationPropertiesReportEndpoint,
				properties.getShowValues(), properties.getRoles());
	}

}
