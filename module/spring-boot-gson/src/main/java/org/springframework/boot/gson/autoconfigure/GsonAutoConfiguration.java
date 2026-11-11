/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gson.autoconfigure;

import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

import org.springframework.beans.factory.ObjectProvider;
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
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Gson.class)
@EnableConfigurationProperties(GsonProperties.class)
public final class GsonAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	GsonBuilder gsonBuilder(ObjectProvider<GsonBuilderCustomizer> customizers) {
		GsonBuilder builder = new GsonBuilder();
		customizers.orderedStream().forEach((c) -> c.customize(builder));
		return builder;
	}

	@Bean
	@ConditionalOnMissingBean
	Gson gson(GsonBuilder gsonBuilder) {
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
			PropertyMapper map = PropertyMapper.get();
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
