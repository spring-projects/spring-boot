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
import org.springframework.boot.configurationprocessor.model.AnnotationReference
import org.springframework.boot.configurationprocessor.model.Declaration

/**
 * [Declaration] backed by a symbol.
 *
 * @author Areg Iazychian
 */
internal abstract class KspDeclaration(
	protected val context: KspProcessingContext,
	/**
	 * The symbol that this declaration is backed by.
	 */
	val symbol: KSAnnotated,
) : Declaration {

	/**
	 * The symbols whose annotations Java sees on this declaration.
	 */
	protected open val annotated: List<KSAnnotated>
		get() = listOf(this.symbol)

	override fun getAnnotations(): List<AnnotationReference> = annotationsOf(this.annotated)

	override fun isDeprecated(): Boolean = isDeprecated(this.annotated)

	protected fun isDeprecated(symbols: List<KSAnnotated>): Boolean =
		annotationsOf(symbols).any { it.type.qualifiedName in DEPRECATED_ANNOTATIONS }

	private fun annotationsOf(symbols: List<KSAnnotated>): List<AnnotationReference> =
		symbols.flatMap { it.annotations }.map { KspAnnotationReference(this.context, it) }

	override fun getDocComment(): String? = null

	override fun equals(other: Any?): Boolean =
		this === other || (other is KspDeclaration && this.javaClass == other.javaClass && this.symbol == other.symbol)

	override fun hashCode(): Int = this.symbol.hashCode()

	private companion object {

		private val DEPRECATED_ANNOTATIONS = setOf("kotlin.Deprecated", "java.lang.Deprecated")

	}

}
