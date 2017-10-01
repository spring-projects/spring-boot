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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import javax.management.MBeanServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.DefaultCachingConfigurationFactory;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointProvider;
import org.springframework.boot.actuate.endpoint.EndpointExposure;
import org.springframework.boot.actuate.endpoint.OperationParameterMapper;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.jmx.EndpointMBeanRegistrar;
import org.springframework.boot.actuate.endpoint.jmx.JmxEndpointOperation;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxAnnotationEndpointDiscoverer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
@EnableConfigurationProperties(JmxEndpointExporterProperties.class)
public class JmxEndpointAutoConfiguration {

	private final ApplicationContext applicationContext;

	public JmxEndpointAutoConfiguration(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	public JmxAnnotationEndpointDiscoverer jmxEndpointDiscoverer(
			OperationParameterMapper operationParameterMapper,
			DefaultCachingConfigurationFactory cachingConfigurationFactory) {
		return new JmxAnnotationEndpointDiscoverer(this.applicationContext,
				operationParameterMapper, cachingConfigurationFactory);
	}

	@ConditionalOnSingleCandidate(MBeanServer.class)
	@Bean
	public JmxEndpointExporter jmxMBeanExporter(JmxEndpointExporterProperties properties,
			MBeanServer mBeanServer, JmxAnnotationEndpointDiscoverer endpointDiscoverer,
			ObjectProvider<ObjectMapper> objectMapper) {
		EndpointProvider<JmxEndpointOperation> endpointProvider = new EndpointProvider<>(
				this.applicationContext.getEnvironment(), endpointDiscoverer,
				EndpointExposure.JMX);
		EndpointMBeanRegistrar endpointMBeanRegistrar = new EndpointMBeanRegistrar(
				mBeanServer, new DefaultEndpointObjectNameFactory(properties, mBeanServer,
						ObjectUtils.getIdentityHexString(this.applicationContext)));
		return new JmxEndpointExporter(endpointProvider, endpointMBeanRegistrar,
				objectMapper.getIfAvailable(ObjectMapper::new));
	}

}
