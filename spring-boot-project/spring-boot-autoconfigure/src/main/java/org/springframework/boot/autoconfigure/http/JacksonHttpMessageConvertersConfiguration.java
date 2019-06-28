/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

/**
 * Configuration for HTTP message converters that use Jackson.
 *
 * @author Andy Wilkinson
 * @since 1.2.2
 */
@Configuration(proxyBeanMethods = false)
class JacksonHttpMessageConvertersConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	@ConditionalOnBean(ObjectMapper.class)
	@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
			havingValue = "jackson", matchIfMissing = true)
	protected static class MappingJackson2HttpMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = MappingJackson2HttpMessageConverter.class,
				ignoredType = {
						"org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter",
						"org.springframework.data.rest.webmvc.alps.AlpsJsonHttpMessageConverter" })
		public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
			return new MappingJackson2HttpMessageConverter(objectMapper);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(XmlMapper.class)
	@ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
	protected static class MappingJackson2XmlHttpMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter(
				Jackson2ObjectMapperBuilder builder) {
			return new MappingJackson2XmlHttpMessageConverter(builder.createXmlMapper(true).build());
		}

	}

}
