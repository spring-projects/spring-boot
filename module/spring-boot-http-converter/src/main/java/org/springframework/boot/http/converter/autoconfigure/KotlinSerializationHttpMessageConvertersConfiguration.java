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

import kotlinx.serialization.Serializable;
import kotlinx.serialization.json.Json;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;

/**
 * Configuration for HTTP message converters that use Kotlin Serialization.
 *
 * @author Brian Clozel
 * @author Dmitry Sulman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Serializable.class, Json.class })
@ConditionalOnBean(Json.class)
class KotlinSerializationHttpMessageConvertersConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Order(-10) // configured ahead of JSON mappers
	KotlinSerializationJsonHttpMessageConverter kotlinSerializationJsonHttpMessageConverter(Json json) {
		return new KotlinSerializationJsonHttpMessageConverter(json);
	}

}
