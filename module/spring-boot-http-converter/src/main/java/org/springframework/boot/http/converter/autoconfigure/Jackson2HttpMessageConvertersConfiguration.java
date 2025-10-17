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

import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

/**
 * Configuration for HTTP message converters that use Jackson 2.
 *
 * @author Andy Wilkinson
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of Jackson 3.
 */
@Configuration(proxyBeanMethods = false)
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
class Jackson2HttpMessageConvertersConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	@ConditionalOnBean(ObjectMapper.class)
	@Conditional(PreferJackson2OrJacksonUnavailableCondition.class)
	static class MappingJackson2HttpMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		org.springframework.http.converter.json.MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(
				ObjectMapper objectMapper) {
			return new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(XmlMapper.class)
	@ConditionalOnBean(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.class)
	protected static class MappingJackson2XmlHttpMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter(
				Jackson2ObjectMapperBuilder builder) {
			return new MappingJackson2XmlHttpMessageConverter(builder.createXmlMapper(true).build());
		}

	}

	private static class PreferJackson2OrJacksonUnavailableCondition extends AnyNestedCondition {

		PreferJackson2OrJacksonUnavailableCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
				havingValue = "jackson2")
		static class Jackson2Preferred {

		}

		@ConditionalOnMissingBean(JacksonJsonHttpMessageConverter.class)
		static class JacksonUnavailable {

		}

	}

}
