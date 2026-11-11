/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure.actuate.web.server;

import org.apache.catalina.startup.Tomcat;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Tomcat-based
 * servlet web endpoint infrastructure when a separate management context running on a
 * different port is required.
 *
 * @author Andy Wilkinson
 */
@ConditionalOnClass(Tomcat.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(TomcatManagementServerProperties.class)
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
class TomcatServletManagementChildContextConfiguration {

	@Bean
	TomcatAccessLogCustomizer<TomcatServletWebServerFactory> tomcatManagementAccessLogCustomizer(
			TomcatManagementServerProperties properties) {
		return new TomcatAccessLogCustomizer<>(properties, TomcatServletWebServerFactory::getEngineValves);
	}

}
