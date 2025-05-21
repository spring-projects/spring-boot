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

package org.springframework.boot.webmvc.actuate.autoconfigure.endpoint.web;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
class WebMvcEndpointManagementContextConfiguration {

	@Bean
	@SuppressWarnings({ "deprecation", "removal" })
	org.springframework.boot.actuate.endpoint.web.ServletEndpointRegistrar servletEndpointRegistrar(
			WebEndpointProperties properties,
			org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier servletEndpointsSupplier,
			DispatcherServletPath dispatcherServletPath, EndpointAccessResolver endpointAccessResolver) {
		return new org.springframework.boot.actuate.endpoint.web.ServletEndpointRegistrar(
				dispatcherServletPath.getRelativePath(properties.getBasePath()),
				servletEndpointsSupplier.getEndpoints(), endpointAccessResolver);
	}

}
