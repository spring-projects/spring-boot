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

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Origin
import org.springframework.boot.configurationprocessor.model.MethodDeclaration
import org.springframework.boot.configurationprocessor.model.TypeDeclaration
import org.springframework.boot.configurationprocessor.model.TypeReference
import org.springframework.boot.configurationprocessor.model.VariableDeclaration

/**
 * [TypeDeclaration] backed by a [KSClassDeclaration].
 *
 * A Kotlin class is exposed the way Java sees it: each of its properties is a field with
 * a getter and, when it is mutable, a setter.
 *
 * @author Areg Iazychian
 */
internal class KspTypeDeclaration(
	context: KspProcessingContext,
	val declaration: KSClassDeclaration,
) : KspDeclaration(context, declaration), TypeDeclaration {

	override fun getName(): String = this.declaration.simpleName.asString()

	override fun getQualifiedName(): String = JavaTypeNames.of(this.declaration)

	override fun getEnclosingType(): TypeDeclaration? =
		(this.declaration.parentDeclaration as? KSClassDeclaration)?.let(this.context::type)

	override fun getSuperclass(): TypeDeclaration? {
		if (this.declaration.classKind in TYPES_WITHOUT_SUPERCLASS || this.qualifiedName == JavaTypeNames.OBJECT) {
			return null
		}
		val superclass = this.declaration.superTypes
			.mapNotNull { it.resolve().classDeclaration() }
			.firstOrNull { it.classKind != ClassKind.INTERFACE }
		return this.context.type(superclass ?: this.context.resolver.builtIns.anyType.classDeclaration()!!)
	}

	override fun asType(): TypeReference = KspTypeReference(this.context, this.declaration.asStarProjectedType())

	override fun isEnum(): Boolean = this.declaration.classKind == ClassKind.ENUM_CLASS

	override fun isRecord(): Boolean = false

	override fun hasSource(): Boolean = this.declaration.origin in SOURCE_ORIGINS

	override fun getConstructors(): List<MethodDeclaration> =
		this.declaration.getConstructors().map { KspMethodDeclaration(this.context, it) }.toList()

	override fun getMethods(): List<MethodDeclaration> {
		val accessors = this.declaration.getDeclaredProperties()
			.flatMap { property -> listOfNotNull(property.getter, property.setter) }
			.map { KspAccessorDeclaration(this.context, it) }
		val functions = this.declaration.getDeclaredFunctions()
			.filterNot { it.isConstructor() }
			.map { KspMethodDeclaration(this.context, it) }
		return (accessors + functions).toList()
	}

	override fun getFields(): List<VariableDeclaration> =
		this.declaration.getDeclaredProperties().map { KspFieldDeclaration(this.context, it) }.toList()

	override fun getRecordComponents(): List<VariableDeclaration> = emptyList()

	// KSP does not expose initializers, see https://github.com/google/ksp/issues/1868
	override fun getFieldValues(): Map<String, Any> = emptyMap()

	override fun getDocComment(): String? = this.declaration.docString

	override fun equals(other: Any?): Boolean = other is KspTypeDeclaration && this.qualifiedName == other.qualifiedName

	override fun hashCode(): Int = this.qualifiedName.hashCode()

	override fun toString(): String = this.qualifiedName

	private companion object {

		private val TYPES_WITHOUT_SUPERCLASS = setOf(ClassKind.INTERFACE, ClassKind.ANNOTATION_CLASS)

		private val SOURCE_ORIGINS = setOf(Origin.KOTLIN, Origin.JAVA)

	}

}
