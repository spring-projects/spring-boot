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

package org.springframework.boot.kotlinx.serialization.json.autoconfigure;

import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link Json} through {@link JsonBuilder} retaining its default configuration.
 *
 * @author Dmitry Sulman
 * @since 4.0.0
 */
@FunctionalInterface
public interface KotlinxSerializationJsonBuilderCustomizer {

	/**
	 * Customize the Kotlin Serialization {@link Json} through {@link JsonBuilder}.
	 * @param jsonBuilder the {@link JsonBuilder} to customize
	 */
	void customize(JsonBuilder jsonBuilder);

}
