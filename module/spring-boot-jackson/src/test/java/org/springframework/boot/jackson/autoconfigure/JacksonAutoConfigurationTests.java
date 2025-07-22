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
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
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
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.json.JsonMapper.Builder;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.util.StdDateFormat;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.boot.jackson.JsonMixin;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.boot.jackson.JsonMixinModuleEntries;
import org.springframework.boot.jackson.ObjectValueSerializer;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.JacksonAutoConfigurationRuntimeHints;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

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
@SuppressWarnings("removal")
class JacksonAutoConfigurationTests {

	protected final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

	@Test
	void doubleModuleRegistration() {
		this.contextRunner.withUserConfiguration(DoubleModulesConfig.class).run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.writeValueAsString(new Foo())).isEqualTo("{\"foo\":\"bar\"}");
		});
	}

	@Test
	void jsonMixinModuleShouldBeAutoConfiguredWithBasePackages() {
		this.contextRunner.withUserConfiguration(MixinConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JsonMixinModule.class).hasSingleBean(JsonMixinModuleEntries.class);
			JsonMixinModuleEntries moduleEntries = context.getBean(JsonMixinModuleEntries.class);
			assertThat(moduleEntries).extracting("entries", InstanceOfAssertFactories.MAP)
				.contains(entry(Person.class, EmptyMixin.class));
		});
	}

	@Test
	void noCustomDateFormat() {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(StdDateFormat.class);
			assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(StdDateFormat.class);
		});
	}

	@Test
	void customDateFormat() {
		this.contextRunner.withPropertyValues("spring.jackson.date-format:yyyyMMddHHmmss").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			DateFormat serializationDateFormat = mapper.serializationConfig().getDateFormat();
			assertThat(serializationDateFormat).isInstanceOf(SimpleDateFormat.class);
			assertThat(((SimpleDateFormat) serializationDateFormat).toPattern()).isEqualTo("yyyyMMddHHmmss");
			DateFormat deserializationDateFormat = mapper.serializationConfig().getDateFormat();
			assertThat(deserializationDateFormat).isInstanceOf(SimpleDateFormat.class);
			assertThat(((SimpleDateFormat) deserializationDateFormat).toPattern()).isEqualTo("yyyyMMddHHmmss");
		});
	}

	@Test
	void customDateFormatClass() {
		this.contextRunner.withPropertyValues("spring.jackson.date-format:" + MyDateFormat.class.getName())
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
				assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
			});
	}

	@Test
	void noCustomPropertyNamingStrategy() {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.serializationConfig().getPropertyNamingStrategy()).isNull();
		});
	}

	@Test
	void customPropertyNamingStrategyField() {
		this.contextRunner.withPropertyValues("spring.jackson.property-naming-strategy:SNAKE_CASE").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.serializationConfig().getPropertyNamingStrategy()).isInstanceOf(SnakeCaseStrategy.class);
		});
	}

	@Test
	void customPropertyNamingStrategyClass() {
		this.contextRunner.withPropertyValues(
				"spring.jackson.property-naming-strategy:tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(mapper.serializationConfig().getPropertyNamingStrategy())
					.isInstanceOf(SnakeCaseStrategy.class);
			});
	}

	@Test
	void enableSerializationFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.serialization.indent_output:true").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(SerializationFeature.INDENT_OUTPUT.enabledByDefault()).isFalse();
			assertThat(
					mapper.serializationConfig().hasSerializationFeatures(SerializationFeature.INDENT_OUTPUT.getMask()))
				.isTrue();
		});
	}

	@Test
	void disableSerializationFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.serialization.wrap_exceptions:false").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(SerializationFeature.WRAP_EXCEPTIONS.enabledByDefault()).isTrue();
			assertThat(mapper.isEnabled(SerializationFeature.WRAP_EXCEPTIONS)).isFalse();
		});
	}

	@Test
	void enableDeserializationFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.deserialization.use_big_decimal_for_floats:true")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.enabledByDefault()).isFalse();
				assertThat(mapper.deserializationConfig()
					.hasDeserializationFeatures(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.getMask())).isTrue();
			});
	}

	@Test
	void disableDeserializationFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.deserialization.fail-on-null-for-primitives:false")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES.enabledByDefault()).isTrue();
				assertThat(mapper.deserializationConfig()
					.hasDeserializationFeatures(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES.getMask()))
					.isFalse();
			});
	}

	@Test
	void enableMapperFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.mapper.require_setters_for_getters:true")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS.enabledByDefault()).isFalse();

				assertThat(mapper.serializationConfig().isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)).isTrue();
				assertThat(mapper.deserializationConfig().isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS))
					.isTrue();
			});
	}

	@Test
	void disableMapperFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.mapper.use_annotations:false").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(MapperFeature.USE_ANNOTATIONS.enabledByDefault()).isTrue();
			assertThat(mapper.deserializationConfig().isEnabled(MapperFeature.USE_ANNOTATIONS)).isFalse();
			assertThat(mapper.serializationConfig().isEnabled(MapperFeature.USE_ANNOTATIONS)).isFalse();
		});
	}

	@Test
	void enableReadFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.read.allow_single_quotes:true").run((context) -> {
			JsonMapper mapper = context.getBean(JsonMapper.class);
			assertThat(JsonReadFeature.ALLOW_SINGLE_QUOTES.enabledByDefault()).isFalse();
			assertThat(mapper.isEnabled(JsonReadFeature.ALLOW_SINGLE_QUOTES)).isTrue();
		});
	}

	@Test
	void enableWriteFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.write.write_numbers_as_strings:true").run((context) -> {
			JsonMapper mapper = context.getBean(JsonMapper.class);
			JsonWriteFeature feature = JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS;
			assertThat(feature.enabledByDefault()).isFalse();
			assertThat(mapper.isEnabled(feature)).isTrue();
		});
	}

	@Test
	void disableWriteFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.write.write_hex_upper_case:false").run((context) -> {
			JsonMapper mapper = context.getBean(JsonMapper.class);
			assertThat(JsonWriteFeature.WRITE_HEX_UPPER_CASE.enabledByDefault()).isTrue();
			assertThat(mapper.isEnabled(JsonWriteFeature.WRITE_HEX_UPPER_CASE)).isFalse();
		});
	}

	@Test
	void enableEnumFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.datatype.enum.write-enums-to-lowercase=true")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(EnumFeature.WRITE_ENUMS_TO_LOWERCASE.enabledByDefault()).isFalse();
				assertThat(mapper.serializationConfig().isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE)).isTrue();
			});
	}

	@Test
	void disableJsonNodeFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.datatype.json-node.write-null-properties:false")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(JsonNodeFeature.WRITE_NULL_PROPERTIES.enabledByDefault()).isTrue();
				assertThat(mapper.deserializationConfig().isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES)).isFalse();
			});
	}

	@Test
	void moduleBeansAndWellKnownModulesAreRegisteredWithTheJsonMapperBuilder() {
		this.contextRunner.withUserConfiguration(ModuleConfig.class).run((context) -> {
			Builder jsonMapperBuilder = context.getBean(JsonMapper.Builder.class);
			JsonMapper jsonMapper = jsonMapperBuilder.build();
			assertThat(context.getBean(CustomModule.class).getOwners()).contains(jsonMapperBuilder);
			assertThat(jsonMapper._serializationContext().findValueSerializer(Baz.class)).isNotNull();
		});
	}

	@Test
	void customModulesRegisteredByBuilderCustomizerShouldBeRetained() {
		this.contextRunner.withUserConfiguration(ModuleConfig.class, CustomModuleBuilderCustomizerConfig.class)
			.run((context) -> {
				JsonMapper jsonMapper = context.getBean(JsonMapper.class);
				assertThat(jsonMapper.getRegisteredModules()).extracting((module) -> module.getModuleName())
					.contains("module-A", "module-B", CustomModule.class.getName());
			});
	}

	@Test
	void defaultSerializationInclusion() {
		this.contextRunner.run((context) -> {
			ObjectMapper objectMapper = context.getBean(JsonMapper.Builder.class).build();
			assertThat(objectMapper.serializationConfig().getDefaultPropertyInclusion().getValueInclusion())
				.isEqualTo(JsonInclude.Include.USE_DEFAULTS);
		});
	}

	@Test
	void customSerializationInclusion() {
		this.contextRunner.withPropertyValues("spring.jackson.default-property-inclusion:non_null").run((context) -> {
			ObjectMapper objectMapper = context.getBean(JsonMapper.Builder.class).build();
			assertThat(objectMapper.serializationConfig().getDefaultPropertyInclusion().getValueInclusion())
				.isEqualTo(JsonInclude.Include.NON_NULL);
		});
	}

	@Test
	void customTimeZoneFormattingADate() {
		this.contextRunner.withPropertyValues("spring.jackson.time-zone:GMT+10", "spring.jackson.date-format:z")
			.run((context) -> {
				ObjectMapper objectMapper = context.getBean(JsonMapper.Builder.class).build();
				Date date = new Date(1436966242231L);
				assertThat(objectMapper.writeValueAsString(date)).isEqualTo("\"GMT+10:00\"");
			});
	}

	@Test
	void enableDefaultLeniency() {
		this.contextRunner.withPropertyValues("spring.jackson.default-leniency:true").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			Person person = mapper.readValue("{\"birthDate\": \"2010-12-30\"}", Person.class);
			assertThat(person.getBirthDate()).isNotNull();
		});
	}

	@Test
	void disableDefaultLeniency() {
		this.contextRunner.withPropertyValues("spring.jackson.default-leniency:false").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThatExceptionOfType(InvalidFormatException.class)
				.isThrownBy(() -> mapper.readValue("{\"birthDate\": \"2010-12-30\"}", Person.class))
				.withMessageContaining("expected format")
				.withMessageContaining("yyyyMMdd");
		});
	}

	@Test
	void constructorDetectorWithNoStrategyUseDefault() {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.HEURISTIC);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@Test
	void constructorDetectorWithDefaultStrategy() {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=default").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.HEURISTIC);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@Test
	void constructorDetectorWithUsePropertiesBasedStrategy() {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=use-properties-based")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
				assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.PROPERTIES);
				assertThat(cd.requireCtorAnnotation()).isFalse();
				assertThat(cd.allowJDKTypeConstructors()).isFalse();
			});
	}

	@Test
	void constructorDetectorWithUseDelegatingStrategy() {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=use-delegating").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.DELEGATING);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@Test
	void constructorDetectorWithExplicitOnlyStrategy() {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=explicit-only").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.REQUIRE_MODE);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@Test
	void additionalJacksonBuilderCustomization() {
		this.contextRunner.withUserConfiguration(JsonMapperBuilderCustomConfig.class).run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
			assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
		});
	}

	@Test
	void writeDurationAsTimestampsDefault() {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			Duration duration = Duration.ofHours(2);
			assertThat(mapper.writeValueAsString(duration)).isEqualTo("\"PT2H\"");
		});
	}

	@Test
	void writeWithVisibility() {
		this.contextRunner
			.withPropertyValues("spring.jackson.visibility.getter:none", "spring.jackson.visibility.field:any")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				String json = mapper.writeValueAsString(new VisibilityBean());
				assertThat(json).contains("property1");
				assertThat(json).contains("property2");
				assertThat(json).doesNotContain("property3");
			});
	}

	@Test
	void builderIsNotSharedAcrossMultipleInjectionPoints() {
		this.contextRunner.withUserConfiguration(JsonMapperBuilderConsumerConfig.class).run((context) -> {
			JsonMapperBuilderConsumerConfig consumer = context.getBean(JsonMapperBuilderConsumerConfig.class);
			assertThat(consumer.builderOne).isNotNull();
			assertThat(consumer.builderTwo).isNotNull();
			assertThat(consumer.builderOne).isNotSameAs(consumer.builderTwo);
		});
	}

	@Test
	void jsonComponentThatInjectsObjectMapperCausesBeanCurrentlyInCreationException() {
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
		ObjectMapper objectMapper() {
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
	static class CustomModuleBuilderCustomizerConfig {

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
	static class JsonMapperBuilderConsumerConfig {

		JsonMapper.Builder builderOne;

		JsonMapper.Builder builderTwo;

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

	protected static final class Foo {

		private String name;

		private Foo() {
		}

		static Foo create() {
			return new Foo();
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	static class Bar {

		private String propertyName;

		String getPropertyName() {
			return this.propertyName;
		}

		void setPropertyName(String propertyName) {
			this.propertyName = propertyName;
		}

	}

	@JsonComponent
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

		private String property1;

		public String property2;

		String getProperty3() {
			return null;
		}

	}

	static class Person {

		@JsonFormat(pattern = "yyyyMMdd")
		private Date birthDate;

		Date getBirthDate() {
			return this.birthDate;
		}

		void setBirthDate(Date birthDate) {
			this.birthDate = birthDate;
		}

	}

	@JsonMixin(type = Person.class)
	static class EmptyMixin {

	}

	@AutoConfigurationPackage
	static class MixinConfiguration {

	}

	@JsonComponent
	static class CircularDependencySerializer extends ValueSerializer<String> {

		CircularDependencySerializer(ObjectMapper objectMapper) {

		}

		@Override
		public void serialize(String value, JsonGenerator gen, SerializationContext context) {

		}

	}

	@Import(CircularDependencySerializer.class)
	@Configuration(proxyBeanMethods = false)
	static class CircularDependencySerializerConfiguration {

	}

}
