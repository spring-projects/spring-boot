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

package org.springframework.boot.docs.web.servlet.springmvc.messageconverters

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import tools.jackson.databind.json.JsonMapper
import java.text.SimpleDateFormat

@Configuration(proxyBeanMethods = false)
class MyHttpMessageConvertersConfiguration {

	@Bean
	fun myClientConvertersCustomizer(): ClientHttpMessageConvertersCustomizer {
		return ClientHttpMessageConvertersCustomizer { clientBuilder: HttpMessageConverters.ClientBuilder ->
			clientBuilder
				.addCustomConverter(AdditionalHttpMessageConverter())
				.addCustomConverter(AnotherHttpMessageConverter())
		}
	}

	@Bean
	fun jacksonConverterCustomizer(): JacksonConverterCustomizer {
		val jsonMapper = JsonMapper.builder()
			.defaultDateFormat(SimpleDateFormat("yyyy-MM"))
			.build()
		return JacksonConverterCustomizer(jsonMapper)
	}

	// contribute a custom JSON converter to both client and server
	class JacksonConverterCustomizer(private val jsonMapper: JsonMapper) :
		ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

		override fun customize(builder: HttpMessageConverters.ClientBuilder) {
			builder.withJsonConverter(JacksonJsonHttpMessageConverter(this.jsonMapper))
		}

		override fun customize(builder: HttpMessageConverters.ServerBuilder) {
			builder.withJsonConverter(JacksonJsonHttpMessageConverter(this.jsonMapper))
		}
	}

}

