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

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility
import org.springframework.boot.configurationprocessor.model.MethodDeclaration
import org.springframework.boot.configurationprocessor.model.TypeDeclaration
import org.springframework.boot.configurationprocessor.model.TypeReference
import org.springframework.boot.configurationprocessor.model.VariableDeclaration

/**
 * [MethodDeclaration] for the getter or the setter that Java sees for a Kotlin property.
 *
 * @author Areg Iazychian
 */
internal class KspAccessorDeclaration(
	context: KspProcessingContext,
	private val accessor: KSPropertyAccessor,
) : KspDeclaration(context, accessor), MethodDeclaration {

	private val property = this.accessor.receiver

	private val setter = this.accessor as? KSPropertySetter

	private val visibility =
		this.accessor.modifiers.firstNotNullOfOrNull(VISIBILITIES::get) ?: this.property.getVisibility()

	override fun getName(): String {
		val name = this.property.simpleName.asString()
		// A property whose name starts with 'is' is not prefixed
		val hasPrefix = name.startsWith("is") && name.length > 2 && name[2] !in 'a'..'z'
		val capitalized = name.replaceFirstChar { if (it in 'a'..'z') it.uppercaseChar() else it }
		return when {
			this.setter == null -> if (hasPrefix) name else "get$capitalized"
			else -> "set" + if (hasPrefix) name.substring(2) else capitalized
		}
	}

	override fun getEnclosingType(): TypeDeclaration? =
		(this.property.parentDeclaration as? KSClassDeclaration)?.let(this.context::type)

	override fun getParameters(): List<VariableDeclaration> =
		listOfNotNull(this.setter?.parameter).map { KspParameterDeclaration(this.context, it) }

	override fun getReturnType(): TypeReference =
		if (this.setter == null) {
			KspTypeReference.of(this.context, this.property)
		}
		else {
			KspTypeReference(this.context, this.context.resolver.builtIns.unitType)
		}

	override fun getSignature(): String = this.name + this.parameters.joinToString(",", "(", ")") { it.type.toString() }

	override fun isPublic(): Boolean = this.visibility == Visibility.PUBLIC

	override fun isPrivate(): Boolean = this.visibility == Visibility.PRIVATE

	override fun isAbstract(): Boolean = this.property.isAbstract()

	override fun isStatic(): Boolean = false

	override fun isDeprecated(): Boolean = super.isDeprecated() || isDeprecated(listOf(this.property))

	override fun toString(): String = this.signature

	private companion object {

		private val VISIBILITIES = mapOf(
			Modifier.PRIVATE to Visibility.PRIVATE,
			Modifier.PROTECTED to Visibility.PROTECTED,
			Modifier.INTERNAL to Visibility.INTERNAL,
		)

	}

}
