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
import org.springframework.boot.http.converter.autoconfigure.JacksonHttpMessageConvertersConfiguration.JacksonJsonHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configuration for HTTP message converters that use Jackson 2.
 *
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of Jackson 3.
 */
@Configuration(proxyBeanMethods = false)
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
class Jackson2HttpMessageConvertersConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ObjectMapper.class)
	@ConditionalOnBean(ObjectMapper.class)
	@Conditional(PreferJackson2OrJacksonUnavailableCondition.class)
	static class MappingJackson2HttpMessageConverterConfiguration {

		@Bean
		@Order(0)
		@ConditionalOnMissingBean(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class)
		Jackson2JsonMessageConvertersCustomizer jackson2HttpMessageConvertersCustomizer(ObjectMapper objectMapper) {
			return new Jackson2JsonMessageConvertersCustomizer(objectMapper);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(XmlMapper.class)
	@ConditionalOnBean(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.class)
	protected static class MappingJackson2XmlHttpMessageConverterConfiguration {

		@Bean
		@Order(0)
		@ConditionalOnMissingBean(org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter.class)
		Jackson2XmlMessageConvertersCustomizer mappingJackson2XmlHttpMessageConverter(
				Jackson2ObjectMapperBuilder builder) {
			return new Jackson2XmlMessageConvertersCustomizer(builder.createXmlMapper(true).build());
		}

	}

	static class Jackson2JsonMessageConvertersCustomizer
			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

		private final ObjectMapper objectMapper;

		Jackson2JsonMessageConvertersCustomizer(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public void customize(ClientBuilder builder) {
			builder.withJsonConverter(
					new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(this.objectMapper));
		}

		@Override
		public void customize(ServerBuilder builder) {
			builder.withJsonConverter(
					new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(this.objectMapper));
		}

	}

	static class Jackson2XmlMessageConvertersCustomizer
			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

		private final ObjectMapper objectMapper;

		Jackson2XmlMessageConvertersCustomizer(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public void customize(ClientBuilder builder) {
			builder.withXmlConverter(new org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter(
					this.objectMapper));
		}

		@Override
		public void customize(ServerBuilder builder) {
			builder.withXmlConverter(new org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter(
					this.objectMapper));
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

		@ConditionalOnMissingBean(JacksonJsonHttpMessageConvertersCustomizer.class)
		static class JacksonUnavailable {

		}

	}

}
