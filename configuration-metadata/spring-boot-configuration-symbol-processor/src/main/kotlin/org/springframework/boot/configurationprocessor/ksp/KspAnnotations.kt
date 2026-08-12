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

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Return the annotation with the given [qualifiedName] that is directly present on this
 * element, or `null` if there is no such annotation.
 *
 * @author Areg Iazychian
 */
internal fun KSAnnotated.findAnnotation(qualifiedName: String): KSAnnotation? =
	annotations.firstOrNull { it.hasQualifiedName(qualifiedName) }

/**
 * Return whether an annotation with the given [qualifiedName] is present on this element,
 * optionally considering meta-annotations.
 *
 * @author Areg Iazychian
 */
internal fun KSAnnotated.hasAnnotation(qualifiedName: String, considerMetaAnnotations: Boolean = false): Boolean {
	if (annotations.any { it.hasQualifiedName(qualifiedName) }) {
		return true
	}
	if (!considerMetaAnnotations) {
		return false
	}
	val seen = mutableSetOf<String>()
	return annotations.any { it.isMetaAnnotatedWith(qualifiedName, seen) }
}

/**
 * Return the value of the annotation attribute with the given [name], or `null` if the
 * attribute is absent or has no value.
 *
 * @author Areg Iazychian
 */
internal fun KSAnnotation.findValue(name: String): Any? =
	arguments.firstOrNull { it.name?.asString() == name }?.value

/**
 * Return the [String] value of the annotation attribute with the given [name], or `null`
 * if the attribute is absent or empty.
 *
 * An empty value is treated as absent to match the Java annotation processor, which only
 * sees attributes that have been declared explicitly.
 *
 * @author Areg Iazychian
 */
internal fun KSAnnotation.findStringValue(name: String): String? = (findValue(name) as? String)?.ifEmpty { null }

private fun KSAnnotation.hasQualifiedName(qualifiedName: String): Boolean {
	if (shortName.asString() != qualifiedName.substringAfterLast('.')) {
		return false
	}
	return annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
}

private fun KSAnnotation.isMetaAnnotatedWith(qualifiedName: String, seen: MutableSet<String>): Boolean {
	val declaration = annotationType.resolve().declaration as? KSClassDeclaration ?: return false
	val name = declaration.qualifiedName?.asString() ?: return false
	if (!seen.add(name)) {
		return false
	}
	return declaration.annotations.any {
		it.hasQualifiedName(qualifiedName) || it.isMetaAnnotatedWith(qualifiedName, seen)
	}
}
