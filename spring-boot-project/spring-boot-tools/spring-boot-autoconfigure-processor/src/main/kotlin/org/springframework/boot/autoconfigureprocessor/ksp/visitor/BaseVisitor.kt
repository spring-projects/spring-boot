/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor.ksp.visitor

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSDefaultVisitor

/**
 * @author Pavel Pletnev
 */
internal abstract class BaseVisitor(
		private val annotationInfo: AnnotationInfo
) : KSDefaultVisitor<Unit, Map<String, String>>() {

	override fun defaultHandler(node: KSNode, data: Unit): Map<String, String> {
		return emptyMap()
	}

	override fun visitClassDeclaration(
			classDeclaration: KSClassDeclaration,
			data: Unit
	): Map<String, String> {
		val properties: MutableMap<String, String> = mutableMapOf()
		classDeclaration.annotations.forEach { ann ->
			val annName = ann.annotationType.resolve().declaration.qualifiedName?.asString()
			with(annotationInfo) {
				if (qualifiedName == annName) {
					val values: List<Any?> = getValues(ann)

					val className = classDeclaration.qualifiedName!!.asString()

					properties["$className.$simpleName"] = values.joinToString(separator = ",")
					properties[className] = ""
				}
			}
		}

		return properties
	}

	abstract fun getValues(annotation: KSAnnotation): List<Any?>

	protected fun extractValues(valueArgument: KSValueArgument?): List<Any?> {
		val value = valueArgument?.value ?: return emptyList()
		return if (value is List<*>) {
			value.map { extractValue(it) }
		} else {
			listOf(extractValue(value))
		}
	}

	private fun extractValue(value: Any?): Any? {
		if (value is KSType) {
			return value.declaration.qualifiedName?.asString()
		}
		return value
	}
}