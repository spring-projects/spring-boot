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

package org.springframework.boot.autoconfigure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link ObjectMapper} via {@link Jackson2ObjectMapperBuilder} retaining its default
 * auto-configuration.
 *
 * @author Grzegorz Poznachowski
 * @since 1.4.0
 */
@FunctionalInterface
public interface Jackson2ObjectMapperBuilderCustomizer {

	/**
	 * Customize the JacksonObjectMapperBuilder.
	 * @param jacksonObjectMapperBuilder the JacksonObjectMapperBuilder to customize
	 */
	void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder);

}
