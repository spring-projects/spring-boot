/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Configuration for HTTP message converters that use Jackson
 *
 * @author Andy Wilkinson
 * @since 1.2.2
 */
@Configuration
class JacksonHttpMessageConvertersConfiguration {

	@Configuration
	@ConditionalOnClass(ObjectMapper.class)
	@ConditionalOnBean(ObjectMapper.class)
	@EnableConfigurationProperties(HttpMapperProperties.class)
	@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY, havingValue = "jackson", matchIfMissing = true)
	@SuppressWarnings("deprecation")
	protected static class MappingJackson2HttpMessageConverterConfiguration {

		// This can be removed when the deprecated class is removed (the ObjectMapper will
		// already have all the correct properties).
		@Autowired
		private HttpMapperProperties properties = new HttpMapperProperties();

		@Bean
		@ConditionalOnMissingBean
		public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(
				ObjectMapper objectMapper) {
			MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(
					objectMapper);
			if (this.properties.isJsonPrettyPrint() != null) {
				converter.setPrettyPrint(this.properties.isJsonPrettyPrint());
			}
			return converter;
		}

	}

	@Configuration
	@ConditionalOnClass(XmlMapper.class)
	@ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
	@EnableConfigurationProperties(HttpMapperProperties.class)
	@SuppressWarnings("deprecation")
	protected static class MappingJackson2XmlHttpMessageConverterConfiguration {

		@Autowired
		private HttpMapperProperties properties = new HttpMapperProperties();

		@Bean
		@ConditionalOnMissingBean
		public MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter(
				Jackson2ObjectMapperBuilder builder) {
			MappingJackson2XmlHttpMessageConverter converter = new MappingJackson2XmlHttpMessageConverter();
			converter.setObjectMapper(builder.createXmlMapper(true).build());
			if (this.properties.isJsonPrettyPrint() != null) {
				converter.setPrettyPrint(this.properties.isJsonPrettyPrint());
			}
			return converter;
		}

	}

}
