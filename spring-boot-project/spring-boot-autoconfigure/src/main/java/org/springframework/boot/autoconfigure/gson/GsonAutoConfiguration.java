/*
 * Copyright 2012-2024 the original author or authors.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

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

	/**
     * Creates a GsonBuilder bean if no other bean of the same type is present.
     * 
     * @param customizers a list of GsonBuilderCustomizer beans to customize the GsonBuilder
     * @return the created GsonBuilder bean
     */
    @Bean
	@ConditionalOnMissingBean
	public GsonBuilder gsonBuilder(List<GsonBuilderCustomizer> customizers) {
		GsonBuilder builder = new GsonBuilder();
		customizers.forEach((c) -> c.customize(builder));
		return builder;
	}

	/**
     * Creates a Gson instance using the provided GsonBuilder.
     * 
     * @param gsonBuilder the GsonBuilder used to configure the Gson instance
     * @return the created Gson instance
     */
    @Bean
	@ConditionalOnMissingBean
	public Gson gson(GsonBuilder gsonBuilder) {
		return gsonBuilder.create();
	}

	/**
     * Creates a new instance of StandardGsonBuilderCustomizer with the provided GsonProperties.
     * 
     * @param gsonProperties the GsonProperties to be used for customization
     * @return a new instance of StandardGsonBuilderCustomizer
     */
    @Bean
	public StandardGsonBuilderCustomizer standardGsonBuilderCustomizer(GsonProperties gsonProperties) {
		return new StandardGsonBuilderCustomizer(gsonProperties);
	}

	/**
     * StandardGsonBuilderCustomizer class.
     */
    static final class StandardGsonBuilderCustomizer implements GsonBuilderCustomizer, Ordered {

		private final GsonProperties properties;

		/**
         * Constructs a new StandardGsonBuilderCustomizer with the specified GsonProperties.
         * 
         * @param properties the GsonProperties to be used for customization
         */
        StandardGsonBuilderCustomizer(GsonProperties properties) {
			this.properties = properties;
		}

		/**
         * Returns the order in which this customizer should be applied.
         * 
         * @return the order value, with lower values indicating higher priority
         */
        @Override
		public int getOrder() {
			return 0;
		}

		/**
         * Customize the GsonBuilder with the provided properties.
         *
         * @param builder the GsonBuilder to customize
         */
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
			map.from(properties::getLenient).whenTrue().toCall(builder::setLenient);
			map.from(properties::getDisableHtmlEscaping).whenTrue().toCall(builder::disableHtmlEscaping);
			map.from(properties::getDateFormat).to(builder::setDateFormat);
		}

	}

}
