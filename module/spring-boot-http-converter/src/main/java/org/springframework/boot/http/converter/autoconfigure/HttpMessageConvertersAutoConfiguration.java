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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration.NotReactiveWebApplicationCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpMessageConverter}s.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Piotr Maj
 * @author Oliver Gierke
 * @author David Liu
 * @author Andy Wilkinson
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Dmitry Sulman
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(afterName = { "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
		"org.springframework.boot.jsonb.autoconfigure.JsonbAutoConfiguration",
		"org.springframework.boot.gson.autoconfigure.GsonAutoConfiguration",
		"org.springframework.boot.kotlin.serialization.autoconfigure.KotlinSerializationAutoConfiguration" })
@ConditionalOnClass(HttpMessageConverter.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@Import({ JacksonHttpMessageConvertersConfiguration.class, GsonHttpMessageConvertersConfiguration.class,
		JsonbHttpMessageConvertersConfiguration.class, KotlinSerializationHttpMessageConvertersConfiguration.class })
public final class HttpMessageConvertersAutoConfiguration {

	static final String PREFERRED_MAPPER_PROPERTY = "spring.http.converters.preferred-json-mapper";

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	@SuppressWarnings("deprecation")
	ClientHttpMessageConvertersCustomizer clientConvertersCustomizer(
			ObjectProvider<HttpMessageConverters> legacyConverters,
			ObjectProvider<HttpMessageConverter<?>> converters) {
		return new DefaultClientHttpMessageConvertersCustomizer(legacyConverters.getIfAvailable(),
				converters.orderedStream().toList());
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	@SuppressWarnings("deprecation")
	ServerHttpMessageConvertersCustomizer serverConvertersCustomizer(
			ObjectProvider<HttpMessageConverters> legacyConverters,
			ObjectProvider<HttpMessageConverter<?>> converters) {
		return new DefaultServerHttpMessageConvertersCustomizer(legacyConverters.getIfAvailable(),
				converters.orderedStream().toList());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(StringHttpMessageConverter.class)
	@EnableConfigurationProperties(HttpMessageConvertersProperties.class)
	protected static class StringHttpMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		StringHttpMessageConverter stringHttpMessageConverter(HttpMessageConvertersProperties properties) {
			StringHttpMessageConverter converter = new StringHttpMessageConverter(
					properties.getStringEncodingCharset());
			converter.setWriteAcceptCharset(false);
			return converter;
		}

	}

	static class NotReactiveWebApplicationCondition extends NoneNestedConditions {

		NotReactiveWebApplicationCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnWebApplication(type = Type.REACTIVE)
		private static final class ReactiveWebApplication {

		}

	}

}
