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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.cbor.CBORMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.JacksonComponentModule;
import org.springframework.boot.jackson.JacksonMixinModule;
import org.springframework.boot.jackson.JacksonMixinModuleEntries;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties.ConstructorDetectorStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.http.converter.json.ProblemDetailJacksonXmlMixin;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Jackson.
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
	JacksonComponentModule jsonComponentModule() {
		return new JacksonComponentModule();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean
	JsonMapper.Builder jsonMapperBuilder(ObjectProvider<JsonFactory> jsonFactory,
			List<JsonMapperBuilderCustomizer> customizers) {
		JsonMapper.Builder builder = JsonMapper.builder(jsonFactory.getIfAvailable(JsonFactory::new));
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
	@ConditionalOnMissingBean
	JsonMapper jacksonJsonMapper(JsonMapper.Builder builder) {
		return builder.build();
	}

	@Configuration(proxyBeanMethods = false)
	static class JacksonMixinConfiguration {

		@Bean
		static JacksonMixinModuleEntries jacksonMixinModuleEntries(ApplicationContext context) {
			List<String> packages = AutoConfigurationPackages.has(context) ? AutoConfigurationPackages.get(context)
					: Collections.emptyList();
			return JacksonMixinModuleEntries.scan(context, packages);
		}

		@Bean
		JacksonMixinModule jacksonMixinModule(ApplicationContext context, JacksonMixinModuleEntries entries) {
			JacksonMixinModule jacksonMixinModule = new JacksonMixinModule();
			jacksonMixinModule.registerEntries(entries, context.getClassLoader());
			return jacksonMixinModule;
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

		static final class StandardJsonMapperBuilderCustomizer
				extends AbstractMapperBuilderCustomizer<JsonMapper.Builder> implements JsonMapperBuilderCustomizer {

			StandardJsonMapperBuilderCustomizer(JacksonProperties jacksonProperties,
					Collection<JacksonModule> modules) {
				super(jacksonProperties, modules);
			}

			@Override
			public void customize(JsonMapper.Builder builder) {
				super.customize(builder);
				configureFeatures(builder, properties().getJson().getRead(), builder::configure);
				configureFeatures(builder, properties().getJson().getWrite(), builder::configure);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ProblemDetail.class)
	static class JsonProblemDetailsConfiguration {

		@Bean
		ProblemDetailJsonMapperBuilderCustomizer problemDetailJsonMapperBuilderCustomizer() {
			return new ProblemDetailJsonMapperBuilderCustomizer();
		}

		static final class ProblemDetailJsonMapperBuilderCustomizer implements JsonMapperBuilderCustomizer {

			@Override
			public void customize(JsonMapper.Builder builder) {
				builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(CBORMapper.class)
	@EnableConfigurationProperties(JacksonCborProperties.class)
	static class CborConfiguration {

		@Bean
		@ConditionalOnMissingBean
		CBORMapper cborMapper(CBORMapper.Builder builder) {
			return builder.build();
		}

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		@ConditionalOnMissingBean
		CBORMapper.Builder cborMapperBuilder(List<CborMapperBuilderCustomizer> customizers) {
			CBORMapper.Builder builder = CBORMapper.builder();
			customize(builder, customizers);
			return builder;
		}

		private void customize(CBORMapper.Builder builder, List<CborMapperBuilderCustomizer> customizers) {
			for (CborMapperBuilderCustomizer customizer : customizers) {
				customizer.customize(builder);
			}
		}

		@Bean
		StandardCborMapperBuilderCustomizer standardCborMapperBuilderCustomizer(JacksonProperties jacksonProperties,
				ObjectProvider<JacksonModule> modules, JacksonCborProperties cborProperties) {
			return new StandardCborMapperBuilderCustomizer(jacksonProperties, modules.stream().toList(),
					cborProperties);
		}

		static class StandardCborMapperBuilderCustomizer extends AbstractMapperBuilderCustomizer<CBORMapper.Builder>
				implements CborMapperBuilderCustomizer {

			private final JacksonCborProperties cborProperties;

			StandardCborMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules,
					JacksonCborProperties cborProperties) {
				super(jacksonProperties, modules);
				this.cborProperties = cborProperties;
			}

			@Override
			public void customize(CBORMapper.Builder builder) {
				super.customize(builder);
				configureFeatures(builder, this.cborProperties.getRead(), builder::configure);
				configureFeatures(builder, this.cborProperties.getWrite(), builder::configure);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(XmlMapper.class)
	@EnableConfigurationProperties(JacksonXmlProperties.class)
	static class XmlConfiguration {

		@Bean
		@ConditionalOnMissingBean
		XmlMapper xmlMapper(XmlMapper.Builder builder) {
			return builder.build();
		}

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		@ConditionalOnMissingBean
		XmlMapper.Builder xmlMapperBuilder(List<XmlMapperBuilderCustomizer> customizers) {
			XmlMapper.Builder builder = XmlMapper.builder();
			customize(builder, customizers);
			return builder;
		}

		private void customize(XmlMapper.Builder builder, List<XmlMapperBuilderCustomizer> customizers) {
			for (XmlMapperBuilderCustomizer customizer : customizers) {
				customizer.customize(builder);
			}
		}

		@Bean
		StandardXmlMapperBuilderCustomizer standardXmlMapperBuilderCustomizer(JacksonProperties jacksonProperties,
				ObjectProvider<JacksonModule> modules, JacksonXmlProperties xmlProperties) {
			return new StandardXmlMapperBuilderCustomizer(jacksonProperties, modules.stream().toList(), xmlProperties);
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(ProblemDetail.class)
		static class XmlProblemDetailsConfiguration {

			@Bean
			ProblemDetailXmlMapperBuilderCustomizer problemDetailXmlMapperBuilderCustomizer() {
				return new ProblemDetailXmlMapperBuilderCustomizer();
			}

			static final class ProblemDetailXmlMapperBuilderCustomizer implements XmlMapperBuilderCustomizer {

				@Override
				public void customize(XmlMapper.Builder builder) {
					builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonXmlMixin.class);
				}

			}

		}

		static class StandardXmlMapperBuilderCustomizer extends AbstractMapperBuilderCustomizer<XmlMapper.Builder>
				implements XmlMapperBuilderCustomizer {

			private final JacksonXmlProperties xmlProperties;

			StandardXmlMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules,
					JacksonXmlProperties xmlProperties) {
				super(jacksonProperties, modules);
				this.xmlProperties = xmlProperties;
			}

			@Override
			public void customize(XmlMapper.Builder builder) {
				super.customize(builder);
				configureFeatures(builder, this.xmlProperties.getRead(), builder::configure);
				configureFeatures(builder, this.xmlProperties.getWrite(), builder::configure);
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

	abstract static class AbstractMapperBuilderCustomizer<B extends MapperBuilder<?, ?>> implements Ordered {

		private final JacksonProperties jacksonProperties;

		private final Collection<JacksonModule> modules;

		AbstractMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules) {
			this.jacksonProperties = jacksonProperties;
			this.modules = modules;
		}

		@Override
		public int getOrder() {
			return 0;
		}

		protected JacksonProperties properties() {
			return this.jacksonProperties;
		}

		protected void customize(B builder) {
			if (this.jacksonProperties.isUseJackson2Defaults()) {
				builder.configureForJackson2()
					.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
			}
			if (this.jacksonProperties.isFindAndAddModules()) {
				builder.findAndAddModules(getClass().getClassLoader());
			}
			Include propertyInclusion = this.jacksonProperties.getDefaultPropertyInclusion();
			if (propertyInclusion != null) {
				builder.changeDefaultPropertyInclusion((handler) -> handler.withValueInclusion(propertyInclusion)
					.withContentInclusion(propertyInclusion));
			}
			if (this.jacksonProperties.getTimeZone() != null) {
				builder.defaultTimeZone(this.jacksonProperties.getTimeZone());
			}
			configureVisibility(builder, this.jacksonProperties.getVisibility());
			configureFeatures(builder, this.jacksonProperties.getDeserialization(), builder::configure);
			configureFeatures(builder, this.jacksonProperties.getSerialization(), builder::configure);
			configureFeatures(builder, this.jacksonProperties.getMapper(), builder::configure);
			configureFeatures(builder, this.jacksonProperties.getDatatype().getDatetime(), builder::configure);
			configureFeatures(builder, this.jacksonProperties.getDatatype().getEnum(), builder::configure);
			configureFeatures(builder, this.jacksonProperties.getDatatype().getJsonNode(), builder::configure);
			configureDateFormat(builder);
			configurePropertyNamingStrategy(builder);
			configureModules(builder);
			configureLocale(builder);
			configureDefaultLeniency(builder);
			configureConstructorDetector(builder);
		}

		protected <T> void configureFeatures(B builder, Map<T, Boolean> features, BiConsumer<T, Boolean> configure) {
			features.forEach((feature, value) -> {
				if (value != null) {
					configure.accept(feature, value);
				}
			});
		}

		private void configureVisibility(MapperBuilder<?, ?> builder,
				Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities) {
			builder.changeDefaultVisibility((visibilityChecker) -> {
				for (Map.Entry<PropertyAccessor, JsonAutoDetect.Visibility> entry : visibilities.entrySet()) {
					visibilityChecker = visibilityChecker.withVisibility(entry.getKey(), entry.getValue());
				}
				return visibilityChecker;
			});
		}

		private void configureDateFormat(MapperBuilder<?, ?> builder) {
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

		private void configurePropertyNamingStrategy(MapperBuilder<?, ?> builder) {
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

		private void configurePropertyNamingStrategyClass(MapperBuilder<?, ?> builder,
				Class<?> propertyNamingStrategyClass) {
			builder.propertyNamingStrategy(
					(PropertyNamingStrategy) BeanUtils.instantiateClass(propertyNamingStrategyClass));
		}

		private void configurePropertyNamingStrategyField(MapperBuilder<?, ?> builder, String fieldName) {
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

		private void configureModules(MapperBuilder<?, ?> builder) {
			builder.addModules(this.modules);
		}

		private void configureLocale(MapperBuilder<?, ?> builder) {
			Locale locale = this.jacksonProperties.getLocale();
			if (locale != null) {
				builder.defaultLocale(locale);
			}
		}

		private void configureDefaultLeniency(MapperBuilder<?, ?> builder) {
			Boolean defaultLeniency = this.jacksonProperties.getDefaultLeniency();
			if (defaultLeniency != null) {
				builder.defaultLeniency(defaultLeniency);
			}
		}

		private void configureConstructorDetector(MapperBuilder<?, ?> builder) {
			ConstructorDetectorStrategy strategy = this.jacksonProperties.getConstructorDetector();
			if (strategy != null) {
				switch (strategy) {
					case USE_PROPERTIES_BASED -> builder.constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
					case USE_DELEGATING -> builder.constructorDetector(ConstructorDetector.USE_DELEGATING);
					case EXPLICIT_ONLY -> builder.constructorDetector(ConstructorDetector.EXPLICIT_ONLY);
					default -> builder.constructorDetector(ConstructorDetector.DEFAULT);
				}
			}
		}

	}

}
