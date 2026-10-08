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
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.springframework.boot.configurationprocessor.model.TypeDeclaration
import org.springframework.boot.configurationprocessor.model.TypeReference
import org.springframework.boot.configurationprocessor.model.VariableDeclaration

/**
 * [VariableDeclaration] for the field of a [KSPropertyDeclaration].
 *
 * The field carries the annotations of the property, and those of the parameter of the
 * primary constructor that declares it. It is documented by the KDoc of the property or
 * by the matching `@property` or `@param` tag of the KDoc of the class.
 *
 * @author Areg Iazychian
 */
internal class KspFieldDeclaration(
	context: KspProcessingContext,
	private val property: KSPropertyDeclaration,
) : KspDeclaration(context, property), VariableDeclaration {

	private val owner = this.property.parentDeclaration as? KSClassDeclaration

	override val annotated: List<KSAnnotated>
		get() = listOfNotNull(this.property, constructorParameter())

	override fun getName(): String = this.property.simpleName.asString()

	override fun getEnclosingType(): TypeDeclaration? = this.owner?.let(this.context::type)

	override fun getType(): TypeReference = KspTypeReference.of(this.context, this.property)

	override fun isFinal(): Boolean = !this.property.isMutable

	override fun hasDefaultValue(): Boolean = false

	override fun getDocComment(): String? = this.property.docString ?: this.owner?.docString?.let(::findTag)

	private fun constructorParameter(): KSAnnotated? =
		this.owner?.primaryConstructor?.parameters?.firstOrNull { parameter ->
			(parameter.isVal || parameter.isVar) && parameter.name?.asString() == this.name
		}

	private fun findTag(docString: String): String? {
		val tag = Regex("@(?:property|param)\\s+${Regex.escape(this.name)}\\b(.*?)(?=\\n\\s*@|$)", DOT_MATCHES_ALL)
		return tag.find(docString)?.groupValues?.get(1)
	}

	private companion object {

		private val DOT_MATCHES_ALL = RegexOption.DOT_MATCHES_ALL

	}

}
