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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import org.springframework.boot.actuate.autoconfigure.endpoint.ExposeExcludePropertyEndpointFilter;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.ServletEndpointRegistrar;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link ManagementContextConfiguration} for servlet endpoints.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 *
 * @since 2.0.0
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class ServletEndpointManagementContextConfiguration {

	@Bean
	public ServletEndpointRegistrar servletEndpointRegistrar(
			WebEndpointProperties properties,
			ServletEndpointsSupplier servletEndpointsSupplier) {
		return new ServletEndpointRegistrar(properties.getBasePath(),
				servletEndpointsSupplier.getEndpoints());
	}

	@Bean
	public ExposeExcludePropertyEndpointFilter<ExposableServletEndpoint> servletExposeExcludePropertyEndpointFilter(
			WebEndpointProperties properties) {
		WebEndpointProperties.Exposure exposure = properties.getExposure();
		return new ExposeExcludePropertyEndpointFilter<>(ExposableServletEndpoint.class,
				exposure.getInclude(), exposure.getExclude());
	}

}
