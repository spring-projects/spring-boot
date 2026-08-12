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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType

/**
 * Resolves the default value of a property from a `@DefaultValue` annotation or from the
 * type itself.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
internal object DefaultValues {

	/**
	 * Return the value declared by the given `@DefaultValue` [annotation], coerced to the
	 * type of the property.
	 * @param annotation the `@DefaultValue` annotation
	 * @param type the type of the property
	 * @param node the node to report coercion failures against
	 * @param context the metadata generation context
	 * @return the default value, a list of values for a collection, or `null` if the
	 * annotation does not declare a value
	 */
	fun fromAnnotation(
		annotation: KSAnnotation,
		type: KSType,
		node: KSNode,
		context: MetadataGenerationContext,
	): Any? {
		val values = annotation.findValue("value").asStringList() ?: return null
		if (values.isEmpty()) {
			return null
		}
		val targetType = elementTypeOf(type, context) ?: type
		val coerced = values.map { coerce(it, targetType, node, context) }
		return if (coerced.size == 1) coerced[0] else coerced
	}

	/**
	 * Return the value a property of the given [type] defaults to when it is bound to a
	 * type that has no `null` representation on the JVM, or `null` when there is no such
	 * default.
	 */
	fun fromType(type: KSType): Any? {
		if (type.isMarkedNullable) {
			return null
		}
		return PRIMITIVE_DEFAULTS[type.declaration.qualifiedName?.asString()]
	}

	private fun elementTypeOf(type: KSType, context: MetadataGenerationContext): KSType? {
		if (!context.isCollectionOrMap(type)) {
			return null
		}
		return type.arguments.firstOrNull()?.type?.resolve()
	}

	private fun coerce(value: String, type: KSType, node: KSNode, context: MetadataGenerationContext): Any {
		val coercion = COERCIONS[type.declaration.qualifiedName?.asString()] ?: return value
		val coerced = coercion.invoke(value)
		if (coerced == null) {
			context.logger.error("Invalid ${type.declaration.simpleName.asString()} representation '$value'", node)
			return value
		}
		return coerced
	}

	private fun Any?.asStringList(): List<String>? = when (this) {
		is String -> listOf(this)
		is List<*> -> filterIsInstance<String>()
		else -> null
	}

	private val PRIMITIVE_DEFAULTS = mapOf<String, Any>(
		"kotlin.Boolean" to false,
		"kotlin.Byte" to 0.toByte(),
		"kotlin.Double" to 0.0,
		"kotlin.Float" to 0.0f,
		"kotlin.Int" to 0,
		"kotlin.Long" to 0L,
		"kotlin.Short" to 0.toShort(),
	)

	private val COERCIONS = mapOf<String, (String) -> Any?>(
		// Matches Boolean.parseBoolean, which never fails
		"kotlin.Boolean" to { value: String -> value.toBoolean() },
		"kotlin.Byte" to String::toByteOrNull,
		"kotlin.Char" to { value: String -> if (value.length > 1) null else value },
		"kotlin.Double" to String::toDoubleOrNull,
		"kotlin.Float" to String::toFloatOrNull,
		"kotlin.Int" to String::toIntOrNull,
		"kotlin.Long" to String::toLongOrNull,
		"kotlin.Short" to String::toShortOrNull,
	)

}
