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
 * Renders a [KSType] using the Java type names that configuration metadata is expected to
 * contain.
 *
 * Kotlin types are mapped to their JVM counterparts (`kotlin.Int` becomes
 * `java.lang.Integer`, `kotlin.collections.List` becomes `java.util.List`), nested classes
 * use the binary `$` separator and generic information is retained, matching the output of
 * the Java annotation processor.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
internal class JavaTypeNameResolver {

	/**
	 * Return the Java type name of the given [type].
	 * @param type the type to render
	 * @return the fully qualified Java name, including generic information
	 */
	fun resolve(type: KSType): String = resolve(type, mutableSetOf())

	/**
	 * Return the binary name of the given [declaration], using `$` to separate nested
	 * classes.
	 * @param declaration the declaration to render
	 * @return the fully qualified binary name
	 */
	fun resolve(declaration: KSClassDeclaration): String {
		val parent = declaration.parentDeclaration
		if (parent is KSClassDeclaration) {
			return resolve(parent) + '$' + declaration.simpleName.asString()
		}
		return declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
	}

	private fun resolve(type: KSType, visitedTypeParameters: MutableSet<KSTypeParameter>): String =
		when (val declaration = type.declaration) {
			is KSTypeAlias -> resolve(declaration.type.resolve(), visitedTypeParameters)
			is KSTypeParameter -> resolve(declaration, visitedTypeParameters)
			is KSClassDeclaration -> resolve(type, declaration, visitedTypeParameters)
			else -> declaration.simpleName.asString()
		}

	private fun resolve(typeParameter: KSTypeParameter, visitedTypeParameters: MutableSet<KSTypeParameter>): String {
		if (!visitedTypeParameters.add(typeParameter)) {
			// Self-referencing bound such as `T : Comparable<T>`, keep the variable name
			return typeParameter.name.asString()
		}
		try {
			val bound = typeParameter.bounds.firstOrNull()?.resolve()
			return if (bound != null) resolve(bound, visitedTypeParameters) else JAVA_OBJECT
		}
		finally {
			visitedTypeParameters.remove(typeParameter)
		}
	}

	private fun resolve(
		type: KSType,
		declaration: KSClassDeclaration,
		visitedTypeParameters: MutableSet<KSTypeParameter>,
	): String {
		val qualifiedName = declaration.qualifiedName?.asString()
		if (qualifiedName == KOTLIN_ARRAY) {
			val component = type.arguments.firstOrNull()?.type?.resolve()
			return (if (component != null) resolve(component, visitedTypeParameters) else JAVA_OBJECT) + "[]"
		}
		PRIMITIVE_ARRAYS[qualifiedName]?.let { return it }
		val javaName = JAVA_NAMES[qualifiedName] ?: resolve(declaration)
		if (type.arguments.isEmpty()) {
			return javaName
		}
		val arguments = type.arguments.joinToString(separator = ",", prefix = "<", postfix = ">") {
			resolve(it, visitedTypeParameters)
		}
		return javaName + arguments
	}

	private fun resolve(argument: KSTypeArgument, visitedTypeParameters: MutableSet<KSTypeParameter>): String {
		if (argument.variance == Variance.STAR) {
			return "?"
		}
		val type = argument.type?.resolve() ?: return "?"
		val name = resolve(type, visitedTypeParameters)
		return when (argument.variance) {
			Variance.COVARIANT -> "? extends $name"
			Variance.CONTRAVARIANT -> "? super $name"
			else -> name
		}
	}

	private companion object {

		private const val JAVA_OBJECT = "java.lang.Object"

		private const val KOTLIN_ARRAY = "kotlin.Array"

		/**
		 * Kotlin types that are mapped to a different type on the JVM. Primitives are
		 * mapped to their boxed counterpart as the Java annotation processor boxes them
		 * too.
		 */
		private val JAVA_NAMES = mapOf(
			"kotlin.Any" to JAVA_OBJECT,
			"kotlin.Boolean" to "java.lang.Boolean",
			"kotlin.Byte" to "java.lang.Byte",
			"kotlin.Char" to "java.lang.Character",
			"kotlin.CharSequence" to "java.lang.CharSequence",
			"kotlin.Comparable" to "java.lang.Comparable",
			"kotlin.Double" to "java.lang.Double",
			"kotlin.Enum" to "java.lang.Enum",
			"kotlin.Float" to "java.lang.Float",
			"kotlin.Int" to "java.lang.Integer",
			"kotlin.Long" to "java.lang.Long",
			"kotlin.Number" to "java.lang.Number",
			"kotlin.Short" to "java.lang.Short",
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
		 * Kotlin primitive arrays, which are plain JVM arrays of the matching primitive.
		 */
		private val PRIMITIVE_ARRAYS = mapOf(
			"kotlin.BooleanArray" to "java.lang.Boolean[]",
			"kotlin.ByteArray" to "java.lang.Byte[]",
			"kotlin.CharArray" to "java.lang.Character[]",
			"kotlin.DoubleArray" to "java.lang.Double[]",
			"kotlin.FloatArray" to "java.lang.Float[]",
			"kotlin.IntArray" to "java.lang.Integer[]",
			"kotlin.LongArray" to "java.lang.Long[]",
			"kotlin.ShortArray" to "java.lang.Short[]",
		)

	}

}
