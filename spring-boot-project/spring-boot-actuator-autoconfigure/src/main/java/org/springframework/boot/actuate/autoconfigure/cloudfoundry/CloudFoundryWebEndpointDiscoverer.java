/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.util.Collection;
import java.util.List;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryWebEndpointDiscoverer.CloudFoundryWebEndpointDiscovererRuntimeHints;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * {@link WebEndpointDiscoverer} for Cloud Foundry that uses Cloud Foundry specific
 * extensions for the {@link HealthEndpoint}.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@ImportRuntimeHints(CloudFoundryWebEndpointDiscovererRuntimeHints.class)
public class CloudFoundryWebEndpointDiscoverer extends WebEndpointDiscoverer {

	/**
	 * Create a new {@link WebEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param parameterValueMapper the parameter value mapper
	 * @param endpointMediaTypes the endpoint media types
	 * @param endpointPathMappers the endpoint path mappers
	 * @param invokerAdvisors invoker advisors to apply
	 * @param filters filters to apply
	 */
	public CloudFoundryWebEndpointDiscoverer(ApplicationContext applicationContext,
			ParameterValueMapper parameterValueMapper, EndpointMediaTypes endpointMediaTypes,
			List<PathMapper> endpointPathMappers, Collection<OperationInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<ExposableWebEndpoint>> filters) {
		super(applicationContext, parameterValueMapper, endpointMediaTypes, endpointPathMappers, invokerAdvisors,
				filters);
	}

	/**
     * Determines if the given extension bean type should be exposed as an extension type.
     * Regular health endpoint extensions are filtered out so that a Cloud Foundry version can replace them.
     * 
     * @param extensionBeanType the extension bean type to check
     * @return true if the extension type should be exposed, false otherwise
     */
    @Override
	protected boolean isExtensionTypeExposed(Class<?> extensionBeanType) {
		// Filter regular health endpoint extensions so a CF version can replace them
		return !isHealthEndpointExtension(extensionBeanType)
				|| isCloudFoundryHealthEndpointExtension(extensionBeanType);
	}

	/**
     * Checks if the given extension bean type is a health endpoint extension.
     * 
     * @param extensionBeanType the extension bean type to check
     * @return true if the extension bean type is a health endpoint extension, false otherwise
     */
    private boolean isHealthEndpointExtension(Class<?> extensionBeanType) {
		return MergedAnnotations.from(extensionBeanType)
			.get(EndpointWebExtension.class)
			.getValue("endpoint", Class.class)
			.map(HealthEndpoint.class::isAssignableFrom)
			.orElse(false);
	}

	/**
     * Checks if the given extension bean type is a Cloud Foundry health endpoint extension.
     * 
     * @param extensionBeanType the class of the extension bean
     * @return {@code true} if the extension bean type is a Cloud Foundry health endpoint extension, {@code false} otherwise
     */
    private boolean isCloudFoundryHealthEndpointExtension(Class<?> extensionBeanType) {
		return MergedAnnotations.from(extensionBeanType).isPresent(EndpointCloudFoundryExtension.class);
	}

	/**
     * CloudFoundryWebEndpointDiscovererRuntimeHints class.
     */
    static class CloudFoundryWebEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {

		/**
         * Registers hints for the CloudFoundryWebEndpointDiscovererRuntimeHints class.
         * 
         * @param hints The RuntimeHints object containing the hints to be registered.
         * @param classLoader The ClassLoader to be used for reflection.
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection()
				.registerType(CloudFoundryEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
