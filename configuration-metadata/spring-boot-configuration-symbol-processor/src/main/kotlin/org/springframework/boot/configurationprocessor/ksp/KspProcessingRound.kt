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

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import org.springframework.boot.configurationprocessor.ProcessingRound
import org.springframework.boot.configurationprocessor.model.Declaration
import org.springframework.boot.configurationprocessor.model.TypeDeclaration

/**
 * [ProcessingRound] backed by a [Resolver].
 *
 * @author Areg Iazychian
 */
internal class KspProcessingRound(
	private val context: KspProcessingContext,
	private val resolver: Resolver,
) : ProcessingRound {

	override fun getDeclarationsAnnotatedWith(annotation: String): Collection<Declaration> =
		this.resolver.getSymbolsWithAnnotation(annotation)
			.mapNotNull { symbol ->
				when {
					symbol is KSClassDeclaration -> this.context.type(symbol)
					symbol is KSFunctionDeclaration && symbol.parentDeclaration is KSClassDeclaration ->
						KspMethodDeclaration(this.context, symbol)
					else -> null
				}
			}
			.toList()

	override fun getRootTypes(): Collection<TypeDeclaration> =
		this.resolver.getNewFiles()
			.flatMap { it.declarations }
			.filterIsInstance<KSClassDeclaration>()
			.map(this.context::type)
			.toList()

}
