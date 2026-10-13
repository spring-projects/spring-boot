/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.converter.autoconfigure;

import jakarta.json.bind.Jsonb;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.converter.autoconfigure.GsonHttpMessageConvertersConfiguration.GsonHttpConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.JacksonHttpMessageConvertersConfiguration.JacksonJsonHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;

/**
 * Configuration for HTTP Message converters that use JSON-B.
 *
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Jsonb.class)
class JsonbHttpMessageConvertersConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(Jsonb.class)
	@Conditional(PreferJsonbOrMissingJacksonAndGsonCondition.class)
	static class JsonbHttpMessageConverterConfiguration {

		@Bean
		@Order(0)
		@ConditionalOnMissingBean(JsonbHttpMessageConverter.class)
		JsonbHttpMessageConvertersCustomizer jsonbHttpMessageConvertersCustomizer(Jsonb jsonb) {
			return new JsonbHttpMessageConvertersCustomizer(jsonb);
		}

	}

	static class JsonbHttpMessageConvertersCustomizer
			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

		private final JsonbHttpMessageConverter converter;

		JsonbHttpMessageConvertersCustomizer(Jsonb jsonb) {
			this.converter = new JsonbHttpMessageConverter(jsonb);
		}

		@Override
		public void customize(ClientBuilder builder) {
			builder.withJsonConverter(this.converter);
		}

		@Override
		public void customize(ServerBuilder builder) {
			builder.withJsonConverter(this.converter);
		}

	}

	private static class PreferJsonbOrMissingJacksonAndGsonCondition extends AnyNestedCondition {

		PreferJsonbOrMissingJacksonAndGsonCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
				havingValue = "jsonb")
		static class JsonbPreferred {

		}

		@SuppressWarnings("removal")
		@ConditionalOnMissingBean({ JacksonJsonHttpMessageConvertersCustomizer.class,
				Jackson2HttpMessageConvertersConfiguration.Jackson2JsonMessageConvertersCustomizer.class,
				GsonHttpConvertersCustomizer.class })
		static class JacksonAndGsonMissing {

		}

	}

}
