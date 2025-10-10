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

package org.springframework.boot.http.converter.autoconfigure;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;

/**
 * Configuration for HTTP message converters that use Jackson.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
class JacksonHttpMessageConvertersConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JsonMapper.class)
	@ConditionalOnBean(JsonMapper.class)
	@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
			havingValue = "jackson", matchIfMissing = true)
	static class JacksonJsonHttpMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean(
				ignoredType = { "org.springframework.hateoas.server.mvc.TypeConstrainedJacksonJsonHttpMessageConverter",
						"org.springframework.data.rest.webmvc.alps.AlpsJacksonJsonHttpMessageConverter" })
		JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter(JsonMapper jsonMapper) {
			return new JacksonJsonHttpMessageConverter(jsonMapper);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(XmlMapper.class)
	@ConditionalOnBean(XmlMapper.class)
	protected static class JacksonXmlHttpMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public JacksonXmlHttpMessageConverter jacksonXmlHttpMessageConverter(XmlMapper xmlMapper) {
			return new JacksonXmlHttpMessageConverter(xmlMapper);
		}

	}

}
