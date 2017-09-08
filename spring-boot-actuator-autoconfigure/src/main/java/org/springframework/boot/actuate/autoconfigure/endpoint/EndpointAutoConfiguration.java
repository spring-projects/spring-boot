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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.actuate.endpoint.EndpointExposure;
import org.springframework.boot.actuate.endpoint.OperationParameterMapper;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.cache.CachingConfigurationFactory;
import org.springframework.boot.actuate.endpoint.convert.ConversionServiceOperationParameterMapper;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.WebEndpointOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link Endpoint} support.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
@Configuration
public class EndpointAutoConfiguration {

	@Bean
	public OperationParameterMapper operationParameterMapper() {
		return new ConversionServiceOperationParameterMapper();
	}

	@Bean
	@ConditionalOnMissingBean(CachingConfigurationFactory.class)
	public DefaultCachingConfigurationFactory cacheConfigurationFactory(
			Environment environment) {
		return new DefaultCachingConfigurationFactory(environment);
	}

	@Configuration
	@ConditionalOnWebApplication
	static class EndpointWebConfiguration {

		private static final List<String> MEDIA_TYPES = Arrays
				.asList(ActuatorMediaType.V2_JSON_VALUE, "application/json");

		private final ApplicationContext applicationContext;

		EndpointWebConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		public EndpointProvider<WebEndpointOperation> webEndpointProvider(
				OperationParameterMapper parameterMapper,
				DefaultCachingConfigurationFactory cachingConfigurationFactory) {
			Environment environment = this.applicationContext.getEnvironment();
			WebAnnotationEndpointDiscoverer endpointDiscoverer = new WebAnnotationEndpointDiscoverer(
					this.applicationContext, parameterMapper, cachingConfigurationFactory,
					MEDIA_TYPES, MEDIA_TYPES);
			return new EndpointProvider<>(environment, endpointDiscoverer,
					EndpointExposure.WEB);
		}

	}

}
