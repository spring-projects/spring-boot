/*
 * Copyright 2012-2017 the original author or authors.
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
 * @since 1.2.2
 */
@Configuration
@ConditionalOnClass(Gson.class)
class GsonHttpMessageConvertersConfiguration {

	@Configuration
	@ConditionalOnBean(Gson.class)
	@Conditional(PreferGsonOrJacksonAndJsonbUnavailableCondition.class)
	protected static class GsonHttpMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public GsonHttpMessageConverter gsonHttpMessageConverter(Gson gson) {
			GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
			converter.setGson(gson);
			return converter;
		}

	}

	private static class PreferGsonOrJacksonAndJsonbUnavailableCondition
			extends AnyNestedCondition {

		PreferGsonOrJacksonAndJsonbUnavailableCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY, havingValue = "gson")
		static class GsonPreferred {

		}

		@Conditional(JacksonAndJsonbUnavailable.class)
		static class JacksonJsonbUnavailable {

		}

	}

	private static class JacksonAndJsonbUnavailable extends NoneNestedConditions {

		JacksonAndJsonbUnavailable() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(MappingJackson2HttpMessageConverter.class)
		static class JacksonAvailable {

		}

		@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY, havingValue = "jsonb")
		static class JsonbPreferred {

		}

	}

}
