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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Nullability
import org.springframework.boot.configurationprocessor.model.TypeDeclaration
import org.springframework.boot.configurationprocessor.model.TypeReference

/**
 * [TypeReference] backed by a [KSType].
 *
 * @author Areg Iazychian
 */
internal class KspTypeReference(
	private val context: KspProcessingContext,
	private val type: KSType,
	/**
	 * Returns this type as it is seen from the given type, which resolves the type
	 * variables of a member that has been inherited.
	 */
	private val asMemberOf: (KSType) -> KSType = { type },
) : TypeReference {

	private val name = this.type.declaration.qualifiedName?.asString()

	override fun isVoid(): Boolean = this.name == "kotlin.Unit"

	override fun isPrimitive(): Boolean =
		this.type.nullability == Nullability.NOT_NULL && this.name in JavaTypeNames.PRIMITIVES

	override fun isTypeVariable(): Boolean = this.type.declaration is KSTypeParameter

	override fun isNullable(): Boolean = this.type.nullability == Nullability.NULLABLE

	override fun isCollectionOrMap(): Boolean = isAssignableTo(COLLECTION) || isAssignableTo(MAP)

	override fun isSameType(other: TypeReference): Boolean = this.type == (other as KspTypeReference).type

	override fun getDeclaration(): TypeDeclaration? {
		if (isPrimitive || this.name in JavaTypeNames.ARRAYS) {
			return null
		}
		return this.type.classDeclaration()?.let(this.context::type)
	}

	override fun getElementType(): TypeReference? {
		if (!isAssignableTo(COLLECTION)) {
			return null
		}
		return this.type.arguments.firstOrNull()?.type?.resolve()?.let { KspTypeReference(this.context, it) }
	}

	override fun getName(declaringType: TypeDeclaration): String {
		val resolved = try {
			this.asMemberOf((declaringType as KspTypeDeclaration).declaration.asStarProjectedType())
		}
		catch (ex: IllegalArgumentException) {
			this.type
		}
		return JavaTypeNames.of(resolved)
	}

	override fun toString(): String = JavaTypeNames.of(this.type)

	private fun isAssignableTo(name: String): Boolean {
		val target = this.context.resolver.getClassDeclarationByName(name) ?: return false
		return target.asStarProjectedType().makeNullable().isAssignableFrom(this.type)
	}

	companion object {

		private const val COLLECTION = "kotlin.collections.Collection"

		private const val MAP = "kotlin.collections.Map"

		/**
		 * Return the type of the given [property].
		 */
		fun of(context: KspProcessingContext, property: KSPropertyDeclaration): KspTypeReference =
			KspTypeReference(context, property.type.resolve()) { property.asMemberOf(it) }

		/**
		 * Return the type that the given [function] returns.
		 */
		fun ofReturnType(context: KspProcessingContext, function: KSFunctionDeclaration): KspTypeReference {
			val returnType = function.returnType?.resolve() ?: context.resolver.builtIns.unitType
			return KspTypeReference(context, returnType) { function.asMemberOf(it).returnType ?: returnType }
		}

	}

}

/**
 * Return the class that this type declares, looking through type aliases.
 */
internal fun KSType.classDeclaration(): KSClassDeclaration? = when (val declaration = this.declaration) {
	is KSClassDeclaration -> declaration
	is KSTypeAlias -> declaration.type.resolve().classDeclaration()
	else -> null
}
