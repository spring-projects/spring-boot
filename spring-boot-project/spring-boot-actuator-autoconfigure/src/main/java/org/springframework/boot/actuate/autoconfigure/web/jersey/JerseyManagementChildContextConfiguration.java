/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link ManagementContextConfiguration} for Jersey infrastructure when a separate
 * management context with a web server running on a different port is required.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
@ManagementContextConfiguration(ManagementContextType.CHILD)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(ResourceConfig.class)
@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
public class JerseyManagementChildContextConfiguration {

	private final ObjectProvider<ResourceConfigCustomizer> resourceConfigCustomizers;

	public JerseyManagementChildContextConfiguration(
			ObjectProvider<ResourceConfigCustomizer> resourceConfigCustomizers) {
		this.resourceConfigCustomizers = resourceConfigCustomizers;
	}

	@Bean
	public ServletRegistrationBean<ServletContainer> jerseyServletRegistration() {
		return new ServletRegistrationBean<>(
				new ServletContainer(endpointResourceConfig()), "/*");
	}

	@Bean
	public ResourceConfig endpointResourceConfig() {
		ResourceConfig resourceConfig = new ResourceConfig();
		this.resourceConfigCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(resourceConfig));
		return resourceConfig;
	}

}
