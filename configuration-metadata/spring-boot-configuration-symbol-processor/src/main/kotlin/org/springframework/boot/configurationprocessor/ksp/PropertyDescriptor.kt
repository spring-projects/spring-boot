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

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.CONFIGURATION_PROPERTIES
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.DEFAULT_VALUE
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.NESTED_CONFIGURATION_PROPERTY
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata

/**
 * Description of a Kotlin declaration that is a candidate for metadata generation.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
internal sealed class PropertyDescriptor(
	val name: String,
	val type: KSType,
	val declaringType: KSClassDeclaration,
) {

	/**
	 * Elements that can carry the annotations describing this property, in the order in
	 * which they should be considered.
	 */
	protected abstract val annotatedElements: List<KSAnnotated>

	/**
	 * Elements that can carry the annotations describing the deprecation of this property.
	 */
	protected open val deprecationElements: List<KSAnnotated>
		get() = annotatedElements

	/**
	 * Return whether this descriptor describes a property rather than a nested group.
	 */
	abstract fun isProperty(context: MetadataGenerationContext): Boolean

	/**
	 * Return the default value of this property, or `null` if it has none or if it cannot
	 * be determined.
	 */
	abstract fun defaultValue(context: MetadataGenerationContext): Any?

	/**
	 * Return the description of this property, or `null` if it is not documented.
	 */
	abstract fun description(context: MetadataGenerationContext): String?

	/**
	 * Return the getter that a nested group should be attributed to, or `null` if the
	 * property has none.
	 */
	protected abstract fun sourceMethod(): String?

	/**
	 * Return whether this property is a nested group of properties.
	 */
	fun isNested(context: MetadataGenerationContext): Boolean {
		val typeDeclaration = type.declaration as? KSClassDeclaration ?: return false
		if (typeDeclaration.classKind == ClassKind.ENUM_CLASS || typeDeclaration.classKind == ClassKind.ENUM_ENTRY) {
			return false
		}
		if (annotatedElements.any { it.hasAnnotation(CONFIGURATION_PROPERTIES) }) {
			return false
		}
		if (annotatedElements.any { it.hasAnnotation(NESTED_CONFIGURATION_PROPERTY) }) {
			return true
		}
		return !isCyclePresent(typeDeclaration, context) && hasSameTopLevelType(typeDeclaration, context)
	}

	/**
	 * Return the metadata for this descriptor, or `null` if it contributes none.
	 */
	fun toItemMetadata(prefix: String, context: MetadataGenerationContext): ItemMetadata? {
		if (isNested(context)) {
			return toGroupMetadata(prefix, context)
		}
		if (isProperty(context)) {
			return toPropertyMetadata(prefix, context)
		}
		return null
	}

	private fun toGroupMetadata(prefix: String, context: MetadataGenerationContext): ItemMetadata {
		val nestedType = type.declaration as KSClassDeclaration
		return ItemMetadata.newGroup(
			ConfigurationMetadata.nestedPrefix(prefix, name),
			context.typeNames.resolve(nestedType),
			context.typeNames.resolve(declaringType),
			sourceMethod(),
		)
	}

	private fun toPropertyMetadata(prefix: String, context: MetadataGenerationContext): ItemMetadata {
		val deprecation = if (context.isDeprecated(*deprecationElements(), declaringType)) {
			context.resolveDeprecation(*deprecationElements())
		}
		else {
			null
		}
		return ItemMetadata.newProperty(
			prefix,
			name,
			context.typeNames.resolve(type),
			context.typeNames.resolve(declaringType),
			null,
			description(context),
			defaultValue(context),
			deprecation,
		)
	}

	private fun deprecationElements(): Array<KSAnnotated> = deprecationElements.toTypedArray()

	/**
	 * Return the name of the JVM getter of this property, mirroring the source method
	 * recorded by the Java annotation processor.
	 */
	protected fun jvmGetterName(): String {
		if (name.length > IS_PREFIX.length && name.startsWith(IS_PREFIX) && !name[IS_PREFIX.length].isLowerCase()) {
			return "$name()"
		}
		return "get" + name.replaceFirstChar(Char::uppercaseChar) + "()"
	}

	private fun isCyclePresent(typeDeclaration: KSClassDeclaration, context: MetadataGenerationContext): Boolean {
		val target = context.typeNames.resolve(typeDeclaration)
		var candidate = declaringType.parentDeclaration
		while (candidate is KSClassDeclaration) {
			if (context.typeNames.resolve(candidate) == target) {
				return true
			}
			candidate = candidate.parentDeclaration
		}
		return false
	}

	private fun hasSameTopLevelType(
		typeDeclaration: KSClassDeclaration,
		context: MetadataGenerationContext,
	): Boolean {
		val target = context.typeNames.resolve(topLevelType(typeDeclaration))
		var candidate: KSClassDeclaration? = declaringType
		while (candidate != null) {
			if (context.typeNames.resolve(topLevelType(candidate)) == target) {
				return true
			}
			candidate = candidate.superClassDeclaration()
		}
		return false
	}

	private fun topLevelType(declaration: KSClassDeclaration): KSClassDeclaration {
		var candidate = declaration
		while (candidate.parentDeclaration is KSClassDeclaration) {
			candidate = candidate.parentDeclaration as KSClassDeclaration
		}
		return candidate
	}

	private fun KSClassDeclaration.superClassDeclaration(): KSClassDeclaration? = superTypes
		.map { it.resolve().declaration }
		.filterIsInstance<KSClassDeclaration>()
		.firstOrNull { it.classKind == ClassKind.CLASS && it.qualifiedName?.asString() != KOTLIN_ANY }

	/**
	 * A [PropertyDescriptor] for a parameter of the constructor used for binding.
	 */
	class ConstructorParameter(
		name: String,
		type: KSType,
		declaringType: KSClassDeclaration,
		private val parameter: KSValueParameter,
		private val property: KSPropertyDeclaration?,
	) : PropertyDescriptor(name, type, declaringType) {

		override val annotatedElements: List<KSAnnotated> =
			listOfNotNull(parameter, property?.getter, property, property?.setter)

		override fun isProperty(context: MetadataGenerationContext): Boolean = !isNested(context)

		override fun defaultValue(context: MetadataGenerationContext): Any? {
			val annotation = annotatedElements.firstNotNullOfOrNull { it.findAnnotation(DEFAULT_VALUE) }
			if (annotation != null) {
				return DefaultValues.fromAnnotation(annotation, type, parameter, context)
			}
			// KSP does not expose parameter default values, see google/ksp#1868
			if (parameter.hasDefault) {
				return null
			}
			return DefaultValues.fromType(type)
		}

		override fun description(context: MetadataGenerationContext): String? = context.documentation(property)
			?: parameter.name?.asString()?.let { context.parameterDocumentation(declaringType, it) }

		override fun sourceMethod(): String? = if (property != null) jvmGetterName() else null

	}

	/**
	 * A [PropertyDescriptor] for a mutable property bound using its getter and setter.
	 */
	class JavaBean(
		name: String,
		type: KSType,
		declaringType: KSClassDeclaration,
		private val property: KSPropertyDeclaration,
		private val factoryMethod: KSFunctionDeclaration? = null,
	) : PropertyDescriptor(name, type, declaringType) {

		override val annotatedElements: List<KSAnnotated> =
			listOfNotNull(property.getter, property, property.setter)

		override val deprecationElements: List<KSAnnotated>
			get() = annotatedElements + listOfNotNull(this.factoryMethod)

		override fun isProperty(context: MetadataGenerationContext): Boolean {
			if (context.isExcluded(type)) {
				return false
			}
			return property.isMutable || context.isCollectionOrMap(type)
		}

		// KSP does not expose property initializers, see google/ksp#1868
		override fun defaultValue(context: MetadataGenerationContext): Any? = null

		override fun description(context: MetadataGenerationContext): String? = context.documentation(property)

		override fun sourceMethod(): String = jvmGetterName()

	}

	private companion object {

		private const val IS_PREFIX = "is"

		private const val KOTLIN_ANY = "kotlin.Any"

	}

}
