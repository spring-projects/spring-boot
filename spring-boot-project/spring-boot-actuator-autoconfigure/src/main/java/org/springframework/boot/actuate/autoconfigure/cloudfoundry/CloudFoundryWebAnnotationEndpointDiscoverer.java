/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.ApplicationContext;

/**
 * {@link WebAnnotationEndpointDiscoverer} for Cloud Foundry that uses Cloud Foundry
 * specific extensions for the {@link HealthEndpoint}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryWebAnnotationEndpointDiscoverer
		extends WebAnnotationEndpointDiscoverer {

	private final Class<?> requiredExtensionType;

	public CloudFoundryWebAnnotationEndpointDiscoverer(
			ApplicationContext applicationContext, ParameterMapper parameterMapper,
			EndpointMediaTypes endpointMediaTypes,
			EndpointPathResolver endpointPathResolver,
			Collection<? extends OperationMethodInvokerAdvisor> invokerAdvisors,
			Collection<? extends EndpointFilter<WebOperation>> filters,
			Class<?> requiredExtensionType) {
		super(applicationContext, parameterMapper, endpointMediaTypes,
				endpointPathResolver, invokerAdvisors, filters);
		this.requiredExtensionType = requiredExtensionType;
	}

	@Override
	protected boolean isExtensionExposed(Class<?> endpointType, Class<?> extensionType,
			EndpointInfo<WebOperation> endpointInfo) {
		if (HealthEndpoint.class.equals(endpointType)
				&& !this.requiredExtensionType.equals(extensionType)) {
			return false;
		}
		return super.isExtensionExposed(endpointType, extensionType, endpointInfo);
	}

}
