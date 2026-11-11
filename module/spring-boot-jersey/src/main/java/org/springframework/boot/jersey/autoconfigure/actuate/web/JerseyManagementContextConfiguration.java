/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure.actuate.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import org.springframework.boot.jersey.autoconfigure.JerseyApplicationPath;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared configuration for Jersey-based actuators regardless of management context type.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
class JerseyManagementContextConfiguration {

	@Bean
	ServletRegistrationBean<ServletContainer> jerseyServletRegistration(JerseyApplicationPath jerseyApplicationPath,
			ResourceConfig resourceConfig) {
		return new ServletRegistrationBean<>(new ServletContainer(resourceConfig),
				jerseyApplicationPath.getUrlMapping());
	}

}
