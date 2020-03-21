/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.boot.actuate.autoconfigure.web.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
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

	@Bean
	ResourceConfig resourceConfig() {
		return new ResourceConfig();
	}

}
