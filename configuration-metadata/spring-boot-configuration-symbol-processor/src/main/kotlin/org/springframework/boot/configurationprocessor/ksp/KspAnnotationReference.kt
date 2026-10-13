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
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import org.springframework.boot.configurationprocessor.model.AnnotationReference
import org.springframework.boot.configurationprocessor.model.TypeDeclaration

/**
 * [AnnotationReference] backed by a [KSAnnotation].
 *
 * @author Areg Iazychian
 */
internal class KspAnnotationReference(
	private val context: KspProcessingContext,
	private val annotation: KSAnnotation,
) : AnnotationReference {

	override fun getType(): TypeDeclaration =
		this.context.type(checkNotNull(this.annotation.annotationType.resolve().classDeclaration()))

	override fun getValues(): Map<String, Any> {
		val values = LinkedHashMap<String, Any>()
		for (argument in this.annotation.arguments) {
			val name = argument.name?.asString() ?: continue
			values[name] = convert(argument.value) ?: continue
		}
		return values
	}

	private fun convert(value: Any?): Any? = when (value) {
		is List<*> -> value.mapNotNull(::convert)
		// An enum constant, depending on the version of KSP
		is KSClassDeclaration -> value.simpleName.asString()
		is KSType -> value.declaration.simpleName.asString()
		else -> value
	}

}
