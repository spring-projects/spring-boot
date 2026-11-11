/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure.actuate.web;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jersey.autoconfigure.DefaultJerseyApplicationPath;
import org.springframework.boot.jersey.autoconfigure.JerseyApplicationPath;
import org.springframework.boot.jersey.autoconfigure.JerseyProperties;
import org.springframework.boot.jersey.autoconfigure.ResourceConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Jersey
 * infrastructure when the management context is the same as the main application context.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@ManagementContextConfiguration(value = ManagementContextType.SAME, proxyBeanMethods = false)
@EnableConfigurationProperties(JerseyProperties.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ JerseyApplicationPath.class, ResourceConfig.class })
@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
public class JerseySameManagementContextConfiguration {

	@Bean
	ResourceConfigCustomizer managementResourceConfigCustomizerAdapter(
			ObjectProvider<ManagementContextResourceConfigCustomizer> customizers) {
		return (config) -> customizers.orderedStream().forEach((customizer) -> customizer.customize(config));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(JerseyManagementContextConfiguration.class)
	@ConditionalOnMissingBean(ResourceConfig.class)
	static class JerseyInfrastructureConfiguration {

		@Bean
		@ConditionalOnMissingBean
		JerseyApplicationPath jerseyApplicationPath(JerseyProperties properties, ResourceConfig config) {
			return new DefaultJerseyApplicationPath(properties.getApplicationPath(), config);
		}

		@Bean
		ResourceConfig resourceConfig(ObjectProvider<ResourceConfigCustomizer> resourceConfigCustomizers) {
			ResourceConfig resourceConfig = new ResourceConfig();
			resourceConfigCustomizers.orderedStream().forEach((customizer) -> customizer.customize(resourceConfig));
			return resourceConfig;
		}

	}

}
