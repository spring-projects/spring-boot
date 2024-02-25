/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration.HttpMessageConvertersAutoConfigurationRuntimeHints;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration.NotReactiveWebApplicationCondition;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.servlet.server.Encoding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;
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
 * @since 2.0.0
 */
@AutoConfiguration(
		after = { GsonAutoConfiguration.class, JacksonAutoConfiguration.class, JsonbAutoConfiguration.class })
@ConditionalOnClass(HttpMessageConverter.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@Import({ JacksonHttpMessageConvertersConfiguration.class, GsonHttpMessageConvertersConfiguration.class,
		JsonbHttpMessageConvertersConfiguration.class })
@ImportRuntimeHints(HttpMessageConvertersAutoConfigurationRuntimeHints.class)
public class HttpMessageConvertersAutoConfiguration {

	static final String PREFERRED_MAPPER_PROPERTY = "spring.mvc.converters.preferred-json-mapper";

	/**
     * Creates a new instance of {@link HttpMessageConverters} by resolving and ordering the available {@link HttpMessageConverter}s.
     * If no custom {@link HttpMessageConverter}s are provided, the default ones will be used.
     * 
     * @param converters the object provider for {@link HttpMessageConverter}s
     * @return a new instance of {@link HttpMessageConverters} with the resolved and ordered {@link HttpMessageConverter}s
     */
    @Bean
	@ConditionalOnMissingBean
	public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
		return new HttpMessageConverters(converters.orderedStream().toList());
	}

	/**
     * StringHttpMessageConverterConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(StringHttpMessageConverter.class)
	protected static class StringHttpMessageConverterConfiguration {

		/**
         * Creates a new instance of {@link StringHttpMessageConverter} if no other bean of the same type is present.
         * 
         * @param environment the environment object used to retrieve the server servlet encoding configuration
         * @return the created {@link StringHttpMessageConverter} instance
         */
        @Bean
		@ConditionalOnMissingBean
		public StringHttpMessageConverter stringHttpMessageConverter(Environment environment) {
			Encoding encoding = Binder.get(environment).bindOrCreate("server.servlet.encoding", Encoding.class);
			StringHttpMessageConverter converter = new StringHttpMessageConverter(encoding.getCharset());
			converter.setWriteAcceptCharset(false);
			return converter;
		}

	}

	/**
     * NotReactiveWebApplicationCondition class.
     */
    static class NotReactiveWebApplicationCondition extends NoneNestedConditions {

		/**
         * Constructs a new instance of the NotReactiveWebApplicationCondition class.
         * 
         * This constructor calls the super constructor with the ConfigurationPhase.PARSE_CONFIGURATION argument.
         */
        NotReactiveWebApplicationCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		/**
         * ReactiveWebApplication class.
         */
        @ConditionalOnWebApplication(type = Type.REACTIVE)
		private static final class ReactiveWebApplication {

		}

	}

	/**
     * HttpMessageConvertersAutoConfigurationRuntimeHints class.
     */
    static class HttpMessageConvertersAutoConfigurationRuntimeHints extends BindableRuntimeHintsRegistrar {

		/**
         * Constructs a new HttpMessageConvertersAutoConfigurationRuntimeHints object.
         * 
         * @param encoding the encoding to use for the HttpMessageConvertersAutoConfigurationRuntimeHints object
         */
        HttpMessageConvertersAutoConfigurationRuntimeHints() {
			super(Encoding.class);
		}

	}

}
