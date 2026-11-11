/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.integration.autoconfigure;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.integration.actuate.endpoint.IntegrationGraphEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.config.IntegrationConfigurationBeanFactoryPostProcessor;
import org.springframework.integration.graph.IntegrationGraphServer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * {@link IntegrationGraphEndpoint}.
 *
 * @author Tim Ysewyn
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = IntegrationAutoConfiguration.class)
@ConditionalOnClass({ IntegrationGraphServer.class, IntegrationGraphEndpoint.class,
		ConditionalOnAvailableEndpoint.class })
@ConditionalOnBean(IntegrationConfigurationBeanFactoryPostProcessor.class)
@ConditionalOnAvailableEndpoint(IntegrationGraphEndpoint.class)
public final class IntegrationGraphEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	IntegrationGraphEndpoint integrationGraphEndpoint(IntegrationGraphServer integrationGraphServer) {
		return new IntegrationGraphEndpoint(integrationGraphServer);
	}

	@Bean
	@ConditionalOnMissingBean
	IntegrationGraphServer integrationGraphServer() {
		return new IntegrationGraphServer();
	}

}
