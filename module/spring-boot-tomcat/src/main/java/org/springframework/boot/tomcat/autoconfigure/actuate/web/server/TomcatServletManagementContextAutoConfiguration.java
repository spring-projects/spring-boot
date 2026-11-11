/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure.actuate.web.server;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextFactory;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.tomcat.autoconfigure.TomcatWebServerConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for a Tomcat-based servlet management context.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(ManagementContextFactory.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
public final class TomcatServletManagementContextAutoConfiguration {

	@Bean
	static ManagementContextFactory servletWebChildContextFactory() {
		return new ManagementContextFactory(WebApplicationType.SERVLET, ServletWebServerFactory.class,
				TomcatWebServerConfiguration.class, TomcatServletWebServerAutoConfiguration.class);
	}

}
