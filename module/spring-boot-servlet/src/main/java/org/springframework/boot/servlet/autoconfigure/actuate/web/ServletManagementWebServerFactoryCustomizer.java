/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.autoconfigure.actuate.web;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementWebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.util.StringUtils;

/**
 * {@link ManagementWebServerFactoryCustomizer} for a servlet web server.
 *
 * @author Andy Wilkinson
 */
class ServletManagementWebServerFactoryCustomizer
		extends ManagementWebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

	ServletManagementWebServerFactoryCustomizer(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	@Override
	protected void customize(ConfigurableServletWebServerFactory webServerFactory,
			ManagementServerProperties managementServerProperties, ServerProperties serverProperties) {
		super.customize(webServerFactory, managementServerProperties, serverProperties);
		webServerFactory.setContextPath(getContextPath(managementServerProperties));
	}

	private String getContextPath(ManagementServerProperties managementServerProperties) {
		String basePath = managementServerProperties.getBasePath();
		return StringUtils.hasText(basePath) ? basePath : "";
	}

}
