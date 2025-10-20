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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import javax.management.MBeanServer;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.jmx.EndpointObjectNameFactory;
import org.springframework.boot.actuate.endpoint.jmx.ExposableJmxEndpoint;
import org.springframework.boot.actuate.endpoint.jmx.JacksonJmxOperationResponseMapper;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointExporter;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperationResponseMapper;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpointDiscoverer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ObjectUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JMX {@link Endpoint @Endpoint}
 * support.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
@AutoConfiguration(after = { JmxAutoConfiguration.class, EndpointAutoConfiguration.class })
@EnableConfigurationProperties({ JmxEndpointProperties.class, JmxProperties.class })
@ConditionalOnBooleanProperty("spring.jmx.enabled")
public final class JmxEndpointAutoConfiguration {

	private final ApplicationContext applicationContext;

	private final JmxEndpointProperties properties;

	private final JmxProperties jmxProperties;

	JmxEndpointAutoConfiguration(ApplicationContext applicationContext, JmxEndpointProperties properties,
			JmxProperties jmxProperties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
		this.jmxProperties = jmxProperties;
	}

	@Bean
	@ConditionalOnMissingBean(JmxEndpointsSupplier.class)
	JmxEndpointDiscoverer jmxAnnotationEndpointDiscoverer(ParameterValueMapper parameterValueMapper,
			ObjectProvider<OperationInvokerAdvisor> invokerAdvisors,
			ObjectProvider<EndpointFilter<ExposableJmxEndpoint>> endpointFilters,
			ObjectProvider<OperationFilter<JmxOperation>> operationFilters) {
		return new JmxEndpointDiscoverer(this.applicationContext, parameterValueMapper,
				invokerAdvisors.orderedStream().toList(), endpointFilters.orderedStream().toList(),
				operationFilters.orderedStream().toList());
	}

	@Bean
	@ConditionalOnMissingBean(value = EndpointObjectNameFactory.class, search = SearchStrategy.CURRENT)
	DefaultEndpointObjectNameFactory endpointObjectNameFactory(MBeanServer mBeanServer) {
		String contextId = ObjectUtils.getIdentityHexString(this.applicationContext);
		return new DefaultEndpointObjectNameFactory(this.properties, this.jmxProperties, mBeanServer, contextId);
	}

	@Bean
	@ConditionalOnSingleCandidate(MBeanServer.class)
	@ConditionalOnClass(JsonMapper.class)
	JmxEndpointExporter jmxMBeanExporter(MBeanServer mBeanServer, EndpointObjectNameFactory endpointObjectNameFactory,
			ObjectProvider<JsonMapper> jsonMapper, JmxEndpointsSupplier jmxEndpointsSupplier) {
		JmxOperationResponseMapper responseMapper = new JacksonJmxOperationResponseMapper(jsonMapper.getIfAvailable());
		return new JmxEndpointExporter(mBeanServer, endpointObjectNameFactory, responseMapper,
				jmxEndpointsSupplier.getEndpoints());
	}

	// FIXME

	@Bean
	IncludeExcludeEndpointFilter<ExposableJmxEndpoint> jmxIncludeExcludePropertyEndpointFilter() {
		JmxEndpointProperties.Exposure exposure = this.properties.getExposure();
		return new IncludeExcludeEndpointFilter<>(ExposableJmxEndpoint.class, exposure.getInclude(),
				exposure.getExclude(), EndpointExposure.JMX.getDefaultIncludes());
	}

	@Bean
	static LazyInitializationExcludeFilter eagerlyInitializeJmxEndpointExporter() {
		return LazyInitializationExcludeFilter.forBeanTypes(JmxEndpointExporter.class);
	}

	@Bean
	OperationFilter<JmxOperation> jmxAccessPropertiesOperationFilter(EndpointAccessResolver endpointAccessResolver) {
		return OperationFilter.byAccess(endpointAccessResolver);
	}

}
