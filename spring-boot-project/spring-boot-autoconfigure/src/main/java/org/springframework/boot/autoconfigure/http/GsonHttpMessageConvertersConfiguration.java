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

import com.google.gson.Gson;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Configuration for HTTP Message converters that use Gson.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Gson.class)
class GsonHttpMessageConvertersConfiguration {

	/**
	 * GsonHttpMessageConverterConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(Gson.class)
	@Conditional(PreferGsonOrJacksonAndJsonbUnavailableCondition.class)
	static class GsonHttpMessageConverterConfiguration {

		/**
		 * Creates a new instance of GsonHttpMessageConverter if no other bean of the same
		 * type is present.
		 * @param gson the Gson object to be used by the converter
		 * @return the created GsonHttpMessageConverter object
		 */
		@Bean
		@ConditionalOnMissingBean
		GsonHttpMessageConverter gsonHttpMessageConverter(Gson gson) {
			GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
			converter.setGson(gson);
			return converter;
		}

	}

	/**
	 * PreferGsonOrJacksonAndJsonbUnavailableCondition class.
	 */
	private static class PreferGsonOrJacksonAndJsonbUnavailableCondition extends AnyNestedCondition {

		/**
		 * Constructor for the PreferGsonOrJacksonAndJsonbUnavailableCondition class.
		 * @param phase the configuration phase for registering the bean
		 */
		PreferGsonOrJacksonAndJsonbUnavailableCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		/**
		 * GsonPreferred class.
		 */
		@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
				havingValue = "gson")
		static class GsonPreferred {

		}

		/**
		 * JacksonJsonbUnavailable class.
		 */
		@Conditional(JacksonAndJsonbUnavailableCondition.class)
		static class JacksonJsonbUnavailable {

		}

	}

	/**
	 * JacksonAndJsonbUnavailableCondition class.
	 */
	private static class JacksonAndJsonbUnavailableCondition extends NoneNestedConditions {

		/**
		 * Constructor for JacksonAndJsonbUnavailableCondition.
		 * @param phase the configuration phase
		 */
		JacksonAndJsonbUnavailableCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		/**
		 * JacksonAvailable class.
		 */
		@ConditionalOnBean(MappingJackson2HttpMessageConverter.class)
		static class JacksonAvailable {

		}

		/**
		 * JsonbPreferred class.
		 */
		@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
				havingValue = "jsonb")
		static class JsonbPreferred {

		}

	}

}
