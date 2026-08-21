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

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.DEPRECATED_CONFIGURATION_PROPERTY
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.JAVA_DEPRECATED
import org.springframework.boot.configurationprocessor.ksp.ConfigurationPropertiesAnnotations.KOTLIN_DEPRECATED
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation

/**
 * Utilities shared by the components that turn Kotlin declarations into configuration
 * metadata.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
internal class MetadataGenerationContext(
	val logger: KSPLogger,
	val typeNames: JavaTypeNameResolver = JavaTypeNameResolver(),
) {

	/**
	 * Return the type of the given [property] as seen from [declaringType], so that the
	 * type arguments that a superclass declares are resolved against the type that
	 * extends it.
	 */
	fun typeOf(property: KSPropertyDeclaration, declaringType: KSClassDeclaration): KSType =
		try {
			property.asMemberOf(declaringType.asStarProjectedType())
		}
		catch (ex: IllegalArgumentException) {
			this.logger.info("Unable to resolve the type of '${property.simpleName.asString()}': ${ex.message}")
			property.type.resolve()
		}

	/**
	 * Return whether the given [type] is a type that can never be a configuration
	 * property.
	 */
	fun isExcluded(type: KSType): Boolean {
		val name = (type.declaration as? KSClassDeclaration)?.let { typeNames.resolve(it) } ?: return false
		return TYPE_EXCLUDES.contains(name)
	}

	/**
	 * Return whether the given [type] is a [Collection] or a [Map], in which case a getter
	 * is enough for the property to be bindable.
	 */
	fun isCollectionOrMap(type: KSType): Boolean {
		val declaration = type.declaration as? KSClassDeclaration ?: return false
		if (declaration.isCollectionOrMap()) {
			return true
		}
		return declaration.getAllSuperTypes().any { (it.declaration as? KSClassDeclaration)?.isCollectionOrMap() == true }
	}

	/**
	 * Return the KDoc of the given [declaration] as a single line of text, or `null` if it
	 * has no documentation.
	 */
	fun documentation(declaration: KSDeclaration?): String? {
		val docString = declaration?.docString ?: return null
		val text = docString.lineSequence()
			.map { it.trimStart().removePrefix("*") }
			.joinToString(separator = " ")
			.replace(WHITESPACE, " ")
			.trim()
		return text.ifEmpty { null }
	}

	/**
	 * Return the documentation of the constructor parameter with the given [name], taken
	 * from the `@property` or `@param` tag of the KDoc of [declaration].
	 */
	fun parameterDocumentation(declaration: KSClassDeclaration, name: String): String? {
		val docString = declaration.docString ?: return null
		val text = docString.lineSequence().map { it.trimStart().removePrefix("*") }.joinToString(separator = "\n")
		val tag = Regex("@(?:property|param)\\s+${Regex.escape(name)}\\b(.*?)(?=\\n\\s*@|$)", RegexOption.DOT_MATCHES_ALL)
		val value = tag.find(text)?.groupValues?.get(1) ?: return null
		return value.replace(WHITESPACE, " ").trim().ifEmpty { null }
	}

	/**
	 * Return whether any of the given [elements] is deprecated.
	 */
	fun isDeprecated(vararg elements: KSAnnotated?): Boolean = elements.filterNotNull().any(::isDeprecated)

	/**
	 * Return the deprecation to use for a property, taking the details of
	 * `@DeprecatedConfigurationProperty` into account when it is present on one of the
	 * given [elements].
	 */
	fun resolveDeprecation(vararg elements: KSAnnotated?): ItemDeprecation {
		val annotation = elements.filterNotNull()
			.firstNotNullOfOrNull { it.findAnnotation(DEPRECATED_CONFIGURATION_PROPERTY) }
			?: return ItemDeprecation(null, null, null)
		return ItemDeprecation(
			annotation.findStringValue("reason"),
			annotation.findStringValue("replacement"),
			annotation.findStringValue("since"),
		)
	}

	private fun isDeprecated(element: KSAnnotated): Boolean =
		element.hasAnnotation(KOTLIN_DEPRECATED) || element.hasAnnotation(JAVA_DEPRECATED) ||
			element.hasAnnotation(DEPRECATED_CONFIGURATION_PROPERTY)

	private fun KSClassDeclaration.isCollectionOrMap(): Boolean =
		COLLECTION_OR_MAP.contains(qualifiedName?.asString())

	private companion object {

		private val WHITESPACE = Regex("\\s+")

		private val COLLECTION_OR_MAP = setOf(
			"java.util.Collection",
			"java.util.Map",
			"kotlin.collections.Collection",
			"kotlin.collections.Map",
			"kotlin.collections.MutableCollection",
			"kotlin.collections.MutableMap",
		)

		/**
		 * Types that are never treated as a nested group or a property. Kept in sync with
		 * the Java annotation processor.
		 */
		private val TYPE_EXCLUDES = setOf(
			"com.zaxxer.hikari.IConnectionCustomizer",
			"groovy.lang.MetaClass",
			"groovy.text.markup.MarkupTemplateEngine",
			"java.io.Writer",
			"java.io.PrintWriter",
			"java.lang.ClassLoader",
			"java.util.concurrent.ThreadFactory",
			"jakarta.jms.XAConnectionFactory",
			"javax.sql.DataSource",
			"javax.sql.XADataSource",
			"org.apache.tomcat.jdbc.pool.PoolConfiguration",
			"org.apache.tomcat.jdbc.pool.Validator",
			"org.flywaydb.core.api.callback.FlywayCallback",
			"org.flywaydb.core.api.resolver.MigrationResolver",
		)

	}

}
