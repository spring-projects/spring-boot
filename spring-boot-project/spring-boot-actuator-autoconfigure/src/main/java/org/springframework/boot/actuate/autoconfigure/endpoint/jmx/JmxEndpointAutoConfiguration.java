/*
 * Copyright 2012-2022 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true")
public class JmxEndpointAutoConfiguration {

	private final ApplicationContext applicationContext;

	private final JmxEndpointProperties properties;

	private final JmxProperties jmxProperties;

	/**
	 * Constructs a new JmxEndpointAutoConfiguration with the specified
	 * ApplicationContext, JmxEndpointProperties, and JmxProperties.
	 * @param applicationContext the ApplicationContext used for dependency injection
	 * @param properties the JmxEndpointProperties used for configuring JMX endpoints
	 * @param jmxProperties the JmxProperties used for configuring JMX settings
	 */
	public JmxEndpointAutoConfiguration(ApplicationContext applicationContext, JmxEndpointProperties properties,
			JmxProperties jmxProperties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
		this.jmxProperties = jmxProperties;
	}

	/**
	 * Creates a JmxEndpointDiscoverer bean if no other bean of type JmxEndpointsSupplier
	 * is present. This bean is responsible for discovering JMX endpoints annotated
	 * with @JmxEndpoint.
	 * @param parameterValueMapper The ParameterValueMapper used for mapping method
	 * parameters.
	 * @param invokerAdvisors The OperationInvokerAdvisor objects used for advising the
	 * operation invokers.
	 * @param filters The EndpointFilter objects used for filtering the discovered JMX
	 * endpoints.
	 * @return The created JmxEndpointDiscoverer bean.
	 */
	@Bean
	@ConditionalOnMissingBean(JmxEndpointsSupplier.class)
	public JmxEndpointDiscoverer jmxAnnotationEndpointDiscoverer(ParameterValueMapper parameterValueMapper,
			ObjectProvider<OperationInvokerAdvisor> invokerAdvisors,
			ObjectProvider<EndpointFilter<ExposableJmxEndpoint>> filters) {
		return new JmxEndpointDiscoverer(this.applicationContext, parameterValueMapper,
				invokerAdvisors.orderedStream().toList(), filters.orderedStream().toList());
	}

	/**
	 * Creates a new instance of {@link DefaultEndpointObjectNameFactory} if no bean of
	 * type {@link EndpointObjectNameFactory} is already present in the application
	 * context.
	 *
	 * The creation of the bean is conditional on the absence of a bean of type
	 * {@link EndpointObjectNameFactory} and the specified search strategy.
	 *
	 * The created instance of {@link DefaultEndpointObjectNameFactory} uses the provided
	 * {@link MBeanServer} and the context ID generated from the application context.
	 * @param mBeanServer the {@link MBeanServer} to be used by the
	 * {@link DefaultEndpointObjectNameFactory}
	 * @return a new instance of {@link DefaultEndpointObjectNameFactory} if no bean of
	 * type {@link EndpointObjectNameFactory} is already present in the application
	 * context
	 */
	@Bean
	@ConditionalOnMissingBean(value = EndpointObjectNameFactory.class, search = SearchStrategy.CURRENT)
	public DefaultEndpointObjectNameFactory endpointObjectNameFactory(MBeanServer mBeanServer) {
		String contextId = ObjectUtils.getIdentityHexString(this.applicationContext);
		return new DefaultEndpointObjectNameFactory(this.properties, this.jmxProperties, mBeanServer, contextId);
	}

	/**
	 * Creates a JmxEndpointExporter bean if there is a single candidate for MBeanServer.
	 * @param mBeanServer The MBeanServer instance.
	 * @param endpointObjectNameFactory The EndpointObjectNameFactory instance.
	 * @param objectMapper The ObjectMapper instance.
	 * @param jmxEndpointsSupplier The JmxEndpointsSupplier instance.
	 * @return The JmxEndpointExporter bean.
	 */
	@Bean
	@ConditionalOnSingleCandidate(MBeanServer.class)
	public JmxEndpointExporter jmxMBeanExporter(MBeanServer mBeanServer,
			EndpointObjectNameFactory endpointObjectNameFactory, ObjectProvider<ObjectMapper> objectMapper,
			JmxEndpointsSupplier jmxEndpointsSupplier) {
		JmxOperationResponseMapper responseMapper = new JacksonJmxOperationResponseMapper(
				objectMapper.getIfAvailable());
		return new JmxEndpointExporter(mBeanServer, endpointObjectNameFactory, responseMapper,
				jmxEndpointsSupplier.getEndpoints());

	}

	/**
	 * Creates a new instance of {@link IncludeExcludeEndpointFilter} for filtering JMX
	 * endpoints based on inclusion and exclusion patterns.
	 * @return the created {@link IncludeExcludeEndpointFilter} instance
	 */
	@Bean
	public IncludeExcludeEndpointFilter<ExposableJmxEndpoint> jmxIncludeExcludePropertyEndpointFilter() {
		JmxEndpointProperties.Exposure exposure = this.properties.getExposure();
		return new IncludeExcludeEndpointFilter<>(ExposableJmxEndpoint.class, exposure.getInclude(),
				exposure.getExclude(), EndpointExposure.JMX.getDefaultIncludes());
	}

	/**
	 * Returns a LazyInitializationExcludeFilter for eagerly initializing the
	 * JmxEndpointExporter bean. This filter excludes the JmxEndpointExporter class from
	 * lazy initialization.
	 * @return the LazyInitializationExcludeFilter for eagerly initializing the
	 * JmxEndpointExporter bean
	 */
	@Bean
	static LazyInitializationExcludeFilter eagerlyInitializeJmxEndpointExporter() {
		return LazyInitializationExcludeFilter.forBeanTypes(JmxEndpointExporter.class);
	}

}
