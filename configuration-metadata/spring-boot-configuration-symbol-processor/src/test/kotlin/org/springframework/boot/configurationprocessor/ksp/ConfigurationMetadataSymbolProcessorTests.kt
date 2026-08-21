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

package org.springframework.boot.configurationprocessor.ksp

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Tests for [ConfigurationMetadataSymbolProcessor].
 *
 * @author Areg Iazychian
 */
class ConfigurationMetadataSymbolProcessorTests {

	private val compiler = MetadataCompiler()

	@Test
	fun `mutable properties are documented`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"SimpleProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties

				@ConfigurationProperties("simple")
				class SimpleProperties {

					var theName: String? = null

					var counter: Int = 0

				}
				""",
			),
		)
		assertThat(metadata.group("simple")?.type).isEqualTo("example.SimpleProperties")
		assertThat(metadata.property("simple.the-name")?.type).isEqualTo("java.lang.String")
		assertThat(metadata.property("simple.the-name")?.sourceType).isEqualTo("example.SimpleProperties")
		assertThat(metadata.property("simple.counter")?.type).isEqualTo("java.lang.Integer")
	}

	@Test
	fun `read only properties are ignored unless they are collections`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"ReadOnlyProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties

				@ConfigurationProperties("read-only")
				class ReadOnlyProperties {

					val name: String = "test"

					val items: MutableList<String> = mutableListOf()

					var writable: String? = null

				}
				""",
			),
		)
		assertThat(metadata.property("read-only.name")).isNull()
		assertThat(metadata.property("read-only.items")?.type).isEqualTo("java.util.List<java.lang.String>")
		assertThat(metadata.property("read-only.writable")).isNotNull()
	}

	@Test
	fun `constructor bound properties use the default value annotation`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"ImmutableProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties
				import org.springframework.boot.context.properties.bind.DefaultValue
				import org.springframework.boot.context.properties.bind.Name

				@ConfigurationProperties("immutable")
				data class ImmutableProperties(
					val name: String?,
					@DefaultValue("8080") val port: Int,
					@Name("import") val importName: String?,
					val flag: Boolean,
				)
				""",
			),
		)
		assertThat(metadata.property("immutable.name")?.type).isEqualTo("java.lang.String")
		assertThat(metadata.property("immutable.port")?.defaultValue).isEqualTo(8080)
		assertThat(metadata.property("immutable.import")).isNotNull()
		assertThat(metadata.property("immutable.flag")?.defaultValue).isEqualTo(false)
	}

	@Test
	fun `parameters with a Kotlin default have no default value`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"DefaultedProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties

				@ConfigurationProperties("defaulted")
				data class DefaultedProperties(val port: Int = 8080)
				""",
			),
		)
		assertThat(metadata.property("defaulted.port")).isNotNull()
		assertThat(metadata.property("defaulted.port")?.defaultValue).isNull()
	}

	@Test
	fun `nested types are documented as groups`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"NestedProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties

				@ConfigurationProperties("nested")
				class NestedProperties {

					val security: Security = Security()

					class Security {

						var username: String? = null

					}

				}
				""",
			),
		)
		assertThat(metadata.group("nested.security")?.type).isEqualTo("example.NestedProperties\$Security")
		assertThat(metadata.group("nested.security")?.sourceMethod).isEqualTo("getSecurity()")
		assertThat(metadata.property("nested.security.username")?.type).isEqualTo("java.lang.String")
	}

	@Test
	fun `types outside of the declaring type require the nested annotation`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"ExternalNestedProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties
				import org.springframework.boot.context.properties.NestedConfigurationProperty

				class Credentials {

					var username: String? = null

				}

				@ConfigurationProperties("external")
				class ExternalNestedProperties {

					var plain: Credentials? = null

					@NestedConfigurationProperty
					var nested: Credentials? = null

				}
				""",
			),
		)
		assertThat(metadata.property("external.plain")?.type).isEqualTo("example.Credentials")
		assertThat(metadata.group("external.nested")?.type).isEqualTo("example.Credentials")
		assertThat(metadata.property("external.nested.username")?.type).isEqualTo("java.lang.String")
	}

	@Test
	fun `kotlin types are mapped to their java counterparts`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"TypeProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties
				import java.time.Duration

				@ConfigurationProperties("types")
				class TypeProperties {

					var names: MutableList<String> = mutableListOf()

					var mappings: MutableMap<String, Int> = mutableMapOf()

					var values: Array<String> = emptyArray()

					var numbers: IntArray = intArrayOf()

					var timeout: Duration? = null

					var anything: Any? = null

				}
				""",
			),
		)
		assertThat(metadata.property("types.names")?.type).isEqualTo("java.util.List<java.lang.String>")
		assertThat(metadata.property("types.mappings")?.type)
			.isEqualTo("java.util.Map<java.lang.String,java.lang.Integer>")
		assertThat(metadata.property("types.values")?.type).isEqualTo("java.lang.String[]")
		assertThat(metadata.property("types.numbers")?.type).isEqualTo("java.lang.Integer[]")
		assertThat(metadata.property("types.timeout")?.type).isEqualTo("java.time.Duration")
		assertThat(metadata.property("types.anything")?.type).isEqualTo("java.lang.Object")
	}

	@Test
	fun `type arguments of a superclass are resolved`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"GenericProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties
				import java.time.Duration

				abstract class BaseProperties<T> {

					var value: T? = null

					var values: MutableList<T> = mutableListOf()

				}

				@ConfigurationProperties("generic")
				class GenericProperties : BaseProperties<Duration>()
				""",
			),
		)
		assertThat(metadata.property("generic.value")?.type).isEqualTo("java.time.Duration")
		assertThat(metadata.property("generic.values")?.type).isEqualTo("java.util.List<java.time.Duration>")
	}

	@Test
	fun `kdoc is used as the description`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"DocumentedProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties

				@ConfigurationProperties("documented")
				class DocumentedProperties {

					/**
					 * Name of the server, spanning
					 * two lines.
					 */
					var name: String? = null

				}
				""",
			),
		)
		assertThat(metadata.property("documented.name")?.description)
			.isEqualTo("Name of the server, spanning two lines.")
	}

	@Test
	fun `kdoc of a constructor parameter is used as the description`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"DocumentedImmutableProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties

				/**
				 * Immutable properties.
				 *
				 * @property name Name of the server.
				 * @property port Port of the server.
				 */
				@ConfigurationProperties("documented-immutable")
				data class DocumentedImmutableProperties(val name: String?, val port: Int)
				""",
			),
		)
		assertThat(metadata.property("documented-immutable.name")?.description).isEqualTo("Name of the server.")
		assertThat(metadata.property("documented-immutable.port")?.description).isEqualTo("Port of the server.")
	}

	@Test
	fun `deprecated properties are documented`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"DeprecatedProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties
				import org.springframework.boot.context.properties.DeprecatedConfigurationProperty

				@ConfigurationProperties("deprecated")
				class DeprecatedProperties {

					@get:DeprecatedConfigurationProperty(reason = "Not needed", replacement = "deprecated.replacement")
					var legacy: String? = null

					var replacement: String? = null

				}
				""",
			),
		)
		val deprecation = metadata.property("deprecated.legacy")?.deprecation
		assertThat(deprecation).isNotNull()
		assertThat(deprecation?.reason).isEqualTo("Not needed")
		assertThat(deprecation?.replacement).isEqualTo("deprecated.replacement")
		assertThat(metadata.property("deprecated.replacement")?.deprecation).isNull()
	}

	@Test
	fun `properties of an annotated method are documented`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"MethodProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties

				class SampleConfiguration {

					@ConfigurationProperties("method")
					fun methodProperties(): MethodProperties = MethodProperties()

				}

				class MethodProperties(val ignored: String = "") {

					var name: String? = null

				}
				""",
			),
		)
		assertThat(metadata.group("method")?.type).isEqualTo("example.MethodProperties")
		assertThat(metadata.group("method")?.sourceType).isEqualTo("example.SampleConfiguration")
		assertThat(metadata.group("method")?.sourceMethod).isEqualTo("methodProperties()")
		assertThat(metadata.property("method.name")?.type).isEqualTo("java.lang.String")
		assertThat(metadata.property("method.ignored")).isNull()
	}

	@Test
	fun `duplicate prefixes are rejected`() {
		assertThatIllegalStateException().isThrownBy {
			this.compiler.compile(
				SourceFile.kotlin(
					"DuplicateProperties.kt",
					"""
					package example

					import org.springframework.boot.context.properties.ConfigurationProperties

					class DuplicateConfiguration {

						@ConfigurationProperties("duplicate")
						fun first(): DuplicateProperties = DuplicateProperties()

						@ConfigurationProperties("duplicate")
						fun second(): DuplicateProperties = DuplicateProperties()

					}

					class DuplicateProperties {

						var name: String? = null

					}
					""",
				),
			)
		}.withMessageContaining("Duplicate @ConfigurationProperties definition for prefix 'duplicate'")
	}

	@Test
	fun `endpoints are documented`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"SampleEndpoint.kt",
				"""
				package example

				import org.springframework.boot.actuate.endpoint.Access
				import org.springframework.boot.actuate.endpoint.annotation.Endpoint
				import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

				@Endpoint(id = "sample")
				class SampleEndpoint {

					@ReadOperation
					fun read(): String = "sample"

				}

				@Endpoint(id = "restricted", defaultAccess = Access.READ_ONLY)
				class RestrictedEndpoint
				""",
			),
		)
		assertThat(metadata.group("management.endpoint.sample")?.type).isEqualTo("example.SampleEndpoint")
		assertThat(metadata.property("management.endpoint.sample.access")?.defaultValue).isEqualTo("unrestricted")
		assertThat(metadata.property("management.endpoint.sample.access")?.type)
			.isEqualTo("org.springframework.boot.actuate.endpoint.Access")
		assertThat(metadata.property("management.endpoint.sample.cache.time-to-live")?.defaultValue).isEqualTo("0ms")
		assertThat(metadata.property("management.endpoint.restricted.access")?.defaultValue).isEqualTo("read_only")
		assertThat(metadata.property("management.endpoint.restricted.cache.time-to-live")).isNull()
	}

	@Test
	fun `configuration properties sources are documented in their own file`() {
		val metadata = this.compiler.compileSourceMetadata(
			"example.Credentials",
			SourceFile.kotlin(
				"Credentials.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationPropertiesSource

				@ConfigurationPropertiesSource
				class Credentials {

					/**
					 * Name of the user.
					 */
					var username: String? = null

				}
				""",
			),
		)
		assertThat(metadata.property("username")?.type).isEqualTo("java.lang.String")
		assertThat(metadata.property("username")?.description).isEqualTo("Name of the user.")
		assertThat(metadata.property("username")?.sourceType).isEqualTo("example.Credentials")
	}

	@Test
	fun `additional metadata is merged`(@TempDir location: Path) {
		writeAdditionalMetadata(
			location,
			"""
			{
			  "properties": [
			    {
			      "name": "merged.extra",
			      "type": "java.lang.String",
			      "description": "Contributed by additional metadata."
			    }
			  ]
			}
			""",
		)
		val metadata = compileMergedSample(location)
		assertThat(metadata.property("merged.name")).isNotNull()
		assertThat(metadata.property("merged.extra")?.description).isEqualTo("Contributed by additional metadata.")
	}

	@Test
	fun `ignored properties are removed`(@TempDir location: Path) {
		writeAdditionalMetadata(
			location,
			"""
			{
			  "ignored": {
			    "properties": [
			      {
			        "name": "merged.name"
			      }
			    ]
			  }
			}
			""",
		)
		val metadata = compileMergedSample(location)
		assertThat(metadata.property("merged.name")).isNull()
	}

	@Test
	fun `no metadata is written when nothing is annotated`() {
		val metadata = this.compiler.compile(
			SourceFile.kotlin(
				"Plain.kt",
				"""
				package example

				class Plain {

					var name: String? = null

				}
				""",
			),
		)
		assertThat(metadata.items).isEmpty()
	}

	@Test
	fun `generated json matches the format of the annotation processor`() {
		val json = this.compiler.compileToJson(
			SourceFile.kotlin(
				"JsonProperties.kt",
				"""
				package example

				import org.springframework.boot.context.properties.ConfigurationProperties

				@ConfigurationProperties("json")
				class JsonProperties {

					/**
					 * Name of the server.
					 */
					var name: String? = null

				}
				""",
			),
		)
		assertThat(json).isEqualTo(
			"""
			{
			  "groups": [
			    {
			      "name": "json",
			      "type": "example.JsonProperties",
			      "sourceType": "example.JsonProperties"
			    }
			  ],
			  "properties": [
			    {
			      "name": "json.name",
			      "type": "java.lang.String",
			      "description": "Name of the server.",
			      "sourceType": "example.JsonProperties"
			    }
			  ],
			  "hints": [],
			  "ignored": {
			    "properties": []
			  }
			}
			""".trimIndent(),
		)
	}

	private fun writeAdditionalMetadata(location: Path, content: String) {
		val metaInf = location.resolve("META-INF").createDirectories()
		metaInf.resolve("additional-spring-configuration-metadata.json").writeText(content.trimIndent())
	}

	private fun compileMergedSample(location: Path): ConfigurationMetadata = this.compiler.compile(
		mapOf("org.springframework.boot.configurationprocessor.additionalMetadataLocations" to location.toString()),
		SourceFile.kotlin(
			"MergedProperties.kt",
			"""
			package example

			import org.springframework.boot.context.properties.ConfigurationProperties

			@ConfigurationProperties("merged")
			class MergedProperties {

				var name: String? = null

			}
			""",
		),
	)

	private fun ConfigurationMetadata.property(name: String): ItemMetadata? =
		item(name, ItemMetadata.ItemType.PROPERTY)

	private fun ConfigurationMetadata.group(name: String): ItemMetadata? = item(name, ItemMetadata.ItemType.GROUP)

	private fun ConfigurationMetadata.item(name: String, type: ItemMetadata.ItemType): ItemMetadata? =
		this.items.firstOrNull { it.isOfItemType(type) && it.name == name }

}
