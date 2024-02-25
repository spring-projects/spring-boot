/*
 * Copyright 2012-2023 the original author or authors.
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

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties.ConstructorDetectorStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.boot.jackson.JsonMixinModuleEntries;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Auto configuration for Jackson. The following auto-configuration will get applied:
 * <ul>
 * <li>an {@link ObjectMapper} in case none is already configured.</li>
 * <li>a {@link Jackson2ObjectMapperBuilder} in case none is already configured.</li>
 * <li>auto-registration for all {@link Module} beans with all {@link ObjectMapper} beans
 * (including the defaulted ones).</li>
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
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
public class JacksonAutoConfiguration {

	private static final Map<?, Boolean> FEATURE_DEFAULTS;

	static {
		Map<Object, Boolean> featureDefaults = new HashMap<>();
		featureDefaults.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		featureDefaults.put(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
		FEATURE_DEFAULTS = Collections.unmodifiableMap(featureDefaults);
	}

	/**
	 * Creates a new instance of the {@link JsonComponentModule} class.
	 * @return The newly created {@link JsonComponentModule} instance.
	 */
	@Bean
	public JsonComponentModule jsonComponentModule() {
		return new JsonComponentModule();
	}

	/**
	 * JacksonMixinConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	static class JacksonMixinConfiguration {

		/**
		 * Scans the given ApplicationContext for packages containing Jackson mixin
		 * classes and returns a list of JsonMixinModuleEntries. If the ApplicationContext
		 * has AutoConfigurationPackages, it will scan those packages. Otherwise, it will
		 * return an empty list.
		 * @param context the ApplicationContext to scan
		 * @return a list of JsonMixinModuleEntries representing the scanned packages
		 */
		@Bean
		static JsonMixinModuleEntries jsonMixinModuleEntries(ApplicationContext context) {
			List<String> packages = AutoConfigurationPackages.has(context) ? AutoConfigurationPackages.get(context)
					: Collections.emptyList();
			return JsonMixinModuleEntries.scan(context, packages);
		}

		/**
		 * Creates and configures a {@link JsonMixinModule} bean.
		 * @param context the application context
		 * @param entries the entries containing mixin classes and target classes
		 * @return the configured {@link JsonMixinModule} bean
		 */
		@Bean
		JsonMixinModule jsonMixinModule(ApplicationContext context, JsonMixinModuleEntries entries) {
			JsonMixinModule jsonMixinModule = new JsonMixinModule();
			jsonMixinModule.registerEntries(entries, context.getClassLoader());
			return jsonMixinModule;
		}

	}

	/**
	 * JacksonObjectMapperConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
	static class JacksonObjectMapperConfiguration {

		/**
		 * Configures and returns the Jackson ObjectMapper bean.
		 *
		 * This method is annotated with @Bean, @Primary, and @ConditionalOnMissingBean
		 * annotations.
		 *
		 * The Jackson2ObjectMapperBuilder parameter is used to build the ObjectMapper
		 * bean.
		 *
		 * The createXmlMapper(false) method is called on the builder to disable XML
		 * mapping.
		 * @param builder the Jackson2ObjectMapperBuilder used to build the ObjectMapper
		 * bean
		 * @return the configured Jackson ObjectMapper bean
		 */
		@Bean
		@Primary
		@ConditionalOnMissingBean
		ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
			return builder.createXmlMapper(false).build();
		}

	}

	/**
	 * ParameterNamesModuleConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ParameterNamesModule.class)
	static class ParameterNamesModuleConfiguration {

		/**
		 * Creates and returns a new instance of the ParameterNamesModule with the
		 * specified JsonCreator mode. This method is annotated with @Bean
		 * and @ConditionalOnMissingBean, indicating that it will be used as a bean
		 * definition if no other bean of the same type is present in the application
		 * context.
		 * @return a new instance of the ParameterNamesModule with the specified
		 * JsonCreator mode
		 */
		@Bean
		@ConditionalOnMissingBean
		ParameterNamesModule parameterNamesModule() {
			return new ParameterNamesModule(JsonCreator.Mode.DEFAULT);
		}

	}

	/**
	 * JacksonObjectMapperBuilderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
	static class JacksonObjectMapperBuilderConfiguration {

		/**
		 * Creates a new instance of Jackson2ObjectMapperBuilder with prototype scope.
		 * This method is conditional on the absence of any existing bean of type
		 * Jackson2ObjectMapperBuilder.
		 * @param applicationContext the ApplicationContext instance
		 * @param customizers the list of Jackson2ObjectMapperBuilderCustomizer instances
		 * @return a new instance of Jackson2ObjectMapperBuilder
		 */
		@Bean
		@Scope("prototype")
		@ConditionalOnMissingBean
		Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder(ApplicationContext applicationContext,
				List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
			Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
			builder.applicationContext(applicationContext);
			customize(builder, customizers);
			return builder;
		}

		/**
		 * Customizes the Jackson2ObjectMapperBuilder with the provided customizers.
		 * @param builder the Jackson2ObjectMapperBuilder to be customized
		 * @param customizers the list of Jackson2ObjectMapperBuilderCustomizer objects to
		 * apply
		 */
		private void customize(Jackson2ObjectMapperBuilder builder,
				List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
			for (Jackson2ObjectMapperBuilderCustomizer customizer : customizers) {
				customizer.customize(builder);
			}
		}

	}

	/**
	 * Jackson2ObjectMapperBuilderCustomizerConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
	@EnableConfigurationProperties(JacksonProperties.class)
	static class Jackson2ObjectMapperBuilderCustomizerConfiguration {

		/**
		 * Returns a customizer for the StandardJackson2ObjectMapperBuilder.
		 * @param jacksonProperties the JacksonProperties object containing the Jackson
		 * configuration properties
		 * @param modules the ObjectProvider of Module objects to be registered with the
		 * ObjectMapper
		 * @return the StandardJackson2ObjectMapperBuilderCustomizer
		 */
		@Bean
		StandardJackson2ObjectMapperBuilderCustomizer standardJacksonObjectMapperBuilderCustomizer(
				JacksonProperties jacksonProperties, ObjectProvider<Module> modules) {
			return new StandardJackson2ObjectMapperBuilderCustomizer(jacksonProperties, modules.stream().toList());
		}

		/**
		 * StandardJackson2ObjectMapperBuilderCustomizer class.
		 */
		static final class StandardJackson2ObjectMapperBuilderCustomizer
				implements Jackson2ObjectMapperBuilderCustomizer, Ordered {

			private final JacksonProperties jacksonProperties;

			private final Collection<Module> modules;

			/**
			 * Constructs a new StandardJackson2ObjectMapperBuilderCustomizer with the
			 * specified JacksonProperties and modules.
			 * @param jacksonProperties the JacksonProperties to be used by the customizer
			 * @param modules the collection of modules to be used by the customizer
			 */
			StandardJackson2ObjectMapperBuilderCustomizer(JacksonProperties jacksonProperties,
					Collection<Module> modules) {
				this.jacksonProperties = jacksonProperties;
				this.modules = modules;
			}

			/**
			 * Returns the order value for this customizer.
			 *
			 * The order value determines the order in which the customizers are applied.
			 * A lower value means higher priority.
			 * @return the order value for this customizer
			 */
			@Override
			public int getOrder() {
				return 0;
			}

			/**
			 * Customize the Jackson2ObjectMapperBuilder with the provided configuration
			 * properties.
			 * @param builder the Jackson2ObjectMapperBuilder to customize
			 */
			@Override
			public void customize(Jackson2ObjectMapperBuilder builder) {
				if (this.jacksonProperties.getDefaultPropertyInclusion() != null) {
					builder.serializationInclusion(this.jacksonProperties.getDefaultPropertyInclusion());
				}
				if (this.jacksonProperties.getTimeZone() != null) {
					builder.timeZone(this.jacksonProperties.getTimeZone());
				}
				configureFeatures(builder, FEATURE_DEFAULTS);
				configureVisibility(builder, this.jacksonProperties.getVisibility());
				configureFeatures(builder, this.jacksonProperties.getDeserialization());
				configureFeatures(builder, this.jacksonProperties.getSerialization());
				configureFeatures(builder, this.jacksonProperties.getMapper());
				configureFeatures(builder, this.jacksonProperties.getParser());
				configureFeatures(builder, this.jacksonProperties.getGenerator());
				configureFeatures(builder, this.jacksonProperties.getDatatype().getEnum());
				configureFeatures(builder, this.jacksonProperties.getDatatype().getJsonNode());
				configureDateFormat(builder);
				configurePropertyNamingStrategy(builder);
				configureModules(builder);
				configureLocale(builder);
				configureDefaultLeniency(builder);
				configureConstructorDetector(builder);
			}

			/**
			 * Configures the features of the Jackson 2 Object Mapper Builder based on the
			 * provided map of features.
			 * @param builder the Jackson 2 Object Mapper Builder to configure
			 * @param features the map of features to enable or disable
			 */
			private void configureFeatures(Jackson2ObjectMapperBuilder builder, Map<?, Boolean> features) {
				features.forEach((feature, value) -> {
					if (value != null) {
						if (value) {
							builder.featuresToEnable(feature);
						}
						else {
							builder.featuresToDisable(feature);
						}
					}
				});
			}

			/**
			 * Configures the visibility of properties in the Jackson ObjectMapper.
			 * @param builder the Jackson2ObjectMapperBuilder instance
			 * @param visibilities a map containing the property accessors and their
			 * corresponding visibility
			 */
			private void configureVisibility(Jackson2ObjectMapperBuilder builder,
					Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities) {
				visibilities.forEach(builder::visibility);
			}

			/**
			 * Configures the date format for the Jackson ObjectMapper.
			 * @param builder the Jackson2ObjectMapperBuilder used to configure the
			 * ObjectMapper
			 */
			private void configureDateFormat(Jackson2ObjectMapperBuilder builder) {
				// We support a fully qualified class name extending DateFormat or a date
				// pattern string value
				String dateFormat = this.jacksonProperties.getDateFormat();
				if (dateFormat != null) {
					try {
						Class<?> dateFormatClass = ClassUtils.forName(dateFormat, null);
						builder.dateFormat((DateFormat) BeanUtils.instantiateClass(dateFormatClass));
					}
					catch (ClassNotFoundException ex) {
						SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
						// Since Jackson 2.6.3 we always need to set a TimeZone (see
						// gh-4170). If none in our properties fallback to the Jackson's
						// default
						TimeZone timeZone = this.jacksonProperties.getTimeZone();
						if (timeZone == null) {
							timeZone = new ObjectMapper().getSerializationConfig().getTimeZone();
						}
						simpleDateFormat.setTimeZone(timeZone);
						builder.dateFormat(simpleDateFormat);
					}
				}
			}

			/**
			 * Configures the property naming strategy for the Jackson object mapper
			 * builder.
			 * @param builder the Jackson object mapper builder
			 */
			private void configurePropertyNamingStrategy(Jackson2ObjectMapperBuilder builder) {
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

			/**
			 * Configures the property naming strategy class for the Jackson 2 object
			 * mapper builder.
			 * @param builder The Jackson 2 object mapper builder.
			 * @param propertyNamingStrategyClass The class representing the property
			 * naming strategy.
			 */
			private void configurePropertyNamingStrategyClass(Jackson2ObjectMapperBuilder builder,
					Class<?> propertyNamingStrategyClass) {
				builder.propertyNamingStrategy(
						(PropertyNamingStrategy) BeanUtils.instantiateClass(propertyNamingStrategyClass));
			}

			/**
			 * Configures the property naming strategy field of the
			 * Jackson2ObjectMapperBuilder.
			 * @param builder the Jackson2ObjectMapperBuilder instance
			 * @param fieldName the name of the field representing the property naming
			 * strategy
			 * @throws IllegalStateException if an exception occurs while setting the
			 * property naming strategy
			 * @throws IllegalArgumentException if the field with the given name is not
			 * found
			 */
			private void configurePropertyNamingStrategyField(Jackson2ObjectMapperBuilder builder, String fieldName) {
				// Find the field (this way we automatically support new constants
				// that may be added by Jackson in the future)
				Field field = findPropertyNamingStrategyField(fieldName);
				Assert.notNull(field, () -> "Constant named '" + fieldName + "' not found");
				try {
					builder.propertyNamingStrategy((PropertyNamingStrategy) field.get(null));
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}

			/**
			 * Finds the specified field in the
			 * {@link com.fasterxml.jackson.databind.PropertyNamingStrategies} class with
			 * the given field name and type {@link PropertyNamingStrategy}.
			 * @param fieldName the name of the field to find
			 * @return the {@link Field} object representing the found field, or
			 * {@code null} if not found
			 */
			private Field findPropertyNamingStrategyField(String fieldName) {
				return ReflectionUtils.findField(com.fasterxml.jackson.databind.PropertyNamingStrategies.class,
						fieldName, PropertyNamingStrategy.class);
			}

			/**
			 * Configures the modules to be installed in the Jackson 2 Object Mapper
			 * Builder.
			 * @param builder the Jackson 2 Object Mapper Builder
			 */
			private void configureModules(Jackson2ObjectMapperBuilder builder) {
				builder.modulesToInstall(this.modules.toArray(new Module[0]));
			}

			/**
			 * Configures the locale for the Jackson object mapper builder.
			 * @param builder the Jackson object mapper builder
			 */
			private void configureLocale(Jackson2ObjectMapperBuilder builder) {
				Locale locale = this.jacksonProperties.getLocale();
				if (locale != null) {
					builder.locale(locale);
				}
			}

			/**
			 * Configures the default leniency for the Jackson ObjectMapper.
			 * @param builder the Jackson2ObjectMapperBuilder to configure
			 */
			private void configureDefaultLeniency(Jackson2ObjectMapperBuilder builder) {
				Boolean defaultLeniency = this.jacksonProperties.getDefaultLeniency();
				if (defaultLeniency != null) {
					builder.postConfigurer((objectMapper) -> objectMapper.setDefaultLeniency(defaultLeniency));
				}
			}

			/**
			 * Configures the constructor detector strategy for the Jackson ObjectMapper.
			 * @param builder the Jackson2ObjectMapperBuilder instance
			 */
			private void configureConstructorDetector(Jackson2ObjectMapperBuilder builder) {
				ConstructorDetectorStrategy strategy = this.jacksonProperties.getConstructorDetector();
				if (strategy != null) {
					builder.postConfigurer((objectMapper) -> {
						switch (strategy) {
							case USE_PROPERTIES_BASED ->
								objectMapper.setConstructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
							case USE_DELEGATING ->
								objectMapper.setConstructorDetector(ConstructorDetector.USE_DELEGATING);
							case EXPLICIT_ONLY ->
								objectMapper.setConstructorDetector(ConstructorDetector.EXPLICIT_ONLY);
							default -> objectMapper.setConstructorDetector(ConstructorDetector.DEFAULT);
						}
					});
				}
			}

		}

	}

	/**
	 * JacksonAutoConfigurationRuntimeHints class.
	 */
	static class JacksonAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

		/**
		 * Registers hints for the given runtime hints and class loader. If the Jackson
		 * databind library is present in the class loader, property naming strategy hints
		 * will be registered for reflection.
		 * @param hints the runtime hints to register
		 * @param classLoader the class loader to check for Jackson databind library
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			if (ClassUtils.isPresent("com.fasterxml.jackson.databind.PropertyNamingStrategy", classLoader)) {
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

		/**
		 * Registers property naming strategy hints for the given type.
		 * @param hints the reflection hints object to register the field with
		 * @param type the class for which to register the property naming strategy hints
		 */
		private void registerPropertyNamingStrategyHints(ReflectionHints hints, Class<?> type) {
			Stream.of(type.getDeclaredFields())
				.filter(this::isPropertyNamingStrategyField)
				.forEach(hints::registerField);
		}

		/**
		 * Checks if the given field is a property naming strategy field.
		 * @param candidate the field to check
		 * @return true if the field is a property naming strategy field, false otherwise
		 */
		private boolean isPropertyNamingStrategyField(Field candidate) {
			return ReflectionUtils.isPublicStaticFinal(candidate)
					&& candidate.getType().isAssignableFrom(PropertyNamingStrategy.class);
		}

	}

}
