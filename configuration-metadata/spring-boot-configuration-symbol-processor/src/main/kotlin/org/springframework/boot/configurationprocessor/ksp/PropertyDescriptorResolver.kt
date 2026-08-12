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

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.AUTOWIRED
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.CONSTRUCTOR_BINDING
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.NAME

/**
 * Resolves the [PropertyDescriptor] instances of a class annotated with
 * `@ConfigurationProperties`.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
internal class PropertyDescriptorResolver(private val context: MetadataGenerationContext) {

	/**
	 * Return the properties of the given [declaration] that are candidates for metadata
	 * generation.
	 */
	fun resolve(
		declaration: KSClassDeclaration,
		factoryMethod: KSFunctionDeclaration? = null,
	): List<PropertyDescriptor> {
		if (factoryMethod != null) {
			return resolveJavaBean(declaration, factoryMethod)
		}
		val bindConstructor = findBindConstructor(declaration)
		return if (bindConstructor != null) {
			resolveConstructorBound(declaration, bindConstructor)
		}
		else {
			resolveJavaBean(declaration)
		}
	}

	private fun findBindConstructor(declaration: KSClassDeclaration): KSFunctionDeclaration? {
		val constructors = declaration.getConstructors().toList()
		val annotated = constructors.filter { it.hasAnnotation(CONSTRUCTOR_BINDING, considerMetaAnnotations = true) }
		if (annotated.isNotEmpty()) {
			return annotated.singleOrNull()
		}
		return deduceBindConstructor(declaration, constructors)
	}

	private fun deduceBindConstructor(
		declaration: KSClassDeclaration,
		constructors: List<KSFunctionDeclaration>,
	): KSFunctionDeclaration? {
		val candidate = constructors.singleOrNull() ?: return null
		if (candidate.parameters.isEmpty() || candidate.hasAnnotation(AUTOWIRED)) {
			return null
		}
		if (declaration.parentDeclaration is KSClassDeclaration && !candidate.isPublic()) {
			return null
		}
		return candidate
	}

	private fun resolveConstructorBound(
		declaration: KSClassDeclaration,
		constructor: KSFunctionDeclaration,
	): List<PropertyDescriptor> {
		val properties = declaration.getAllProperties().associateBy { it.simpleName.asString() }
		val descriptors = LinkedHashMap<String, PropertyDescriptor>()
		constructor.parameters.forEach { parameter ->
			val parameterName = parameter.name?.asString()
			if (parameterName != null) {
				val name = parameter.findAnnotation(NAME)?.findStringValue("value") ?: parameterName
				val descriptor = PropertyDescriptor.ConstructorParameter(
					name,
					parameter.type.resolve(),
					declaration,
					parameter,
					properties[parameterName],
				)
				register(descriptors, descriptor, declaration)
			}
		}
		return descriptors.values.toList()
	}

	private fun resolveJavaBean(
		declaration: KSClassDeclaration,
		factoryMethod: KSFunctionDeclaration? = null,
	): List<PropertyDescriptor> {
		val descriptors = LinkedHashMap<String, PropertyDescriptor>()
		declaration.getAllProperties().filter(::isCandidate).forEach { property ->
			val name = property.findAnnotation(NAME)?.findStringValue("value") ?: property.simpleName.asString()
			val descriptor = PropertyDescriptor.JavaBean(
				name,
				this.context.typeOf(property, declaration),
				declaration,
				property,
				factoryMethod,
			)
			register(descriptors, descriptor, declaration)
		}
		return descriptors.values.toList()
	}

	private fun isCandidate(property: KSPropertyDeclaration): Boolean =
		property.isPublic() && property.getter != null

	private fun register(
		descriptors: MutableMap<String, PropertyDescriptor>,
		descriptor: PropertyDescriptor,
		declaration: KSClassDeclaration,
	) {
		if (!descriptor.isProperty(this.context) && !descriptor.isNested(this.context)) {
			return
		}
		val existing = descriptors.putIfAbsent(descriptor.name, descriptor)
		if (existing != null && existing.type != descriptor.type) {
			this.context.logger.error(
				"Property name '${descriptor.name}' maps to distinct properties in type " +
					context.typeNames.resolve(declaration),
				declaration,
			)
		}
	}

}
