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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.cfg.ConstructorDetector.SingleArgConstructor;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.json.JsonMapper.Builder;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.util.StdDateFormat;
import tools.jackson.dataformat.cbor.CBORMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.module.kotlin.KotlinModule;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.boot.jackson.JacksonMixin;
import org.springframework.boot.jackson.JacksonMixinModule;
import org.springframework.boot.jackson.JacksonMixinModuleEntries;
import org.springframework.boot.jackson.ObjectValueSerializer;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.CborConfiguration.StandardCborMapperBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.JacksonAutoConfigurationRuntimeHints;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.JacksonJsonMapperBuilderCustomizerConfiguration.StandardJsonMapperBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.XmlConfiguration.StandardXmlMapperBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JacksonAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Sebastien Deleuze
 * @author Johannes Edmeier
 * @author Grzegorz Poznachowski
 * @author Ralf Ueberfuhr
 * @author Eddú Meléndez
 */
class JacksonAutoConfigurationTests {

	protected final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

	@EnumSource
	@ParameterizedTest
	void definesMapper(MapperType mapperType) {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(mapperType.mapperClass));
	}

	@EnumSource
	@ParameterizedTest
	void definesMapperBuilder(MapperType mapperType) {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(mapperType.builderClass));
	}

	@EnumSource
	@ParameterizedTest
	void mapperBacksOffWhenCustomMapperIsDefined(MapperType mapperType) {
		this.contextRunner.withBean("customMapper", mapperType.mapperClass).run((context) -> {
			assertThat(context).hasSingleBean(mapperType.mapperClass);
			assertThat(context).hasBean("customMapper");
		});
	}

	@EnumSource
	@ParameterizedTest
	void mapperDoesNotBackOffWhenObjectMapperIsDefined(MapperType mapperType) {
		this.contextRunner.withBean(ObjectMapper.class).run((context) -> {
			assertThat(context).hasSingleBean(mapperType.mapperClass);
			assertThat(context.getBeansOfType(ObjectMapper.class)).hasSize(MapperType.values().length + 1);
		});
	}

	@EnumSource
	@ParameterizedTest
	void mapperBuilderDoesNotBackOffWhenMapperIsDefined(MapperType mapperType) {
		this.contextRunner.withBean(mapperType.mapperClass)
			.run((context) -> assertThat(context).hasSingleBean(mapperType.builderClass));
	}

	@Test
	void standardJsonMapperBuilderCustomizerDoesNotBackOffWhenCustomizerIsDefined() {
		this.contextRunner.withBean(JsonMapperBuilderCustomizer.class, () -> mock(JsonMapperBuilderCustomizer.class))
			.run((context) -> assertThat(context).hasSingleBean(StandardJsonMapperBuilderCustomizer.class));
	}

	@Test
	void standardCborMapperBuilderCustomizerDoesNotBackOffWhenCustomizerIsDefined() {
		this.contextRunner.withBean(CborMapperBuilderCustomizer.class, () -> mock(CborMapperBuilderCustomizer.class))
			.run((context) -> assertThat(context).hasSingleBean(StandardCborMapperBuilderCustomizer.class));
	}

	@Test
	void standardXmlMapperBuilderCustomizerDoesNotBackOffWhenCustomizerIsDefined() {
		this.contextRunner.withBean(XmlMapperBuilderCustomizer.class, () -> mock(XmlMapperBuilderCustomizer.class))
			.run((context) -> assertThat(context).hasSingleBean(StandardXmlMapperBuilderCustomizer.class));
	}

	@Test
	void doubleModuleRegistration() {
		this.contextRunner.withUserConfiguration(DoubleModulesConfig.class).run((context) -> {
			JsonMapper mapper = context.getBean(JsonMapper.class);
			assertThat(mapper.writeValueAsString(new Foo())).isEqualTo("{\"foo\":\"bar\"}");
		});
	}

	@Test
	void jsonMixinModuleShouldBeAutoConfiguredWithBasePackages() {
		this.contextRunner.withUserConfiguration(MixinConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JacksonMixinModule.class).hasSingleBean(JacksonMixinModuleEntries.class);
			JacksonMixinModuleEntries moduleEntries = context.getBean(JacksonMixinModuleEntries.class);
			assertThat(moduleEntries).extracting("entries", InstanceOfAssertFactories.MAP)
				.contains(entry(Person.class, EmptyMixin.class));
		});
	}

	@EnumSource
	@ParameterizedTest
	void noCustomDateFormat(MapperType mapperType) {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(StdDateFormat.class);
			assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(StdDateFormat.class);
		});
	}

	@EnumSource
	@ParameterizedTest
	void customDateFormat(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.date-format:yyyyMMddHHmmss").run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			DateFormat serializationDateFormat = mapper.serializationConfig().getDateFormat();
			assertThat(serializationDateFormat).isInstanceOf(SimpleDateFormat.class);
			assertThat(((SimpleDateFormat) serializationDateFormat).toPattern()).isEqualTo("yyyyMMddHHmmss");
			DateFormat deserializationDateFormat = mapper.serializationConfig().getDateFormat();
			assertThat(deserializationDateFormat).isInstanceOf(SimpleDateFormat.class);
			assertThat(((SimpleDateFormat) deserializationDateFormat).toPattern()).isEqualTo("yyyyMMddHHmmss");
		});
	}

	@EnumSource
	@ParameterizedTest
	void customDateFormatClass(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.date-format:" + MyDateFormat.class.getName())
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
				assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
			});
	}

	@EnumSource
	@ParameterizedTest
	void noCustomPropertyNamingStrategy(MapperType mapperType) {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			assertThat(mapper.serializationConfig().getPropertyNamingStrategy()).isNull();
		});
	}

	@EnumSource
	@ParameterizedTest
	void customPropertyNamingStrategyField(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.property-naming-strategy:SNAKE_CASE").run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			assertThat(mapper.serializationConfig().getPropertyNamingStrategy()).isInstanceOf(SnakeCaseStrategy.class);
		});
	}

	@EnumSource
	@ParameterizedTest
	void customPropertyNamingStrategyClass(MapperType mapperType) {
		this.contextRunner.withPropertyValues(
				"spring.jackson.property-naming-strategy:tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy")
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				assertThat(mapper.serializationConfig().getPropertyNamingStrategy())
					.isInstanceOf(SnakeCaseStrategy.class);
			});
	}

	@EnumSource
	@ParameterizedTest
	void enableSerializationFeature(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.serialization.indent_output:true").run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			assertThat(SerializationFeature.INDENT_OUTPUT.enabledByDefault()).isFalse();
			assertThat(
					mapper.serializationConfig().hasSerializationFeatures(SerializationFeature.INDENT_OUTPUT.getMask()))
				.isTrue();
		});
	}

	@EnumSource
	@ParameterizedTest
	void disableSerializationFeature(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.serialization.wrap_exceptions:false").run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			assertThat(SerializationFeature.WRAP_EXCEPTIONS.enabledByDefault()).isTrue();
			assertThat(mapper.isEnabled(SerializationFeature.WRAP_EXCEPTIONS)).isFalse();
		});
	}

	@EnumSource
	@ParameterizedTest
	void enableDeserializationFeature(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.deserialization.use_big_decimal_for_floats:true")
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				assertThat(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.enabledByDefault()).isFalse();
				assertThat(mapper.deserializationConfig()
					.hasDeserializationFeatures(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.getMask())).isTrue();
			});
	}

	@EnumSource
	@ParameterizedTest
	void disableDeserializationFeature(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.deserialization.fail-on-null-for-primitives:false")
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				assertThat(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES.enabledByDefault()).isTrue();
				assertThat(mapper.deserializationConfig()
					.hasDeserializationFeatures(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES.getMask()))
					.isFalse();
			});
	}

	@EnumSource
	@ParameterizedTest
	void enableMapperFeature(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.mapper.require_setters_for_getters:true")
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				assertThat(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS.enabledByDefault()).isFalse();

				assertThat(mapper.serializationConfig().isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)).isTrue();
				assertThat(mapper.deserializationConfig().isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS))
					.isTrue();
			});
	}

	@EnumSource
	@ParameterizedTest
	void disableMapperFeature(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.mapper.infer-property-mutators:false").run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			assertThat(MapperFeature.INFER_PROPERTY_MUTATORS.enabledByDefault()).isTrue();
			assertThat(mapper.deserializationConfig().isEnabled(MapperFeature.INFER_PROPERTY_MUTATORS)).isFalse();
			assertThat(mapper.serializationConfig().isEnabled(MapperFeature.INFER_PROPERTY_MUTATORS)).isFalse();
		});
	}

	@Test
	void enableJsonReadFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.json.read.allow_single_quotes:true").run((context) -> {
			JsonMapper mapper = context.getBean(JsonMapper.class);
			assertThat(JsonReadFeature.ALLOW_SINGLE_QUOTES.enabledByDefault()).isFalse();
			assertThat(mapper.isEnabled(JsonReadFeature.ALLOW_SINGLE_QUOTES)).isTrue();
		});
	}

	@Test
	void enableJsonWriteFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.json.write.write_numbers_as_strings:true")
			.run((context) -> {
				JsonMapper mapper = context.getBean(JsonMapper.class);
				JsonWriteFeature feature = JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS;
				assertThat(feature.enabledByDefault()).isFalse();
				assertThat(mapper.isEnabled(feature)).isTrue();
			});
	}

	@Test
	void disableJsonWriteFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.json.write.write_hex_upper_case:false").run((context) -> {
			JsonMapper mapper = context.getBean(JsonMapper.class);
			assertThat(JsonWriteFeature.WRITE_HEX_UPPER_CASE.enabledByDefault()).isTrue();
			assertThat(mapper.isEnabled(JsonWriteFeature.WRITE_HEX_UPPER_CASE)).isFalse();
		});
	}

	@EnumSource
	@ParameterizedTest
	void enableDatetimeFeature(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.datatype.datetime.write-dates-as-timestamps:true")
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				DateTimeFeature feature = DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;
				assertThat(feature.enabledByDefault()).isFalse();
				assertThat(mapper.isEnabled(feature)).isTrue();
			});
	}

	@EnumSource
	@ParameterizedTest
	void disableDatetimeFeature(MapperType mapperType) {
		this.contextRunner
			.withPropertyValues("spring.jackson.datatype.datetime.adjust-dates-to-context-time-zone:false")
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				assertThat(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE.enabledByDefault()).isTrue();
				assertThat(mapper.isEnabled(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)).isFalse();
			});
	}

	@EnumSource
	@ParameterizedTest
	void enableEnumFeature(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.datatype.enum.write-enums-to-lowercase=true")
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				assertThat(EnumFeature.WRITE_ENUMS_TO_LOWERCASE.enabledByDefault()).isFalse();
				assertThat(mapper.serializationConfig().isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE)).isTrue();
			});
	}

	@EnumSource
	@ParameterizedTest
	void disableJsonNodeFeature(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.datatype.json-node.write-null-properties:false")
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				assertThat(JsonNodeFeature.WRITE_NULL_PROPERTIES.enabledByDefault()).isTrue();
				assertThat(mapper.deserializationConfig().isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES)).isFalse();
			});
	}

	@EnumSource
	@ParameterizedTest
	void moduleBeansAndWellKnownModulesAreRegisteredWithTheMapperBuilder(MapperType mapperType) {
		this.contextRunner.withUserConfiguration(ModuleConfig.class).run((context) -> {
			Builder jsonMapperBuilder = context.getBean(JsonMapper.Builder.class);
			JsonMapper jsonMapper = jsonMapperBuilder.build();
			assertThat(context.getBean(CustomModule.class).getOwners()).contains(jsonMapperBuilder);
			assertThat(jsonMapper._serializationContext().findValueSerializer(Baz.class)).isNotNull();
		});
	}

	@Test
	void customModulesRegisteredByJsonMapperBuilderCustomizerShouldBeRetained() {
		this.contextRunner
			.withUserConfiguration(ModuleConfig.class, CustomModuleJsonMapperBuilderCustomizerConfig.class)
			.run((context) -> {
				JsonMapper jsonMapper = context.getBean(JsonMapper.class);
				assertThat(jsonMapper.registeredModules()).extracting(JacksonModule::getModuleName)
					.contains("module-A", "module-B", CustomModule.class.getName());
			});
	}

	@Test
	void customModulesRegisteredByCborMapperBuilderCustomizerShouldBeRetained() {
		this.contextRunner
			.withUserConfiguration(ModuleConfig.class, CustomModuleCborMapperBuilderCustomizerConfig.class)
			.run((context) -> {
				CBORMapper mapper = context.getBean(CBORMapper.class);
				assertThat(mapper.registeredModules()).extracting(JacksonModule::getModuleName)
					.contains("module-A", "module-B", CustomModule.class.getName());
			});
	}

	@Test
	void customModulesRegisteredByXmlMapperBuilderCustomizerShouldBeRetained() {
		this.contextRunner.withUserConfiguration(ModuleConfig.class, CustomModuleXmlMapperBuilderCustomizerConfig.class)
			.run((context) -> {
				XmlMapper mapper = context.getBean(XmlMapper.class);
				assertThat(mapper.registeredModules()).extracting(JacksonModule::getModuleName)
					.contains("module-A", "module-B", CustomModule.class.getName());
			});
	}

	@EnumSource
	@ParameterizedTest
	void defaultSerializationInclusion(MapperType mapperType) {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			assertThat(mapper.serializationConfig().getDefaultPropertyInclusion().getValueInclusion())
				.isEqualTo(JsonInclude.Include.USE_DEFAULTS);
		});
	}

	@EnumSource
	@ParameterizedTest
	void customSerializationInclusion(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.default-property-inclusion:non_null").run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			assertThat(mapper.serializationConfig().getDefaultPropertyInclusion().getValueInclusion())
				.isEqualTo(JsonInclude.Include.NON_NULL);
		});
	}

	@Test
	void customTimeZoneFormattingADateToJson() {
		this.contextRunner.withPropertyValues("spring.jackson.time-zone:GMT+10", "spring.jackson.date-format:z")
			.run((context) -> {
				JsonMapper mapper = context.getBean(JsonMapper.class);
				Date date = new Date(1436966242231L);
				assertThat(mapper.writeValueAsString(date)).isEqualTo("\"GMT+10:00\"");
			});
	}

	@Test
	void customTimeZoneFormattingADateToCbor() {
		this.contextRunner.withPropertyValues("spring.jackson.time-zone:GMT+10", "spring.jackson.date-format:z")
			.run((context) -> {
				CBORMapper mapper = context.getBean(CBORMapper.class);
				Date date = new Date(1436966242231L);
				assertThat(mapper.writeValueAsBytes(date))
					.isEqualTo(new byte[] { 105, 71, 77, 84, 43, 49, 48, 58, 48, 48 });
			});
	}

	@Test
	void customTimeZoneFormattingADateToXml() {
		this.contextRunner.withPropertyValues("spring.jackson.time-zone:GMT+10", "spring.jackson.date-format:z")
			.run((context) -> {
				XmlMapper mapper = context.getBean(XmlMapper.class);
				Date date = new Date(1436966242231L);
				assertThat(mapper.writeValueAsString(date)).isEqualTo("<Date>GMT+10:00</Date>");
			});
	}

	@EnumSource
	@ParameterizedTest
	void enableDefaultLeniency(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.default-leniency:true").run((context) -> {
			ObjectMapper mapper = context.getBean(mapperType.mapperClass);
			byte[] source = mapper.writeValueAsBytes(Map.of("birthDate", "2010-12-30"));
			Person person = mapper.readValue(source, Person.class);
			assertThat(person.getBirthDate()).isNotNull();
		});
	}

	@EnumSource
	@ParameterizedTest
	void disableDefaultLeniency(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.default-leniency:false").run((context) -> {
			ObjectMapper mapper = context.getBean(mapperType.mapperClass);
			byte[] source = mapper.writeValueAsBytes(Map.of("birthDate", "2010-12-30"));
			assertThatExceptionOfType(InvalidFormatException.class)
				.isThrownBy(() -> mapper.readValue(source, Person.class))
				.withMessageContaining("expected format")
				.withMessageContaining("yyyyMMdd");
		});
	}

	@EnumSource
	@ParameterizedTest
	void constructorDetectorWithNoStrategyUseDefault(MapperType mapperType) {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.HEURISTIC);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@EnumSource
	@ParameterizedTest
	void constructorDetectorWithDefaultStrategy(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=default").run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.HEURISTIC);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@EnumSource
	@ParameterizedTest
	void constructorDetectorWithUsePropertiesBasedStrategy(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=use-properties-based")
			.run((context) -> {
				ObjectMapper mapper = mapperType.getMapper(context);
				ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
				assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.PROPERTIES);
				assertThat(cd.requireCtorAnnotation()).isFalse();
				assertThat(cd.allowJDKTypeConstructors()).isFalse();
			});
	}

	@EnumSource
	@ParameterizedTest
	void constructorDetectorWithUseDelegatingStrategy(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=use-delegating").run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.DELEGATING);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@EnumSource
	@ParameterizedTest
	void constructorDetectorWithExplicitOnlyStrategy(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=explicit-only").run((context) -> {
			ObjectMapper mapper = mapperType.getMapper(context);
			ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.REQUIRE_MODE);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@Test
	void additionalJsonMapperBuilderCustomization() {
		this.contextRunner.withBean(JsonMapperBuilderCustomizer.class, () -> null)
			.withUserConfiguration(JsonMapperBuilderCustomConfig.class)
			.run((context) -> {
				JsonMapper mapper = context.getBean(JsonMapper.class);
				assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
				assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
			});
	}

	@Test
	void additionalCborMapperBuilderCustomization() {
		this.contextRunner.withBean(CborMapperBuilderCustomizer.class, () -> null)
			.withUserConfiguration(CborMapperBuilderCustomConfig.class)
			.run((context) -> {
				CBORMapper mapper = context.getBean(CBORMapper.class);
				assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
				assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
			});
	}

	@Test
	void additionalXmlMapperBuilderCustomization() {
		this.contextRunner.withBean(XmlMapperBuilderCustomizer.class, () -> null)
			.withUserConfiguration(XmlMapperBuilderCustomConfig.class)
			.run((context) -> {
				XmlMapper mapper = context.getBean(XmlMapper.class);
				assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
				assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
			});
	}

	@EnumSource
	@ParameterizedTest
	void writeDurationAsTimestampsDefault(MapperType mapperType) {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = context.getBean(mapperType.mapperClass);
			Duration duration = Duration.ofHours(2);
			String written = mapper.readerFor(String.class).readValue(mapper.writeValueAsBytes(duration));
			assertThat(written).isEqualTo("PT2H");
		});
	}

	@EnumSource
	@ParameterizedTest
	@SuppressWarnings("unchecked")
	void writeWithVisibility(MapperType mapperType) {
		this.contextRunner
			.withPropertyValues("spring.jackson.visibility.getter:none", "spring.jackson.visibility.field:any")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(mapperType.mapperClass);
				Map<String, ?> written = mapper.readValue(mapper.writeValueAsBytes(new VisibilityBean()), Map.class);
				assertThat(written).containsOnlyKeys("property1", "property2");
			});
	}

	@Test
	void jsonMapperBuilderIsNotSharedAcrossMultipleInjectionPoints() {
		this.contextRunner.withUserConfiguration(JsonMapperBuilderConsumerConfig.class).run((context) -> {
			JsonMapperBuilderConsumerConfig consumer = context.getBean(JsonMapperBuilderConsumerConfig.class);
			assertThat(consumer.builderOne).isNotNull();
			assertThat(consumer.builderTwo).isNotNull();
			assertThat(consumer.builderOne).isNotSameAs(consumer.builderTwo);
		});
	}

	@Test
	void cborMapperBuilderIsNotSharedAcrossMultipleInjectionPoints() {
		this.contextRunner.withUserConfiguration(CborMapperBuilderConsumerConfig.class).run((context) -> {
			CborMapperBuilderConsumerConfig consumer = context.getBean(CborMapperBuilderConsumerConfig.class);
			assertThat(consumer.builderOne).isNotNull();
			assertThat(consumer.builderTwo).isNotNull();
			assertThat(consumer.builderOne).isNotSameAs(consumer.builderTwo);
		});
	}

	@Test
	void xmlMapperBuilderIsNotSharedAcrossMultipleInjectionPoints() {
		this.contextRunner.withUserConfiguration(XmlMapperBuilderConsumerConfig.class).run((context) -> {
			XmlMapperBuilderConsumerConfig consumer = context.getBean(XmlMapperBuilderConsumerConfig.class);
			assertThat(consumer.builderOne).isNotNull();
			assertThat(consumer.builderTwo).isNotNull();
			assertThat(consumer.builderOne).isNotSameAs(consumer.builderTwo);
		});
	}

	@Test
	void jacksonComponentThatInjectsJsonMapperCausesBeanCurrentlyInCreationException() {
		this.contextRunner.withUserConfiguration(CircularDependencySerializerConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure().hasRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
		});
	}

	@Test
	void shouldRegisterPropertyNamingStrategyHints() {
		RuntimeHints hints = new RuntimeHints();
		new JacksonAutoConfigurationRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(PropertyNamingStrategies.class)).accepts(hints);
	}

	@Test
	void shouldRegisterProblemDetailsMixinWithJsonMapper() {
		this.contextRunner.run((context) -> {
			JsonMapper mapper = context.getBean(JsonMapper.class);
			ProblemDetail problemDetail = ProblemDetail.forStatus(404);
			problemDetail.setProperty("spring", "boot");
			String json = mapper.writeValueAsString(problemDetail);
			assertThat(json).isEqualTo("{\"status\":404,\"title\":\"Not Found\",\"spring\":\"boot\"}");
		});
	}

	@Test
	void shouldRegisterProblemDetailsMixinWithXmlMapper() {
		this.contextRunner.run((context) -> {
			XmlMapper mapper = context.getBean(XmlMapper.class);
			ProblemDetail problemDetail = ProblemDetail.forStatus(404);
			problemDetail.setProperty("spring", "boot");
			String xml = mapper.writeValueAsString(problemDetail);
			assertThat(xml).isEqualTo(
					"<problem xmlns=\"urn:ietf:rfc:7807\"><status>404</status><title>Not Found</title><spring>boot</spring></problem>");
		});
	}

	@Test
	void whenUsingJackson2DefaultsJsonMapperShouldBeConfiguredUsingConfigureForJackson2() {
		this.contextRunner.withPropertyValues("spring.jackson.use-jackson2-defaults=true").run((context) -> {
			JsonMapper jsonMapper = context.getBean(JsonMapper.class);
			JsonMapper jackson2ConfiguredJsonMapper = JsonMapper.builder().configureForJackson2().build();
			assertCommonFeatureConfiguration(jsonMapper, jackson2ConfiguredJsonMapper);
			for (JsonReadFeature feature : JsonReadFeature.values()) {
				assertThat(jsonMapper.isEnabled(feature)).as(feature.name())
					.isEqualTo(jackson2ConfiguredJsonMapper.isEnabled(feature));
			}
			for (JsonWriteFeature feature : JsonWriteFeature.values()) {
				assertThat(jsonMapper.isEnabled(feature)).as(feature.name())
					.isEqualTo(jackson2ConfiguredJsonMapper.isEnabled(feature));
			}
		});
	}

	@Test
	void whenUsingJackson2DefaultsCborMapperShouldBeConfiguredUsingConfigureForJackson2() {
		this.contextRunner.withPropertyValues("spring.jackson.use-jackson2-defaults=true").run((context) -> {
			CBORMapper cborMapper = context.getBean(CBORMapper.class);
			CBORMapper jackson2ConfiguredCborMapper = CBORMapper.builder().configureForJackson2().build();
			assertCommonFeatureConfiguration(cborMapper, jackson2ConfiguredCborMapper);
			assertThat(cborMapper.deserializationConfig().getFormatReadFeatures())
				.isEqualTo(jackson2ConfiguredCborMapper.deserializationConfig().getFormatReadFeatures());
			assertThat(cborMapper.serializationConfig().getFormatWriteFeatures())
				.isEqualTo(jackson2ConfiguredCborMapper.serializationConfig().getFormatWriteFeatures());
		});
	}

	@Test
	void whenUsingJackson2DefaultsXmlMapperShouldBeConfiguredUsingConfigureForJackson2() {
		this.contextRunner.withPropertyValues("spring.jackson.use-jackson2-defaults=true").run((context) -> {
			XmlMapper xmlMapper = context.getBean(XmlMapper.class);
			XmlMapper jackson2ConfiguredXmlMapper = XmlMapper.builder().configureForJackson2().build();
			assertCommonFeatureConfiguration(xmlMapper, jackson2ConfiguredXmlMapper);
			assertThat(xmlMapper.deserializationConfig().getFormatReadFeatures())
				.isEqualTo(jackson2ConfiguredXmlMapper.deserializationConfig().getFormatReadFeatures());
			assertThat(xmlMapper.serializationConfig().getFormatWriteFeatures())
				.isEqualTo(jackson2ConfiguredXmlMapper.serializationConfig().getFormatWriteFeatures());
		});
	}

	private void assertCommonFeatureConfiguration(ObjectMapper mapper, ObjectMapper jackson2ConfiguredMapper) {
		for (DateTimeFeature feature : DateTimeFeature.values()) {
			boolean expected = (feature == DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS
					|| feature == DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS) ? false
							: jackson2ConfiguredMapper.isEnabled(feature);
			assertThat(mapper.isEnabled(feature)).as(feature.name()).isEqualTo(expected);
		}
		for (DeserializationFeature feature : DeserializationFeature.values()) {
			assertThat(mapper.isEnabled(feature)).as(feature.name())
				.isEqualTo(jackson2ConfiguredMapper.isEnabled(feature));
		}
		for (EnumFeature feature : EnumFeature.values()) {
			assertThat(mapper.isEnabled(feature)).as(feature.name())
				.isEqualTo(jackson2ConfiguredMapper.isEnabled(feature));
		}
		for (JsonNodeFeature feature : JsonNodeFeature.values()) {
			assertThat(mapper.isEnabled(feature)).as(feature.name())
				.isEqualTo(jackson2ConfiguredMapper.isEnabled(feature));
		}
		for (MapperFeature feature : MapperFeature.values()) {
			assertThat(mapper.isEnabled(feature)).as(feature.name())
				.isEqualTo(jackson2ConfiguredMapper.isEnabled(feature));
		}
		for (SerializationFeature feature : SerializationFeature.values()) {
			assertThat(mapper.isEnabled(feature)).as(feature.name())
				.isEqualTo(jackson2ConfiguredMapper.isEnabled(feature));
		}
		for (StreamReadFeature feature : StreamReadFeature.values()) {
			assertThat(mapper.isEnabled(feature)).as(feature.name())
				.isEqualTo(jackson2ConfiguredMapper.isEnabled(feature));
		}
		for (StreamWriteFeature feature : StreamWriteFeature.values()) {
			assertThat(mapper.isEnabled(feature)).as(feature.name())
				.isEqualTo(jackson2ConfiguredMapper.isEnabled(feature));
		}
	}

	@EnumSource
	@ParameterizedTest
	void shouldFindAndAddModulesByDefault(MapperType mapperType) {
		this.contextRunner.run((context) -> assertThat(mapperType.getMapper(context).registeredModules())
			.hasAtLeastOneElementOfType(KotlinModule.class));
	}

	@EnumSource
	@ParameterizedTest
	void shouldNotFindAndAddModulesWhenDisabled(MapperType mapperType) {
		this.contextRunner.withPropertyValues("spring.jackson.find-and-add-modules=false")
			.run((context) -> assertThat(mapperType.getMapper(context).registeredModules())
				.doesNotHaveAnyElementsOfTypes(KotlinModule.class));
	}

	static class MyDateFormat extends SimpleDateFormat {

		MyDateFormat() {
			super("yyyy-MM-dd HH:mm:ss");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MockObjectMapperConfig {

		@Bean
		@Primary
		ObjectMapper objectMapper() {
			return mock(ObjectMapper.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BazSerializer.class)
	static class ModuleConfig {

		@Bean
		CustomModule jacksonModule() {
			return new CustomModule();
		}

	}

	@Configuration
	static class DoubleModulesConfig {

		@Bean
		JacksonModule jacksonModule() {
			SimpleModule module = new SimpleModule();
			module.addSerializer(Foo.class, new ValueSerializer<>() {

				@Override
				public void serialize(Foo value, JsonGenerator jgen, SerializationContext context) {
					jgen.writeStartObject();
					jgen.writeStringProperty("foo", "bar");
					jgen.writeEndObject();
				}
			});
			return module;
		}

		@Bean
		@Primary
		JsonMapper jsonMapper() {
			return JsonMapper.builder().addModule(jacksonModule()).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JsonMapperBuilderCustomConfig {

		@Bean
		JsonMapperBuilderCustomizer customDateFormat() {
			return (builder) -> builder.defaultDateFormat(new MyDateFormat());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CborMapperBuilderCustomConfig {

		@Bean
		CborMapperBuilderCustomizer customDateFormat() {
			return (builder) -> builder.defaultDateFormat(new MyDateFormat());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class XmlMapperBuilderCustomConfig {

		@Bean
		XmlMapperBuilderCustomizer customDateFormat() {
			return (builder) -> builder.defaultDateFormat(new MyDateFormat());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomModuleJsonMapperBuilderCustomizerConfig {

		@Bean
		@Order(-1)
		JsonMapperBuilderCustomizer highPrecedenceCustomizer() {
			return (builder) -> builder.addModule(new SimpleModule("module-A"));
		}

		@Bean
		@Order(1)
		JsonMapperBuilderCustomizer lowPrecedenceCustomizer() {
			return (builder) -> builder.addModule(new SimpleModule("module-B"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomModuleCborMapperBuilderCustomizerConfig {

		@Bean
		@Order(-1)
		CborMapperBuilderCustomizer highPrecedenceCustomizer() {
			return (builder) -> builder.addModule(new SimpleModule("module-A"));
		}

		@Bean
		@Order(1)
		CborMapperBuilderCustomizer lowPrecedenceCustomizer() {
			return (builder) -> builder.addModule(new SimpleModule("module-B"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomModuleXmlMapperBuilderCustomizerConfig {

		@Bean
		@Order(-1)
		XmlMapperBuilderCustomizer highPrecedenceCustomizer() {
			return (builder) -> builder.addModule(new SimpleModule("module-A"));
		}

		@Bean
		@Order(1)
		XmlMapperBuilderCustomizer lowPrecedenceCustomizer() {
			return (builder) -> builder.addModule(new SimpleModule("module-B"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JsonMapperBuilderConsumerConfig {

		JsonMapper.@Nullable Builder builderOne;

		JsonMapper.@Nullable Builder builderTwo;

		@Bean
		String consumerOne(JsonMapper.Builder builder) {
			this.builderOne = builder;
			return "one";
		}

		@Bean
		String consumerTwo(JsonMapper.Builder builder) {
			this.builderTwo = builder;
			return "two";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CborMapperBuilderConsumerConfig {

		CBORMapper.@Nullable Builder builderOne;

		CBORMapper.@Nullable Builder builderTwo;

		@Bean
		String consumerOne(CBORMapper.Builder builder) {
			this.builderOne = builder;
			return "one";
		}

		@Bean
		String consumerTwo(CBORMapper.Builder builder) {
			this.builderTwo = builder;
			return "two";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class XmlMapperBuilderConsumerConfig {

		XmlMapper.@Nullable Builder builderOne;

		XmlMapper.@Nullable Builder builderTwo;

		@Bean
		String consumerOne(XmlMapper.Builder builder) {
			this.builderOne = builder;
			return "one";
		}

		@Bean
		String consumerTwo(XmlMapper.Builder builder) {
			this.builderTwo = builder;
			return "two";
		}

	}

	protected static final class Foo {

		private @Nullable String name;

		private Foo() {
		}

		static Foo create() {
			return new Foo();
		}

		public @Nullable String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

	}

	static class Bar {

		private @Nullable String propertyName;

		@Nullable String getPropertyName() {
			return this.propertyName;
		}

		void setPropertyName(@Nullable String propertyName) {
			this.propertyName = propertyName;
		}

	}

	@JacksonComponent
	static class BazSerializer extends ObjectValueSerializer<Baz> {

		@Override
		protected void serializeObject(Baz value, JsonGenerator jgen, SerializationContext context) {
		}

	}

	static class Baz {

	}

	static class CustomModule extends SimpleModule {

		private final Set<Object> owners = new HashSet<>();

		@Override
		public void setupModule(SetupContext context) {
			this.owners.add(context.getOwner());
		}

		Set<Object> getOwners() {
			return this.owners;
		}

	}

	@SuppressWarnings("unused")
	static class VisibilityBean {

		private @Nullable String property1;

		public @Nullable String property2;

		@Nullable String getProperty3() {
			return null;
		}

	}

	static class Person {

		@JsonFormat(pattern = "yyyyMMdd")
		private @Nullable Date birthDate;

		@Nullable Date getBirthDate() {
			return this.birthDate;
		}

		void setBirthDate(@Nullable Date birthDate) {
			this.birthDate = birthDate;
		}

	}

	@JacksonMixin(type = Person.class)
	static class EmptyMixin {

	}

	@AutoConfigurationPackage
	static class MixinConfiguration {

	}

	@JacksonComponent
	static class CircularDependencySerializer extends ValueSerializer<String> {

		CircularDependencySerializer(JsonMapper jsonMapper) {

		}

		@Override
		public void serialize(String value, JsonGenerator gen, SerializationContext context) {

		}

	}

	@Import(CircularDependencySerializer.class)
	@Configuration(proxyBeanMethods = false)
	static class CircularDependencySerializerConfiguration {

	}

	enum MapperType {

		CBOR(CBORMapper.class, CBORMapper.Builder.class), JSON(JsonMapper.class, JsonMapper.Builder.class),
		XML(XmlMapper.class, XmlMapper.Builder.class);

		private final Class<? extends ObjectMapper> mapperClass;

		private final Class<? extends MapperBuilder<?, ?>> builderClass;

		<M extends ObjectMapper, B extends MapperBuilder<M, B>> MapperType(Class<M> mapperClass,
				Class<B> builderClass) {
			this.mapperClass = mapperClass;
			this.builderClass = builderClass;
		}

		ObjectMapper getMapper(ApplicationContext context) {
			return context.getBean(this.mapperClass);
		}

	}

}
