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

package org.springframework.boot.jackson.autoconfigure;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.boot.jackson.JsonMixinModuleEntries;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties.ConstructorDetectorStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Auto configuration for Jackson. The following auto-configuration will get applied:
 * <ul>
 * <li>an {@link JsonMapper} in case none is already configured.</li>
 * <li>a {@link tools.jackson.databind.json.JsonMapper.Builder} in case none is already
 * configured.</li>
 * <li>auto-registration for all {@link JacksonModule} beans with all {@link ObjectMapper}
 * beans (including the defaulted ones).</li>
 * </ul>
 *
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Sebastien Deleuze
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Ralf Ueberfuhr
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(JsonMapper.class)
public final class JacksonAutoConfiguration {

	@Bean
	JsonComponentModule jsonComponentModule() {
		return new JsonComponentModule();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean
	JsonMapper.Builder jsonMapperBuilder(List<JsonMapperBuilderCustomizer> customizers) {
		JsonMapper.Builder builder = JsonMapper.builder();
		customize(builder, customizers);
		return builder;
	}

	private void customize(JsonMapper.Builder builder, List<JsonMapperBuilderCustomizer> customizers) {
		for (JsonMapperBuilderCustomizer customizer : customizers) {
			customizer.customize(builder);
		}
	}

	@Bean
	@Primary
	@ConditionalOnMissingBean(ObjectMapper.class)
	JsonMapper jacksonJsonMapper(JsonMapper.Builder builder) {
		return builder.build();
	}

	@Configuration(proxyBeanMethods = false)
	static class JacksonMixinConfiguration {

		@Bean
		static JsonMixinModuleEntries jsonMixinModuleEntries(ApplicationContext context) {
			List<String> packages = AutoConfigurationPackages.has(context) ? AutoConfigurationPackages.get(context)
					: Collections.emptyList();
			return JsonMixinModuleEntries.scan(context, packages);
		}

		@Bean
		JsonMixinModule jsonMixinModule(ApplicationContext context, JsonMixinModuleEntries entries) {
			JsonMixinModule jsonMixinModule = new JsonMixinModule();
			jsonMixinModule.registerEntries(entries, context.getClassLoader());
			return jsonMixinModule;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JacksonProperties.class)
	static class JacksonJsonMapperBuilderCustomizerConfiguration {

		@Bean
		StandardJsonMapperBuilderCustomizer standardJsonMapperBuilderCustomizer(JacksonProperties jacksonProperties,
				ObjectProvider<JacksonModule> modules) {
			return new StandardJsonMapperBuilderCustomizer(jacksonProperties, modules.stream().toList());
		}

		static final class StandardJsonMapperBuilderCustomizer implements JsonMapperBuilderCustomizer, Ordered {

			private final JacksonProperties jacksonProperties;

			private final Collection<JacksonModule> modules;

			StandardJsonMapperBuilderCustomizer(JacksonProperties jacksonProperties,
					Collection<JacksonModule> modules) {
				this.jacksonProperties = jacksonProperties;
				this.modules = modules;
			}

			@Override
			public int getOrder() {
				return 0;
			}

			@Override
			public void customize(JsonMapper.Builder builder) {
				if (this.jacksonProperties.getDefaultPropertyInclusion() != null) {
					builder.changeDefaultPropertyInclusion((handler) -> handler
						.withValueInclusion(this.jacksonProperties.getDefaultPropertyInclusion()));
				}
				if (this.jacksonProperties.getTimeZone() != null) {
					builder.defaultTimeZone(this.jacksonProperties.getTimeZone());
				}
				configureVisibility(builder, this.jacksonProperties.getVisibility());
				configureFeatures(builder, this.jacksonProperties.getDeserialization(), builder::configure);
				configureFeatures(builder, this.jacksonProperties.getSerialization(), builder::configure);
				configureFeatures(builder, this.jacksonProperties.getMapper(), builder::configure);
				configureFeatures(builder, this.jacksonProperties.getRead(), builder::configure);
				configureFeatures(builder, this.jacksonProperties.getWrite(), builder::configure);
				configureFeatures(builder, this.jacksonProperties.getDatatype().getEnum(), builder::configure);
				configureFeatures(builder, this.jacksonProperties.getDatatype().getJsonNode(), builder::configure);
				configureDateFormat(builder);
				configurePropertyNamingStrategy(builder);
				configureModules(builder);
				configureLocale(builder);
				configureDefaultLeniency(builder);
				configureConstructorDetector(builder);
			}

			private <T> void configureFeatures(JsonMapper.Builder builder, Map<T, Boolean> features,
					BiConsumer<T, Boolean> configure) {
				features.forEach((feature, value) -> {
					if (value != null) {
						configure.accept(feature, value);
					}
				});
			}

			private void configureVisibility(JsonMapper.Builder builder,
					Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities) {
				builder.changeDefaultVisibility((visibilityChecker) -> {
					for (Map.Entry<PropertyAccessor, JsonAutoDetect.Visibility> entry : visibilities.entrySet()) {
						visibilityChecker = visibilityChecker.withVisibility(entry.getKey(), entry.getValue());
					}
					return visibilityChecker;
				});
			}

			private void configureDateFormat(JsonMapper.Builder builder) {
				// We support a fully qualified class name extending DateFormat or a date
				// pattern string value
				String dateFormat = this.jacksonProperties.getDateFormat();
				if (dateFormat != null) {
					try {
						Class<?> dateFormatClass = ClassUtils.forName(dateFormat, null);
						builder.defaultDateFormat((DateFormat) BeanUtils.instantiateClass(dateFormatClass));
					}
					catch (ClassNotFoundException ex) {
						SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
						// Since Jackson 2.6.3 we always need to set a TimeZone (see
						// gh-4170). If none in our properties fallback to Jackson's
						// default
						TimeZone timeZone = this.jacksonProperties.getTimeZone();
						if (timeZone == null) {
							timeZone = new ObjectMapper().serializationConfig().getTimeZone();
						}
						simpleDateFormat.setTimeZone(timeZone);
						builder.defaultDateFormat(simpleDateFormat);
					}
				}
			}

			private void configurePropertyNamingStrategy(JsonMapper.Builder builder) {
				// We support a fully qualified class name extending Jackson's
				// PropertyNamingStrategy or a string value corresponding to the constant
				// names in PropertyNamingStrategy which hold default provided
				// implementations
				String strategy = this.jacksonProperties.getPropertyNamingStrategy();
				if (strategy != null) {
					try {
						configurePropertyNamingStrategyClass(builder, ClassUtils.forName(strategy, null));
					}
					catch (ClassNotFoundException ex) {
						configurePropertyNamingStrategyField(builder, strategy);
					}
				}
			}

			private void configurePropertyNamingStrategyClass(JsonMapper.Builder builder,
					Class<?> propertyNamingStrategyClass) {
				builder.propertyNamingStrategy(
						(PropertyNamingStrategy) BeanUtils.instantiateClass(propertyNamingStrategyClass));
			}

			private void configurePropertyNamingStrategyField(JsonMapper.Builder builder, String fieldName) {
				// Find the field (this way we automatically support new constants
				// that may be added by Jackson in the future)
				Field field = findPropertyNamingStrategyField(fieldName);
				try {
					builder.propertyNamingStrategy((PropertyNamingStrategy) field.get(null));
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}

			private Field findPropertyNamingStrategyField(String fieldName) {
				Field field = ReflectionUtils.findField(PropertyNamingStrategies.class, fieldName,
						PropertyNamingStrategy.class);
				Assert.state(field != null, () -> "Constant named '" + fieldName + "' not found");
				return field;
			}

			private void configureModules(JsonMapper.Builder builder) {
				builder.addModules(this.modules);
			}

			private void configureLocale(JsonMapper.Builder builder) {
				Locale locale = this.jacksonProperties.getLocale();
				if (locale != null) {
					builder.defaultLocale(locale);
				}
			}

			private void configureDefaultLeniency(JsonMapper.Builder builder) {
				Boolean defaultLeniency = this.jacksonProperties.getDefaultLeniency();
				if (defaultLeniency != null) {
					builder.defaultLeniency(defaultLeniency);
				}
			}

			private void configureConstructorDetector(JsonMapper.Builder builder) {
				ConstructorDetectorStrategy strategy = this.jacksonProperties.getConstructorDetector();
				if (strategy != null) {
					switch (strategy) {
						case USE_PROPERTIES_BASED ->
							builder.constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
						case USE_DELEGATING -> builder.constructorDetector(ConstructorDetector.USE_DELEGATING);
						case EXPLICIT_ONLY -> builder.constructorDetector(ConstructorDetector.EXPLICIT_ONLY);
						default -> builder.constructorDetector(ConstructorDetector.DEFAULT);
					}
				}
			}

		}

	}

	static class JacksonAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			if (ClassUtils.isPresent("tools.jackson.databind.PropertyNamingStrategy", classLoader)) {
				registerPropertyNamingStrategyHints(hints.reflection());
			}
		}

		/**
		 * Register hints for the {@code configurePropertyNamingStrategyField} method to
		 * use.
		 * @param hints reflection hints
		 */
		private void registerPropertyNamingStrategyHints(ReflectionHints hints) {
			registerPropertyNamingStrategyHints(hints, PropertyNamingStrategies.class);
		}

		private void registerPropertyNamingStrategyHints(ReflectionHints hints, Class<?> type) {
			Stream.of(type.getDeclaredFields())
				.filter(this::isPropertyNamingStrategyField)
				.forEach(hints::registerField);
		}

		private boolean isPropertyNamingStrategyField(Field candidate) {
			return ReflectionUtils.isPublicStaticFinal(candidate)
					&& candidate.getType().isAssignableFrom(PropertyNamingStrategy.class);
		}

	}

}
