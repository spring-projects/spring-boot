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

import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import org.springframework.boot.configurationprocessor.model.MethodDeclaration
import org.springframework.boot.configurationprocessor.model.TypeDeclaration
import org.springframework.boot.configurationprocessor.model.TypeReference
import org.springframework.boot.configurationprocessor.model.VariableDeclaration

/**
 * [MethodDeclaration] backed by a [KSFunctionDeclaration].
 *
 * @author Areg Iazychian
 */
internal class KspMethodDeclaration(
	context: KspProcessingContext,
	private val function: KSFunctionDeclaration,
) : KspDeclaration(context, function), MethodDeclaration {

	override fun getName(): String = this.function.simpleName.asString()

	override fun getEnclosingType(): TypeDeclaration? =
		(this.function.parentDeclaration as? KSClassDeclaration)?.let(this.context::type)

	override fun getParameters(): List<VariableDeclaration> =
		this.function.parameters.map { KspParameterDeclaration(this.context, it) }

	override fun getReturnType(): TypeReference = KspTypeReference.ofReturnType(this.context, this.function)

	override fun getSignature(): String = this.name + this.parameters.joinToString(",", "(", ")") { it.type.toString() }

	override fun isPublic(): Boolean = this.function.isPublic()

	override fun isPrivate(): Boolean = this.function.isPrivate()

	override fun isAbstract(): Boolean = this.function.isAbstract

	override fun isStatic(): Boolean = Modifier.JAVA_STATIC in this.function.modifiers

	override fun toString(): String = this.signature

}
