/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
