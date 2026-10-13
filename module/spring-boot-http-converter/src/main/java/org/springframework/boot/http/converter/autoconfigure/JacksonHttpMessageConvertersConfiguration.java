/*
 * Copyright 2012-present the original author or authors.
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
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;

/**
 * Configuration for HTTP message converters that use Jackson.
 *
 * @author Andy Wilkinson
 * @author Brian Clozel
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
		@Order(0)
		@ConditionalOnMissingBean(value = JacksonJsonHttpMessageConverter.class,
				ignoredType = { "org.springframework.hateoas.server.mvc.TypeConstrainedJacksonJsonHttpMessageConverter",
						"org.springframework.data.rest.webmvc.alps.AlpsJacksonJsonHttpMessageConverter" })
		JacksonJsonHttpMessageConvertersCustomizer jacksonJsonHttpMessageConvertersCustomizer(JsonMapper jsonMapper) {
			return new JacksonJsonHttpMessageConvertersCustomizer(jsonMapper);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(XmlMapper.class)
	@ConditionalOnBean(XmlMapper.class)
	protected static class JacksonXmlHttpMessageConverterConfiguration {

		@Bean
		@Order(0)
		@ConditionalOnMissingBean(JacksonXmlHttpMessageConverter.class)
		JacksonXmlHttpMessageConvertersCustomizer jacksonXmlHttpMessageConvertersCustomizer(XmlMapper xmlMapper) {
			return new JacksonXmlHttpMessageConvertersCustomizer(xmlMapper);
		}

	}

	static class JacksonJsonHttpMessageConvertersCustomizer
			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

		private final JsonMapper jsonMapper;

		JacksonJsonHttpMessageConvertersCustomizer(JsonMapper jsonMapper) {
			this.jsonMapper = jsonMapper;
		}

		@Override
		public void customize(ClientBuilder builder) {
			builder.withJsonConverter(new JacksonJsonHttpMessageConverter(this.jsonMapper));
		}

		@Override
		public void customize(ServerBuilder builder) {
			builder.withJsonConverter(new JacksonJsonHttpMessageConverter(this.jsonMapper));
		}

	}

	static class JacksonXmlHttpMessageConvertersCustomizer
			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

		private final XmlMapper xmlMapper;

		JacksonXmlHttpMessageConvertersCustomizer(XmlMapper xmlMapper) {
			this.xmlMapper = xmlMapper;
		}

		@Override
		public void customize(ClientBuilder builder) {
			builder.withXmlConverter(new JacksonXmlHttpMessageConverter(this.xmlMapper));
		}

		@Override
		public void customize(ServerBuilder builder) {
			builder.withXmlConverter(new JacksonXmlHttpMessageConverter(this.xmlMapper));
		}

	}

}
