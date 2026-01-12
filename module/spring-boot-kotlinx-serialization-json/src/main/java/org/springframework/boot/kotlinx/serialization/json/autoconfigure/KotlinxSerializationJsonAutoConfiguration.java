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

import java.util.List;
import java.util.function.Consumer;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonBuilder;
import kotlinx.serialization.json.JsonKt;
import kotlinx.serialization.json.JsonNamingStrategy;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Kotlinx Serialization JSON.
 *
 * @author Dmitry Sulman
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Json.class)
@EnableConfigurationProperties(KotlinxSerializationJsonProperties.class)
public final class KotlinxSerializationJsonAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	Json kotlinSerializationJson(List<KotlinxSerializationJsonBuilderCustomizer> customizers) {
		Function1<JsonBuilder, Unit> builderAction = (jsonBuilder) -> {
			customizers.forEach((c) -> c.customize(jsonBuilder));
			return Unit.INSTANCE;
		};
		return JsonKt.Json(Json.Default, builderAction);
	}

	@Bean
	StandardKotlinSerializationJsonBuilderCustomizer standardKotlinSerializationJsonBuilderCustomizer(
			KotlinxSerializationJsonProperties kotlinSerializationProperties) {
		return new StandardKotlinSerializationJsonBuilderCustomizer(kotlinSerializationProperties);
	}

	static final class StandardKotlinSerializationJsonBuilderCustomizer
			implements KotlinxSerializationJsonBuilderCustomizer, Ordered {

		private final KotlinxSerializationJsonProperties properties;

		StandardKotlinSerializationJsonBuilderCustomizer(KotlinxSerializationJsonProperties properties) {
			this.properties = properties;
		}

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public void customize(JsonBuilder jsonBuilder) {
			KotlinxSerializationJsonProperties properties = this.properties;
			PropertyMapper map = PropertyMapper.get();
			map.from(properties::getNamingStrategy).to(setNamingStrategy(jsonBuilder));
			map.from(properties::isPrettyPrint).to(jsonBuilder::setPrettyPrint);
			map.from(properties::isLenient).to(jsonBuilder::setLenient);
			map.from(properties::isIgnoreUnknownKeys).to(jsonBuilder::setIgnoreUnknownKeys);
			map.from(properties::isEncodeDefaults).to(jsonBuilder::setEncodeDefaults);
			map.from(properties::isExplicitNulls).to(jsonBuilder::setExplicitNulls);
			map.from(properties::isCoerceInputValues).to(jsonBuilder::setCoerceInputValues);
			map.from(properties::isAllowStructuredMapKeys).to(jsonBuilder::setAllowStructuredMapKeys);
			map.from(properties::isAllowSpecialFloatingPointValues).to(jsonBuilder::setAllowSpecialFloatingPointValues);
			map.from(properties::getClassDiscriminator).to(jsonBuilder::setClassDiscriminator);
			map.from(properties::getClassDiscriminatorMode).to(jsonBuilder::setClassDiscriminatorMode);
			map.from(properties::isDecodeEnumsCaseInsensitive).to(jsonBuilder::setDecodeEnumsCaseInsensitive);
			map.from(properties::isUseAlternativeNames).to(jsonBuilder::setUseAlternativeNames);
			map.from(properties::isAllowTrailingComma).to(jsonBuilder::setAllowTrailingComma);
			map.from(properties::isAllowComments).to(jsonBuilder::setAllowComments);
		}

		private Consumer<KotlinxSerializationJsonProperties.JsonNamingStrategy> setNamingStrategy(JsonBuilder builder) {
			return (strategy) -> {
				JsonNamingStrategy namingStrategy = switch (strategy) {
					case SNAKE_CASE -> JsonNamingStrategy.Builtins.getSnakeCase();
					case KEBAB_CASE -> JsonNamingStrategy.Builtins.getKebabCase();
				};
				builder.setNamingStrategy(namingStrategy);
			};
		}

	}

}
