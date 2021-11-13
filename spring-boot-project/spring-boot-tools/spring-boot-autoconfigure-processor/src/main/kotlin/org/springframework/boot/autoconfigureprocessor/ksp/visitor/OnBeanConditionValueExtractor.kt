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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueArgument


/**
 * @author Pavel Pletnev
 */
internal class OnBeanConditionValueExtractor(
		annotationInfo: AnnotationInfo
) : BaseVisitor(annotationInfo) {

	override fun getValues(annotation: KSAnnotation): List<Any?> {
		val valuesByName = annotation.arguments.associateBy { it.name?.getShortName() }
		return if (isNotEmpty(valuesByName["name"])) {
			listOf()
		} else {
			(extractValues(valuesByName["value"]) + extractValues(valuesByName["type"])).sortedBy { it.toString() }
		}
	}

	private fun isNotEmpty(value: KSValueArgument?): Boolean {
		if (value == null) {
			return false
		}
		return when (value.value) {
			is Array<*> -> (value.value as Array<*>).isNotEmpty()
			is Collection<*> -> (value.value as Collection<*>).isNotEmpty()
			else -> false
		}
	}
}