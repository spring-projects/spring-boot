/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure.actuate.web;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.jersey.autoconfigure.JerseyApplicationPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Jersey
 * infrastructure when a separate management context with a web server running on a
 * different port is required.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@Import(JerseyManagementContextConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ JerseyApplicationPath.class, ResourceConfig.class })
@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
public class JerseyChildManagementContextConfiguration {

	@Bean
	public JerseyApplicationPath jerseyApplicationPath() {
		return () -> "/";
	}

	@Bean
	ResourceConfig resourceConfig(ObjectProvider<ManagementContextResourceConfigCustomizer> customizers) {
		ResourceConfig resourceConfig = new ResourceConfig();
		customizers.orderedStream().forEach((customizer) -> customizer.customize(resourceConfig));
		return resourceConfig;
	}

}
