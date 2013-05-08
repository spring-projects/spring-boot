/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.autoconfigure.service;

import java.util.List;

import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.context.annotation.EnableConfigurationProperties;
import org.springframework.bootstrap.service.properties.EndpointsProperties;
import org.springframework.bootstrap.service.properties.ManagementServerProperties;
import org.springframework.bootstrap.service.properties.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for service apps.
 * 
 * @author Dave Syer
 */
@Configuration
@Import({ ManagementConfiguration.class, MetricConfiguration.class,
		ServerConfiguration.class, SecurityConfiguration.class,
		MetricFilterConfiguration.class, AuditConfiguration.class })
public class ServiceAutoConfiguration extends WebMvcConfigurationSupport {

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		addDefaultHttpMessageConverters(converters);
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
				jacksonConverter.getObjectMapper().disable(
						SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			}
		}
	}

	/*
	 * ServerProperties has to be declared in a non-conditional bean, so that it gets
	 * added to the context early enough
	 */
	@EnableConfigurationProperties({ ServerProperties.class,
			ManagementServerProperties.class })
	public static class ServerPropertiesConfiguration {

		@Bean
		@ConditionalOnMissingBean(EndpointsProperties.class)
		public EndpointsProperties endpointsProperties() {
			return new EndpointsProperties();
		}
	}

}
