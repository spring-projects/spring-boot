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

import java.util.Collection;

import javax.management.MBeanServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.ExposeExcludePropertyEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.jmx.EndpointMBeanRegistrar;
import org.springframework.boot.actuate.endpoint.jmx.EndpointObjectNameFactory;
import org.springframework.boot.actuate.endpoint.jmx.JmxOperation;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxAnnotationEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "management.endpoints.jmx.enabled", matchIfMissing = true)
public class JmxEndpointAutoConfiguration {

	private final ApplicationContext applicationContext;

	private final JmxEndpointProperties properties;

	public JmxEndpointAutoConfiguration(ApplicationContext applicationContext,
			JmxEndpointProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	@Bean
	public JmxAnnotationEndpointDiscoverer jmxAnnotationEndpointDiscoverer(
			ParameterMapper parameterMapper,
			Collection<OperationMethodInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<JmxOperation>> filters) {
		return new JmxAnnotationEndpointDiscoverer(this.applicationContext,
				parameterMapper, invokerAdvisors, filters);
	}

	@Bean
	@ConditionalOnSingleCandidate(MBeanServer.class)
	public JmxEndpointExporter jmxMBeanExporter(
			JmxAnnotationEndpointDiscoverer jmxAnnotationEndpointDiscoverer,
			MBeanServer mBeanServer, JmxAnnotationEndpointDiscoverer endpointDiscoverer,
			ObjectProvider<ObjectMapper> objectMapper) {
		EndpointObjectNameFactory objectNameFactory = new DefaultEndpointObjectNameFactory(
				this.properties, mBeanServer,
				ObjectUtils.getIdentityHexString(this.applicationContext));
		EndpointMBeanRegistrar registrar = new EndpointMBeanRegistrar(mBeanServer,
				objectNameFactory);
		return new JmxEndpointExporter(jmxAnnotationEndpointDiscoverer, registrar,
				objectMapper.getIfAvailable(ObjectMapper::new));
	}

	@Bean
	public ExposeExcludePropertyEndpointFilter<JmxOperation> jmxIncludeExcludePropertyEndpointFilter() {
		return new ExposeExcludePropertyEndpointFilter<>(
				JmxAnnotationEndpointDiscoverer.class, this.properties.getExpose(),
				this.properties.getExclude(), "*");
	}

}
