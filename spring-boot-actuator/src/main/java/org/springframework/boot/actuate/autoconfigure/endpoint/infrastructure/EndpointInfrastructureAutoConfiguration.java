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

package org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.management.MBeanServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.endpoint.ConversionServiceOperationParameterMapper;
import org.springframework.boot.endpoint.EndpointExposure;
import org.springframework.boot.endpoint.OperationParameterMapper;
import org.springframework.boot.endpoint.jmx.EndpointMBeanRegistrar;
import org.springframework.boot.endpoint.jmx.JmxAnnotationEndpointDiscoverer;
import org.springframework.boot.endpoint.jmx.JmxEndpointOperation;
import org.springframework.boot.endpoint.web.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.endpoint.web.WebEndpointOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the endpoint infrastructure used
 * by the Actuator.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@AutoConfigureAfter(JmxAutoConfiguration.class)
@EnableConfigurationProperties(JmxEndpointExporterProperties.class)
public class EndpointInfrastructureAutoConfiguration {

	private final ApplicationContext applicationContext;

	public EndpointInfrastructureAutoConfiguration(
			ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	public OperationParameterMapper operationParameterMapper() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(String.class, Date.class, this::convertToDate);
		return new ConversionServiceOperationParameterMapper(conversionService);
	}

	private Date convertToDate(String value) {
		if (StringUtils.hasLength(value)) {
			OffsetDateTime offsetDateTime = OffsetDateTime.parse(value,
					DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			return new Date(offsetDateTime.toEpochSecond() * 1000);
		}
		return null;

	}

	@Bean
	public CachingConfigurationFactory cacheConfigurationFactory() {
		return new CachingConfigurationFactory(this.applicationContext.getEnvironment());
	}

	@Bean
	public JmxAnnotationEndpointDiscoverer jmxEndpointDiscoverer(
			OperationParameterMapper operationParameterMapper,
			CachingConfigurationFactory cachingConfigurationFactory) {
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

	@Configuration
	@ConditionalOnWebApplication
	static class WebInfrastructureConfiguration {

		private final ApplicationContext applicationContext;

		WebInfrastructureConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		public EndpointProvider<WebEndpointOperation> webEndpointProvider(
				OperationParameterMapper operationParameterMapper,
				CachingConfigurationFactory cachingConfigurationFactory) {
			return new EndpointProvider<>(this.applicationContext.getEnvironment(),
					webEndpointDiscoverer(operationParameterMapper,
							cachingConfigurationFactory),
					EndpointExposure.WEB);
		}

		private WebAnnotationEndpointDiscoverer webEndpointDiscoverer(
				OperationParameterMapper operationParameterMapper,
				CachingConfigurationFactory cachingConfigurationFactory) {
			List<String> mediaTypes = Arrays.asList(
					ActuatorMediaTypes.APPLICATION_ACTUATOR_V2_JSON_VALUE,
					"application/json");
			return new WebAnnotationEndpointDiscoverer(this.applicationContext,
					operationParameterMapper, cachingConfigurationFactory, mediaTypes,
					mediaTypes);
		}

	}

}
