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

import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.CONFIGURATION_PROPERTIES
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.CONFIGURATION_PROPERTIES_SOURCE
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.ENDPOINT_ANNOTATIONS
import org.springframework.boot.configurationprocessor.json.JSONException
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller
import java.io.File
import java.io.IOException

/**
 * [SymbolProcessor] that writes the configuration metadata of Kotlin classes annotated
 * with `@ConfigurationProperties`.
 *
 * Metadata is collected across all processing rounds and written as a single
 * `META-INF/spring-configuration-metadata.json` resource once processing has completed.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
class ConfigurationMetadataSymbolProcessor(
	private val codeGenerator: CodeGenerator,
	logger: KSPLogger,
	private val options: Map<String, String> = emptyMap(),
) : SymbolProcessor {

	private val context = MetadataGenerationContext(logger)

	private val propertyDescriptorResolver = PropertyDescriptorResolver(this.context)

	private val endpointMetadataResolver = EndpointMetadataResolver(this.context)

	private val metadata = ConfigurationMetadata()

	private val sourceFiles = LinkedHashSet<KSFile>()

	private val sourceMetadata = LinkedHashMap<String, ConfigurationMetadata>()

	override fun process(resolver: Resolver): List<KSAnnotated> {
		val symbols = resolver.getSymbolsWithAnnotation(CONFIGURATION_PROPERTIES).toList()
		symbols.filterIsInstance<KSClassDeclaration>().forEach(::processConfigurationProperties)
		symbols.filterIsInstance<KSFunctionDeclaration>().forEach(::processConfigurationPropertiesMethod)
		resolver.getSymbolsWithAnnotation(CONFIGURATION_PROPERTIES_SOURCE)
			.filterIsInstance<KSClassDeclaration>()
			.forEach(::processConfigurationPropertiesSource)
		ENDPOINT_ANNOTATIONS.asSequence()
			.flatMap { resolver.getSymbolsWithAnnotation(it) }
			.filterIsInstance<KSClassDeclaration>()
			.distinct()
			.forEach(::processEndpoint)
		return emptyList()
	}

	override fun finish() {
		writeSourceMetadata()
		val metadata = mergeAdditionalMetadata()
		removeIgnored(metadata)
		if (metadata.items.isEmpty()) {
			return
		}
		write(metadata, METADATA_PATH)
	}

	private fun writeSourceMetadata() {
		this.sourceMetadata.forEach { (type, metadata) ->
			removeIgnored(metadata)
			if (metadata.items.isNotEmpty()) {
				write(metadata, SOURCE_METADATA_PATH + type)
			}
		}
	}

	private fun write(metadata: ConfigurationMetadata, path: String) {
		val dependencies = Dependencies(true, *this.sourceFiles.toTypedArray())
		this.codeGenerator.createNewFileByPath(dependencies, path, METADATA_EXTENSION).use { output ->
			JsonMarshaller().write(metadata, output)
		}
	}

	/**
	 * Process a type annotated with `@ConfigurationPropertiesSource`, whose properties are
	 * described in a file of their own so that other modules can reuse them.
	 */
	private fun processConfigurationPropertiesSource(declaration: KSClassDeclaration) {
		val type = this.context.typeNames.resolve(declaration)
		val metadata = this.sourceMetadata.getOrPut(type) { ConfigurationMetadata() }
		processType("", declaration, ArrayDeque(), target = metadata)
	}

	/**
	 * Merge the metadata contributed by `META-INF/additional-spring-configuration-metadata.json`.
	 *
	 * As KSP gives a processor no access to the resources of the module, the directories
	 * to look into have to be provided using the
	 * `org.springframework.boot.configurationprocessor.additionalMetadataLocations` option.
	 */
	private fun mergeAdditionalMetadata(): ConfigurationMetadata {
		val additional = readAdditionalMetadata() ?: return this.metadata
		val merged = ConfigurationMetadata(this.metadata)
		merged.merge(additional)
		return merged
	}

	private fun readAdditionalMetadata(): ConfigurationMetadata? {
		val locations = this.options[ADDITIONAL_METADATA_LOCATIONS_OPTION] ?: return null
		val file = locations.split(",")
			.map(String::trim)
			.filter(String::isNotEmpty)
			.map { File(it, ADDITIONAL_METADATA_PATH) }
			.firstOrNull(File::isFile) ?: return null
		return try {
			file.inputStream().use { JsonMarshaller().read(it) }
		}
		catch (ex: IOException) {
			this.context.logger.warn("Unable to read additional metadata from '$file': ${ex.message}")
			null
		}
		catch (ex: JSONException) {
			this.context.logger.error("Invalid additional meta-data in '$file': ${ex.message}")
			null
		}
	}

	private fun removeIgnored(metadata: ConfigurationMetadata) {
		metadata.ignored.forEach { metadata.removeMetadata(it.type, it.name) }
	}

	private fun processConfigurationProperties(declaration: KSClassDeclaration) {
		val annotation = declaration.findAnnotation(CONFIGURATION_PROPERTIES) ?: return
		val prefix = prefixOf(annotation)
		val type = this.context.typeNames.resolve(declaration)
		this.metadata.add(ItemMetadata.newGroup(prefix, type, type, null))
		processType(prefix, declaration, ArrayDeque())
	}

	/**
	 * Process a method annotated with `@ConfigurationProperties`, whose return type carries
	 * the properties.
	 */
	private fun processConfigurationPropertiesMethod(function: KSFunctionDeclaration) {
		val annotation = function.findAnnotation(CONFIGURATION_PROPERTIES) ?: return
		if (function.isPrivate()) {
			return
		}
		val returnType = function.returnType?.resolve() ?: return
		val declaration = returnType.declaration as? KSClassDeclaration ?: return
		if (declaration.qualifiedName?.asString() == KOTLIN_UNIT) {
			return
		}
		val owner = function.parentDeclaration as? KSClassDeclaration ?: return
		val prefix = prefixOf(annotation)
		val group = ItemMetadata.newGroup(
			prefix,
			this.context.typeNames.resolve(declaration),
			this.context.typeNames.resolve(owner),
			sourceMethod(function),
		)
		if (hasSimilarGroup(group)) {
			this.context.logger.error("Duplicate @ConfigurationProperties definition for prefix '$prefix'", function)
			return
		}
		this.metadata.add(group)
		function.containingFile?.let(this.sourceFiles::add)
		processType(prefix, declaration, ArrayDeque(), function)
	}

	private fun processType(
		prefix: String,
		declaration: KSClassDeclaration,
		seen: ArrayDeque<String>,
		factoryMethod: KSFunctionDeclaration? = null,
		target: ConfigurationMetadata = this.metadata,
	) {
		val type = this.context.typeNames.resolve(declaration)
		if (seen.contains(type)) {
			return
		}
		seen.addLast(type)
		declaration.containingFile?.let(this.sourceFiles::add)
		this.propertyDescriptorResolver.resolve(declaration, factoryMethod).forEach { descriptor ->
			descriptor.toItemMetadata(prefix, this.context)?.let(target::add)
			if (descriptor.isNested(this.context)) {
				processNestedType(prefix, descriptor, seen, factoryMethod, target)
			}
		}
		seen.removeLast()
	}

	private fun processNestedType(
		prefix: String,
		descriptor: PropertyDescriptor,
		seen: ArrayDeque<String>,
		factoryMethod: KSFunctionDeclaration?,
		target: ConfigurationMetadata,
	) {
		val nestedType = descriptor.type.declaration as? KSClassDeclaration ?: return
		val nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, descriptor.name)
		processType(nestedPrefix, nestedType, seen, factoryMethod, target)
	}

	/**
	 * Process a type annotated with one of the actuator endpoint annotations.
	 */
	private fun processEndpoint(declaration: KSClassDeclaration) {
		val endpoint = this.endpointMetadataResolver.resolve(declaration) ?: return
		declaration.containingFile?.let(this.sourceFiles::add)
		this.metadata.addIfMissing(endpoint.group)
		endpoint.properties.forEach { property -> addEndpointProperty(property, declaration) }
	}

	private fun addEndpointProperty(property: ItemMetadata, declaration: KSClassDeclaration) {
		val existing = this.metadata.items.firstOrNull {
			it.isOfItemType(ItemMetadata.ItemType.PROPERTY) && it.name == property.name
		}
		if (existing == null) {
			this.metadata.add(property)
			return
		}
		if (existing.defaultValue != property.defaultValue) {
			this.context.logger.error(
				"Existing property '${existing.name}' from type ${existing.sourceType} has a conflicting value. " +
					"Existing value: ${existing.defaultValue}, new value from type ${property.sourceType}: " +
					"${property.defaultValue}",
				declaration,
			)
		}
	}

	private fun prefixOf(annotation: KSAnnotation): String =
		annotation.findStringValue("prefix") ?: annotation.findStringValue("value") ?: ""

	private fun hasSimilarGroup(group: ItemMetadata): Boolean = this.metadata.items.any {
		it.isOfItemType(ItemMetadata.ItemType.GROUP) && it.name == group.name && it.type == group.type
	}

	private fun sourceMethod(function: KSFunctionDeclaration): String {
		val parameters = function.parameters.joinToString(separator = ",") {
			this.context.typeNames.resolve(it.type.resolve())
		}
		return "${function.simpleName.asString()}($parameters)"
	}

	private companion object {

		private const val METADATA_PATH = "META-INF/spring-configuration-metadata"

		private const val SOURCE_METADATA_PATH = "META-INF/spring/configuration-metadata/"

		private const val METADATA_EXTENSION = "json"

		private const val KOTLIN_UNIT = "kotlin.Unit"

		private const val ADDITIONAL_METADATA_PATH = "META-INF/additional-spring-configuration-metadata.json"

		private const val ADDITIONAL_METADATA_LOCATIONS_OPTION =
			"org.springframework.boot.configurationprocessor.additionalMetadataLocations"

	}

}
