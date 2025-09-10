/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package org.springframework.boot.kotlin.serialization.autoconfigure

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonNamingStrategy
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Tests for [KotlinSerializationAutoConfiguration].
 *
 * @author Dmitry Sulman
 */
class KotlinSerializationAutoConfigurationTests {
	private val contextRunner = ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(KotlinSerializationAutoConfiguration::class.java))

	@Test
	fun shouldSupplyJsonBean() {
		this.contextRunner.run { context ->
			assertThat(context).hasSingleBean(Json::class.java)
		}
	}

	@Test
	fun shouldNotSupplyJsonBean() {
		this.contextRunner
			.withClassLoader(FilteredClassLoader(Json::class.java))
			.run { context ->
				assertThat(context).doesNotHaveBean(Json::class.java)
			}
	}

	@Test
	fun serializeToString() {
		this.contextRunner.run { context ->
			val json = context.getBean(Json::class.java)
			assertThat(json.encodeToString(DataObject("hello")))
				.isEqualTo("""{"stringField":"hello"}""")
		}
	}

	@Test
	fun deserializeFromString() {
		this.contextRunner.run { context ->
			val json = context.getBean(Json::class.java)
			assertThat(json.decodeFromString<DataObject>("""{"stringField":"hello"}"""))
				.isEqualTo(DataObject("hello"))
		}
	}

	@Test
	fun customJsonBean() {
		this.contextRunner
			.withUserConfiguration(CustomKotlinSerializationConfig::class.java)
			.run { context ->
				assertThat(context).hasSingleBean(Json::class.java)
				val json = context.getBean(Json::class.java)
				assertThat(json.encodeToString(DataObject("hello")))
					.isEqualTo("""{"string_field":"hello"}""")
			}
	}

	@Test
	fun serializeSnakeCase() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.naming-strategy=snake_case")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.encodeToString(DataObject("hello")))
					.isEqualTo("""{"string_field":"hello"}""")
			}
	}

	@Test
	fun serializeKebabCase() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.naming-strategy=kebab_case")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.encodeToString(DataObject("hello")))
					.isEqualTo("""{"string-field":"hello"}""")
			}
	}

	@Test
	fun serializePrettyPrint() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.pretty-print=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.encodeToString(DataObject("hello")))
					.isEqualTo(
						"""
						{
						    "stringField": "hello"
						}
						""".trimIndent()
					)
			}
	}

	@Test
	@Suppress("JsonStandardCompliance")
	fun deserializeLenient() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.lenient=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.decodeFromString<DataObject>("""{"stringField":hello}"""))
					.isEqualTo(DataObject("hello"))
			}
	}

	@Test
	fun deserializeIgnoreUnknownKeys() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.ignore-unknown-keys=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.decodeFromString<DataObject>("""{"stringField":"hello", "anotherField":"value"}"""))
					.isEqualTo(DataObject("hello"))
			}
	}

	@Test
	fun serializeDefaults() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.encode-defaults=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.encodeToString(DataObjectWithDefault("hello")))
					.isEqualTo("""{"stringField":"hello","defaultField":"default"}""")
			}
	}

	@Test
	fun serializeExplicitNullsFalse() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.explicit-nulls=false")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.encodeToString(DataObjectWithDefault(null, "hello")))
					.isEqualTo("""{"defaultField":"hello"}""")
			}
	}

	@Test
	fun deserializeCoerceInputValues() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.coerce-input-values=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.decodeFromString<DataObjectWithDefault>("""{"stringField":"hello", "defaultField":null}"""))
					.isEqualTo(DataObjectWithDefault("hello", "default"))
			}
	}

	@Test
	fun serializeStructuredMapKeys() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.allow-structured-map-keys=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				val map = mapOf(
					DataObject("key1") to "value1",
					DataObject("key2") to "value2",
				)
				assertThat(json.encodeToString(map))
					.isEqualTo("""[{"stringField":"key1"},"value1",{"stringField":"key2"},"value2"]""")
			}
	}

	@Test
	fun serializeSpecialFloatingPointValues() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.allow-special-floating-point-values=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.encodeToString(DataObjectDouble(Double.NaN)))
					.isEqualTo("""{"value":NaN}""")
			}
	}

	@Test
	fun serializeClassDiscriminator() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.class-discriminator=class")
			.run { context ->
				val json = context.getBean(Json::class.java)
				val value: BaseClass = ChildClass("value")
				assertThat(json.encodeToString(value))
					.isEqualTo("""{"class":"child","stringField":"value"}""")
			}
	}

	@Test
	fun serializeClassDiscriminatorNone() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.class-discriminator-mode=none")
			.run { context ->
				val json = context.getBean(Json::class.java)
				val value: BaseClass = ChildClass("value")
				assertThat(json.encodeToString(value))
					.isEqualTo("""{"stringField":"value"}""")
			}
	}

	@Test
	fun deserializeEnumsCaseInsensitive() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.decode-enums-case-insensitive=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.decodeFromString<DataObjectEnumValues>("""{"values":["value_A", "alternative"]}"""))
					.isEqualTo(DataObjectEnumValues(listOf(EnumValue.VALUE_A, EnumValue.VALUE_B)))
			}
	}

	@Test
	fun deserializeAlternativeNames() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.use-alternative-names=false")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThatExceptionOfType(SerializationException::class.java).isThrownBy {
					json.decodeFromString<DataObject>("""{"alternative":"hello"}""")
				}.withMessageContaining("Encountered an unknown key")
			}
	}

	@Test
	@Suppress("JsonStandardCompliance")
	fun deserializeTrailingComma() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.allow-trailing-comma=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.decodeFromString<DataObject>("""{"stringField":"hello",}"""))
					.isEqualTo(DataObject("hello"))
			}
	}

	@Test
	@Suppress("JsonStandardCompliance")
	fun deserializeComments() {
		this.contextRunner
			.withPropertyValues("spring.kotlin.serialization.allow-comments=true")
			.run { context ->
				val json = context.getBean(Json::class.java)
				assertThat(json.decodeFromString<DataObject>("""{"stringField":"hello" /*comment*/}"""))
					.isEqualTo(DataObject("hello"))
			}
	}

	@Serializable
	@OptIn(ExperimentalSerializationApi::class)
	private data class DataObject(@JsonNames("alternative") private val stringField: String)

	@Serializable
	private data class DataObjectWithDefault(
		private val stringField: String?,
		private val defaultField: String = "default",
	)

	@Serializable
	private data class DataObjectDouble(private val value: Double)

	@OptIn(ExperimentalSerializationApi::class)
	enum class EnumValue { VALUE_A, @JsonNames("Alternative") VALUE_B }

	@Serializable
	private data class DataObjectEnumValues(private val values: List<EnumValue>)

	@Serializable
	sealed class BaseClass {
		abstract val stringField: String
	}

	@Serializable
	@SerialName("child")
	class ChildClass(override val stringField: String) : BaseClass()

	@Configuration(proxyBeanMethods = false)
	class CustomKotlinSerializationConfig {

		@Bean
		@OptIn(ExperimentalSerializationApi::class)
		fun customKotlinSerializationJson(): Json {
			return Json { namingStrategy = JsonNamingStrategy.SnakeCase }
		}

	}

}
