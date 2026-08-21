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

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.ENDPOINT_ACCESS_ENUM
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.ENDPOINT_ANNOTATIONS
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.READ_OPERATION
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata

/**
 * Resolves the metadata contributed by a type annotated with one of the actuator endpoint
 * annotations.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
internal class EndpointMetadataResolver(private val context: MetadataGenerationContext) {

	/**
	 * Return the metadata of the given [declaration] when it is an endpoint, or `null`
	 * when it is not an endpoint or declares no identifier.
	 */
	fun resolve(declaration: KSClassDeclaration): EndpointMetadata? {
		val annotation = ENDPOINT_ANNOTATIONS.firstNotNullOfOrNull { declaration.findAnnotation(it) }
			?: return null
		val id = annotation.findStringValue("id") ?: return null
		val key = ItemMetadata.newItemMetadataPrefix(ENDPOINT_PREFIX, id)
		val type = this.context.typeNames.resolve(declaration)
		val defaultAccess = defaultAccessOf(annotation.findValue("defaultAccess"))
		val items = mutableListOf(
			ItemMetadata.newProperty(
				key,
				"access",
				ENDPOINT_ACCESS_ENUM,
				type,
				null,
				"Permitted level of access for the $id endpoint.",
				defaultAccess,
				null,
			),
		)
		if (hasMainReadOperation(declaration)) {
			items += ItemMetadata.newProperty(
				key,
				"cache.time-to-live",
				"java.time.Duration",
				type,
				null,
				"Maximum time that a response can be cached.",
				"0ms",
				null,
			)
		}
		return EndpointMetadata(ItemMetadata.newGroup(key, type, type, null), items, defaultAccess)
	}

	private fun defaultAccessOf(value: Any?): String {
		val name = when (value) {
			null -> DEFAULT_ACCESS
			is KSType -> value.declaration.simpleName.asString()
			is KSClassDeclaration -> value.simpleName.asString()
			else -> value.toString()
		}
		return name.lowercase()
	}

	private fun hasMainReadOperation(declaration: KSClassDeclaration): Boolean =
		declaration.getDeclaredFunctions().any { function ->
			function.hasAnnotation(READ_OPERATION) && !returnsUnit(function) && hasNoMandatoryParameters(function)
		}

	private fun returnsUnit(function: KSFunctionDeclaration): Boolean =
		function.returnType?.resolve()?.declaration?.qualifiedName?.asString() == KOTLIN_UNIT

	private fun hasNoMandatoryParameters(function: KSFunctionDeclaration): Boolean =
		function.parameters.all { it.type.resolve().isMarkedNullable || it.hasDefault }

	/**
	 * The metadata contributed by a single endpoint.
	 *
	 * @author Areg Iazychian
	 * @since 4.2.0
	 */
	data class EndpointMetadata(val group: ItemMetadata, val properties: List<ItemMetadata>, val defaultAccess: String)

	private companion object {

		private const val ENDPOINT_PREFIX = "management.endpoint."

		private const val DEFAULT_ACCESS = "unrestricted"

		private const val KOTLIN_UNIT = "kotlin.Unit"

	}

}
