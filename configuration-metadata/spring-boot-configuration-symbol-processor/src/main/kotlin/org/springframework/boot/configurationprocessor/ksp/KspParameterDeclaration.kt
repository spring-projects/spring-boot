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

import com.google.devtools.ksp.symbol.KSValueParameter
import org.springframework.boot.configurationprocessor.model.TypeDeclaration
import org.springframework.boot.configurationprocessor.model.TypeReference
import org.springframework.boot.configurationprocessor.model.VariableDeclaration

/**
 * [VariableDeclaration] backed by a [KSValueParameter].
 *
 * @author Areg Iazychian
 */
internal class KspParameterDeclaration(
	context: KspProcessingContext,
	private val parameter: KSValueParameter,
) : KspDeclaration(context, parameter), VariableDeclaration {

	override fun getName(): String = this.parameter.name?.asString().orEmpty()

	override fun getEnclosingType(): TypeDeclaration? = null

	override fun getType(): TypeReference = KspTypeReference(this.context, this.parameter.type.resolve())

	override fun isFinal(): Boolean = true

	// KSP does not expose the default value, see https://github.com/google/ksp/issues/1868
	override fun hasDefaultValue(): Boolean = this.parameter.hasDefault

}
