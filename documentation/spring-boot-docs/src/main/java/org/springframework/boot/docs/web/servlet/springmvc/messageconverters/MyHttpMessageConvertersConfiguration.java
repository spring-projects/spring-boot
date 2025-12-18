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

package org.springframework.boot.docs.web.servlet.springmvc.messageconverters;

import java.text.SimpleDateFormat;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

@Configuration(proxyBeanMethods = false)
public class MyHttpMessageConvertersConfiguration {

	@Bean
	public ClientHttpMessageConvertersCustomizer myClientConvertersCustomizer() {
		return (clientBuilder) -> clientBuilder.addCustomConverter(new AdditionalHttpMessageConverter())
			.addCustomConverter(new AnotherHttpMessageConverter());
	}

	@Bean
	public JacksonConverterCustomizer jacksonConverterCustomizer() {
		JsonMapper jsonMapper = JsonMapper.builder().defaultDateFormat(new SimpleDateFormat("yyyy-MM")).build();
		return new JacksonConverterCustomizer(jsonMapper);
	}

	// contribute a custom JSON converter to both client and server
	static class JacksonConverterCustomizer
			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

		private final JsonMapper jsonMapper;

		JacksonConverterCustomizer(JsonMapper jsonMapper) {
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

}
