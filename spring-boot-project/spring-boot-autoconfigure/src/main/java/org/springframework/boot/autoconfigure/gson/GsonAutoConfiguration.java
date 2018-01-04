/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.gson;

import java.util.List;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Gson.
 *
 * @author David Liu
 * @author Ivan Golovko
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass(Gson.class)
public class GsonAutoConfiguration {

	@Configuration
	static class GsonConfiguration {

		@Bean
		@Primary
		@ConditionalOnMissingBean(Gson.class)
		public Gson gson(GsonBuilder gsonBuilder) {
			return gsonBuilder.create();
		}
	}

	@Configuration
	static class GsonBuilderConfiguration {

		@Bean
		public GsonBuilder gsonBuilder(List<GsonBuilderCustomizer> customizers) {
			final GsonBuilder gsonBuilder = new GsonBuilder();
			customizers.forEach(c -> c.customize(gsonBuilder));
			return gsonBuilder;
		}

	}

	@Configuration
	@EnableConfigurationProperties(GsonProperties.class)
	static class GsonBuilderCustomizerConfiguration {

		@Bean
		public StandardGsonBuilderCustomizer standardGsonBuilderCustomizer(
				GsonProperties gsonProperties) {
			return new StandardGsonBuilderCustomizer(gsonProperties);
		}

		private static final class StandardGsonBuilderCustomizer
				implements GsonBuilderCustomizer, Ordered {

			private final GsonProperties properties;

			StandardGsonBuilderCustomizer(GsonProperties properties) {
				this.properties = properties;
			}

			@Override
			public int getOrder() {
				return 0;
			}

			@Override
			public void customize(GsonBuilder gsonBuilder) {

				boolean generateNonExecutableJson = this.properties
						.isGenerateNonExecutableJson();
				if (generateNonExecutableJson) {
					gsonBuilder.generateNonExecutableJson();
				}

				boolean excludeFieldsWithoutExposeAnnotation = this.properties
						.isExcludeFieldsWithoutExposeAnnotation();
				if (excludeFieldsWithoutExposeAnnotation) {
					gsonBuilder.excludeFieldsWithoutExposeAnnotation();
				}

				boolean serializeNulls = this.properties.isSerializeNulls();
				if (serializeNulls) {
					gsonBuilder.serializeNulls();
				}

				boolean enableComplexMapKeySerialization = this.properties
						.isEnableComplexMapKeySerialization();
				if (enableComplexMapKeySerialization) {
					gsonBuilder.enableComplexMapKeySerialization();
				}

				boolean disableInnerClassSerialization = this.properties
						.isDisableInnerClassSerialization();
				if (disableInnerClassSerialization) {
					gsonBuilder.disableInnerClassSerialization();
				}

				LongSerializationPolicy longSerializationPolicy = this.properties
						.getLongSerializationPolicy();
				if (longSerializationPolicy != null) {
					gsonBuilder.setLongSerializationPolicy(longSerializationPolicy);
				}

				FieldNamingPolicy fieldNamingPolicy = this.properties
						.getFieldNamingPolicy();
				if (fieldNamingPolicy != null) {
					gsonBuilder.setFieldNamingPolicy(fieldNamingPolicy);
				}

				boolean prettyPrinting = this.properties.isPrettyPrinting();
				if (prettyPrinting) {
					gsonBuilder.setPrettyPrinting();
				}

				boolean isLenient = this.properties.isLenient();
				if (isLenient) {
					gsonBuilder.setLenient();
				}

				boolean disableHtmlEscaping = this.properties.isDisableHtmlEscaping();
				if (disableHtmlEscaping) {
					gsonBuilder.disableHtmlEscaping();
				}

				String dateFormat = this.properties.getDateFormat();
				if (dateFormat != null) {
					gsonBuilder.setDateFormat(dateFormat);
				}
			}
		}
	}

}
