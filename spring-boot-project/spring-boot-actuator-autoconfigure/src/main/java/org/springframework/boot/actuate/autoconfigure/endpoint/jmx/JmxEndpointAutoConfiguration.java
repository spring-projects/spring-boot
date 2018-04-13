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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import java.util.Collection;
import java.util.Collections;

import javax.management.MBeanServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.ExposeExcludePropertyEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.jmx.EndpointObjectNameFactory;
import org.springframework.boot.actuate.endpoint.jmx.ExposableJmxEndpoint;
import org.springframework.boot.actuate.endpoint.jmx.JacksonJmxOperationResponseMapper;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointExporter;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperationResponseMapper;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpointDiscoverer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ObjectUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JMX {@link Endpoint} support.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
@AutoConfigureAfter(JmxAutoConfiguration.class)
@EnableConfigurationProperties(JmxEndpointProperties.class)
public class JmxEndpointAutoConfiguration {

	private final ApplicationContext applicationContext;

	private final JmxEndpointProperties properties;

	public JmxEndpointAutoConfiguration(ApplicationContext applicationContext,
			JmxEndpointProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(JmxEndpointsSupplier.class)
	public JmxEndpointDiscoverer jmxAnnotationEndpointDiscoverer(
			ParameterValueMapper parameterValueMapper,
			ObjectProvider<Collection<OperationInvokerAdvisor>> invokerAdvisors,
			ObjectProvider<Collection<EndpointFilter<ExposableJmxEndpoint>>> filters) {
		return new JmxEndpointDiscoverer(this.applicationContext, parameterValueMapper,
				invokerAdvisors.getIfAvailable(Collections::emptyList),
				filters.getIfAvailable(Collections::emptyList));
	}

	@Bean
	@ConditionalOnSingleCandidate(MBeanServer.class)
	public JmxEndpointExporter jmxMBeanExporter(MBeanServer mBeanServer,
			ObjectProvider<ObjectMapper> objectMapper,
			JmxEndpointsSupplier jmxEndpointsSupplier) {
		String contextId = ObjectUtils.getIdentityHexString(this.applicationContext);
		EndpointObjectNameFactory objectNameFactory = new DefaultEndpointObjectNameFactory(
				this.properties, mBeanServer, contextId);
		JmxOperationResponseMapper responseMapper = new JacksonJmxOperationResponseMapper(
				objectMapper.getIfAvailable());
		return new JmxEndpointExporter(mBeanServer, objectNameFactory, responseMapper,
				jmxEndpointsSupplier.getEndpoints());

	}

	@Bean
	public ExposeExcludePropertyEndpointFilter<ExposableJmxEndpoint> jmxIncludeExcludePropertyEndpointFilter() {
		JmxEndpointProperties.Exposure exposure = this.properties.getExposure();
		return new ExposeExcludePropertyEndpointFilter<>(ExposableJmxEndpoint.class,
				exposure.getInclude(), exposure.getExclude(), "*");
	}

}
