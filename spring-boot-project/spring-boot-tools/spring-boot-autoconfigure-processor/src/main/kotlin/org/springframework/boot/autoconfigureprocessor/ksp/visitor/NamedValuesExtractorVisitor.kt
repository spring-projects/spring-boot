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

/**
 * @author Pavel Pletnev
 */
internal open class NamedValuesExtractorVisitor(
		annotationInfo: AnnotationInfo,
		vararg names: String
) : BaseVisitor(annotationInfo) {

	private val names: Set<String> = names.toHashSet()

	override fun getValues(annotation: KSAnnotation): List<Any?> {
		val values: MutableList<Any?> = mutableListOf()

		annotation.arguments.forEach { arg ->
			val argName = arg.name?.getShortName()
			if (names.contains(argName)) {
				values.addAll(extractValues(arg))
			}
		}

		return values
	}

}