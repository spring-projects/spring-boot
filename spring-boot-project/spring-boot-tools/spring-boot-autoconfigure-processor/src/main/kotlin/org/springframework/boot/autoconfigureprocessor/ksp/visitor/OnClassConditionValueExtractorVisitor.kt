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
internal class OnClassConditionValueExtractorVisitor(
		annotationInfo: AnnotationInfo
) : NamedValuesExtractorVisitor(annotationInfo, "value", "name") {

	override fun getValues(annotation: KSAnnotation): List<Any?> {
		return super.getValues(annotation)
				.sortedWith(java.util.Comparator { o1: Any?, o2: Any? -> compare(o1!!, o2!!) })
	}

	private fun compare(o1: Any, o2: Any): Int {
		return Comparator.comparing<String, Boolean> { type: String -> this.isSpringClass(type) }
				.thenComparing(java.lang.String.CASE_INSENSITIVE_ORDER)
				.compare(o1.toString(), o2.toString())
	}

	private fun isSpringClass(type: String): Boolean {
		return type.startsWith("org.springframework")
	}
}