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

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Variance

/**
 * Provides the names that Java gives to Kotlin types, as configuration meta-data
 * describes types the way that Java sees them: `kotlin.Int` is `java.lang.Integer` and
 * `kotlin.collections.List` is `java.util.List`.
 *
 * @author Areg Iazychian
 */
internal object JavaTypeNames {

	const val OBJECT = "java.lang.Object"

	private const val ARRAY = "kotlin.Array"

	/**
	 * The Kotlin types that are mapped to a primitive type when they are not nullable,
	 * and the wrapper that they are mapped to otherwise.
	 */
	val PRIMITIVES = mapOf(
		"kotlin.Boolean" to "java.lang.Boolean",
		"kotlin.Byte" to "java.lang.Byte",
		"kotlin.Char" to "java.lang.Character",
		"kotlin.Double" to "java.lang.Double",
		"kotlin.Float" to "java.lang.Float",
		"kotlin.Int" to "java.lang.Integer",
		"kotlin.Long" to "java.lang.Long",
		"kotlin.Short" to "java.lang.Short",
	)

	/**
	 * The Kotlin types that are mapped to an array.
	 */
	val ARRAYS = PRIMITIVES.entries.associate { (name, wrapper) -> "${name}Array" to "$wrapper[]" } +
		(ARRAY to "$OBJECT[]")

	private val CLASSES = PRIMITIVES + mapOf(
		"kotlin.Annotation" to "java.lang.annotation.Annotation",
		"kotlin.Any" to OBJECT,
		"kotlin.CharSequence" to "java.lang.CharSequence",
		"kotlin.Cloneable" to "java.lang.Cloneable",
		"kotlin.Comparable" to "java.lang.Comparable",
		"kotlin.Enum" to "java.lang.Enum",
		"kotlin.Number" to "java.lang.Number",
		"kotlin.String" to "java.lang.String",
		"kotlin.Throwable" to "java.lang.Throwable",
		"kotlin.Unit" to "void",
		"kotlin.collections.Collection" to "java.util.Collection",
		"kotlin.collections.Iterable" to "java.lang.Iterable",
		"kotlin.collections.Iterator" to "java.util.Iterator",
		"kotlin.collections.List" to "java.util.List",
		"kotlin.collections.ListIterator" to "java.util.ListIterator",
		"kotlin.collections.Map" to "java.util.Map",
		"kotlin.collections.Map.Entry" to "java.util.Map\$Entry",
		"kotlin.collections.MutableCollection" to "java.util.Collection",
		"kotlin.collections.MutableIterable" to "java.lang.Iterable",
		"kotlin.collections.MutableIterator" to "java.util.Iterator",
		"kotlin.collections.MutableList" to "java.util.List",
		"kotlin.collections.MutableListIterator" to "java.util.ListIterator",
		"kotlin.collections.MutableMap" to "java.util.Map",
		"kotlin.collections.MutableMap.MutableEntry" to "java.util.Map\$Entry",
		"kotlin.collections.MutableSet" to "java.util.Set",
		"kotlin.collections.Set" to "java.util.Set",
	)

	/**
	 * Return the name of the given [declaration], suitable for a call to `Class.forName`.
	 */
	fun of(declaration: KSClassDeclaration): String {
		val name = declaration.qualifiedName?.asString() ?: return declaration.simpleName.asString()
		CLASSES[name]?.let { return it }
		val parent = declaration.parentDeclaration as? KSClassDeclaration ?: return name
		return of(parent) + '$' + declaration.simpleName.asString()
	}

	/**
	 * Return the name of the given [type], including all its generic information.
	 */
	fun of(type: KSType): String = of(type, mutableSetOf())

	private fun of(type: KSType, visited: MutableSet<KSTypeParameter>): String =
		when (val declaration = type.declaration) {
			is KSTypeAlias -> of(declaration.type.resolve(), visited)
			is KSTypeParameter -> of(declaration, visited)
			is KSClassDeclaration -> of(type, declaration, visited)
			else -> declaration.simpleName.asString()
		}

	private fun of(typeParameter: KSTypeParameter, visited: MutableSet<KSTypeParameter>): String {
		if (!visited.add(typeParameter)) {
			// Self-referencing bound such as T : Comparable<T>
			return typeParameter.name.asString()
		}
		try {
			return typeParameter.bounds.firstOrNull()?.resolve()?.let { of(it, visited) } ?: OBJECT
		}
		finally {
			visited.remove(typeParameter)
		}
	}

	private fun of(type: KSType, declaration: KSClassDeclaration, visited: MutableSet<KSTypeParameter>): String {
		val name = declaration.qualifiedName?.asString()
		if (name == ARRAY) {
			return (type.arguments.firstOrNull()?.type?.resolve()?.let { of(it, visited) } ?: OBJECT) + "[]"
		}
		ARRAYS[name]?.let { return it }
		if (type.arguments.isEmpty()) {
			return of(declaration)
		}
		return of(declaration) + type.arguments.joinToString(",", "<", ">") { of(it, visited) }
	}

	private fun of(argument: KSTypeArgument, visited: MutableSet<KSTypeParameter>): String {
		val name = argument.type?.resolve()?.let { of(it, visited) }
		return when {
			name == null || argument.variance == Variance.STAR -> "?"
			argument.variance == Variance.COVARIANT -> "? extends $name"
			argument.variance == Variance.CONTRAVARIANT -> "? super $name"
			else -> name
		}
	}

}
