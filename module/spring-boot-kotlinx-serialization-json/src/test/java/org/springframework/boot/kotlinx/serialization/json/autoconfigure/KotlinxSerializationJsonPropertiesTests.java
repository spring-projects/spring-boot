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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KotlinxSerializationJsonProperties}.
 *
 * @author Andy Wilkinson
 */
class KotlinxSerializationJsonPropertiesTests {

	@ParameterizedTest
	@EnumSource
	void defaultsAreAligned(JsonBuilderSettings settings) {
		JsonBuilder jsonBuilder = new JsonBuilder(Json.Default);
		KotlinxSerializationJsonProperties properties = new KotlinxSerializationJsonProperties();
		assertThat(settings.propertyGetter.get(properties)).isEqualTo(settings.jsonBuilderGetter.get(jsonBuilder));
	}

	private enum JsonBuilderSettings {

		ALLOW_COMMENTS(KotlinxSerializationJsonProperties::isAllowComments, JsonBuilder::getAllowComments),

		ALLOW_SPECIAL_FLOATING_POINT_VALUES(KotlinxSerializationJsonProperties::isAllowSpecialFloatingPointValues,
				JsonBuilder::getAllowSpecialFloatingPointValues),

		ALLOW_STRUCTURED_MAP_KEYS(KotlinxSerializationJsonProperties::isAllowStructuredMapKeys,
				JsonBuilder::getAllowStructuredMapKeys),

		ALLOW_TRAILING_COMMA(KotlinxSerializationJsonProperties::isAllowTrailingComma,
				JsonBuilder::getAllowTrailingComma),

		CLASS_DISCRIMINATOR(KotlinxSerializationJsonProperties::getClassDiscriminator,
				JsonBuilder::getClassDiscriminator),

		CLASS_DISCRIMINATOR_MODE(KotlinxSerializationJsonProperties::getClassDiscriminatorMode,
				JsonBuilder::getClassDiscriminatorMode),

		COERCE_INPUT_VALUES(KotlinxSerializationJsonProperties::isCoerceInputValues, JsonBuilder::getCoerceInputValues),

		DECODE_ENUMS_CASE_INSENSITIVE(KotlinxSerializationJsonProperties::isDecodeEnumsCaseInsensitive,
				JsonBuilder::getDecodeEnumsCaseInsensitive),

		ENCODE_DEFAULTS(KotlinxSerializationJsonProperties::isEncodeDefaults, JsonBuilder::getEncodeDefaults),

		EXPLICIT_NULLS(KotlinxSerializationJsonProperties::isExplicitNulls, JsonBuilder::getExplicitNulls),

		IGNORE_UNKNOWN_KEYS(KotlinxSerializationJsonProperties::isIgnoreUnknownKeys, JsonBuilder::getIgnoreUnknownKeys),

		LENIENT(KotlinxSerializationJsonProperties::isLenient, JsonBuilder::isLenient),

		NAMING_STRATEGY(KotlinxSerializationJsonProperties::getNamingStrategy, JsonBuilder::getNamingStrategy),

		PRETTY_PRINT(KotlinxSerializationJsonProperties::isPrettyPrint, JsonBuilder::getPrettyPrint),

		USE_ALTERNATIVE_NAMES(KotlinxSerializationJsonProperties::isUseAlternativeNames,
				JsonBuilder::getUseAlternativeNames);

		private final Accessor<KotlinxSerializationJsonProperties, ?> propertyGetter;

		private final Accessor<JsonBuilder, ?> jsonBuilderGetter;

		JsonBuilderSettings(Accessor<KotlinxSerializationJsonProperties, @Nullable Object> propertyGetter,
				Accessor<JsonBuilder, @Nullable Object> jsonBuilderGetter) {
			this.propertyGetter = propertyGetter;
			this.jsonBuilderGetter = jsonBuilderGetter;
		}

		private interface Accessor<S, @Nullable P extends @Nullable Object> {

			@Nullable P get(S source);

		}

	}

}
