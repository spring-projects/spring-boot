/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.gson;

import java.util.List;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Gson.
 *
 * @author David Liu
 * @author Ivan Golovko
 * @since 1.2.0
 */
@AutoConfiguration
@ConditionalOnClass(Gson.class)
@EnableConfigurationProperties(GsonProperties.class)
public class GsonAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public GsonBuilder gsonBuilder(List<GsonBuilderCustomizer> customizers) {
		GsonBuilder builder = new GsonBuilder();
		customizers.forEach((c) -> c.customize(builder));
		return builder;
	}

	@Bean
	@ConditionalOnMissingBean
	public Gson gson(GsonBuilder gsonBuilder) {
		return gsonBuilder.create();
	}

	@Bean
	StandardGsonBuilderCustomizer standardGsonBuilderCustomizer(GsonProperties gsonProperties) {
		return new StandardGsonBuilderCustomizer(gsonProperties);
	}

	static final class StandardGsonBuilderCustomizer implements GsonBuilderCustomizer, Ordered {

		private final GsonProperties properties;

		StandardGsonBuilderCustomizer(GsonProperties properties) {
			this.properties = properties;
		}

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public void customize(GsonBuilder builder) {
			GsonProperties properties = this.properties;
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(properties::getGenerateNonExecutableJson).whenTrue().toCall(builder::generateNonExecutableJson);
			map.from(properties::getExcludeFieldsWithoutExposeAnnotation)
				.whenTrue()
				.toCall(builder::excludeFieldsWithoutExposeAnnotation);
			map.from(properties::getSerializeNulls).whenTrue().toCall(builder::serializeNulls);
			map.from(properties::getEnableComplexMapKeySerialization)
				.whenTrue()
				.toCall(builder::enableComplexMapKeySerialization);
			map.from(properties::getDisableInnerClassSerialization)
				.whenTrue()
				.toCall(builder::disableInnerClassSerialization);
			map.from(properties::getLongSerializationPolicy).to(builder::setLongSerializationPolicy);
			map.from(properties::getFieldNamingPolicy).to(builder::setFieldNamingPolicy);
			map.from(properties::getPrettyPrinting).whenTrue().toCall(builder::setPrettyPrinting);
			map.from(properties::getStrictness).to(strictnessOrLeniency(builder));
			map.from(properties::getDisableHtmlEscaping).whenTrue().toCall(builder::disableHtmlEscaping);
			map.from(properties::getDateFormat).to(builder::setDateFormat);
		}

		@SuppressWarnings("deprecation")
		private Consumer<GsonProperties.Strictness> strictnessOrLeniency(GsonBuilder builder) {
			if (ClassUtils.isPresent("com.google.gson.Strictness", getClass().getClassLoader())) {
				return (strictness) -> builder.setStrictness(Strictness.valueOf(strictness.name()));
			}
			return (strictness) -> {
				if (strictness == GsonProperties.Strictness.LENIENT) {
					builder.setLenient();
				}
			};
		}

	}

}
