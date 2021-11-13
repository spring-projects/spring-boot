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

package org.springframework.boot.autoconfigureprocessor.ksp

import com.google.devtools.ksp.processing.Dependencies.Companion.ALL_FILES
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import org.springframework.boot.autoconfigureprocessor.ksp.visitor.AnnotationInfo
import org.springframework.boot.autoconfigureprocessor.ksp.visitor.NamedValuesExtractorVisitor
import org.springframework.boot.autoconfigureprocessor.ksp.visitor.OnBeanConditionValueExtractor
import org.springframework.boot.autoconfigureprocessor.ksp.visitor.OnClassConditionValueExtractorVisitor
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal const val PROPERTIES_FILE_NAME = "META-INF/spring-autoconfigure-metadata"
internal const val PROPERTIES_FILE_EXT = "properties"
internal const val PROPERTIES_FILE_FULL_NAME = "$PROPERTIES_FILE_NAME.$PROPERTIES_FILE_EXT"


/**
 * Kotlin Symbol Processor to store certain annotations from auto-configuration classes in a
 * property file.
 *
 * @author Pavel Pletnev
 * @since 2.6.0
 */
class AutoConfigureKotlinSymbolProcessor(
		private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

	private val propertiesExtractors: Map<AnnotationInfo, (k: KSAnnotated, info: AnnotationInfo) -> Map<String, String>> = mapOf(
			AnnotationInfo("org.springframework.boot.autoconfigure.condition.ConditionalOnClass") to { k: KSAnnotated, info: AnnotationInfo ->
				k.accept(OnClassConditionValueExtractorVisitor(info), Unit)
			},

			AnnotationInfo("org.springframework.boot.autoconfigure.condition.ConditionalOnBean") to { k: KSAnnotated, info: AnnotationInfo ->
				k.accept(OnBeanConditionValueExtractor(info), Unit)
			},

			AnnotationInfo("org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate") to { k: KSAnnotated, info: AnnotationInfo ->
				k.accept(OnBeanConditionValueExtractor(info), Unit)
			},


			AnnotationInfo("org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication") to { k: KSAnnotated, info: AnnotationInfo ->
				k.accept(NamedValuesExtractorVisitor(info, "type"), Unit)
			},

			AnnotationInfo("org.springframework.boot.autoconfigure.AutoConfigureBefore") to { k: KSAnnotated, info: AnnotationInfo ->
				k.accept(NamedValuesExtractorVisitor(info, "value", "name"), Unit)
			},

			AnnotationInfo("org.springframework.boot.autoconfigure.AutoConfigureAfter") to { k: KSAnnotated, info: AnnotationInfo ->
				k.accept(NamedValuesExtractorVisitor(info, "value", "name"), Unit)
			},

			AnnotationInfo("org.springframework.boot.autoconfigure.AutoConfigureOrder") to { k: KSAnnotated, info: AnnotationInfo ->
				k.accept(NamedValuesExtractorVisitor(info, "value"), Unit)
			}
	)


	override fun process(resolver: Resolver): List<KSAnnotated> {
		val properties: MutableMap<String, String> = mutableMapOf()

		propertiesExtractors.forEach { (info, extractor) ->
			val elements = resolver.getSymbolsWithAnnotation(info.qualifiedName, true)
			elements.forEach {
				properties.putAll(extractor.invoke(it, info))
			}
		}

		if (properties.isNotEmpty()) {
			environment.codeGenerator.createNewFile(
					ALL_FILES,
					"",
					PROPERTIES_FILE_NAME,
					PROPERTIES_FILE_EXT
			).use {
				OutputStreamWriter(it, StandardCharsets.UTF_8).use { writer ->
					for ((key, value) in properties.entries) {
						writer.append(key)
						writer.append("=")
						writer.append(value)
						writer.append(System.lineSeparator())
					}
				}
			}
		}

		return emptyList()
	}

}

class AutoConfigureSymbolProcessorProvider : SymbolProcessorProvider {

	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
		return AutoConfigureKotlinSymbolProcessor(environment)
	}
}