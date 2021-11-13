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

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.Result
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.util.FileCopyUtils.copyToByteArray
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Tests for [AutoConfigureKotlinSymbolProcessor].
 *
 * @author Pavel Pletnev
 */
class AutoConfigureKotlinSymbolProcessorTest {

	private var compiler: KotlinCompilation? = null

	@BeforeEach
	fun createCompiler() {
		compiler = KotlinCompilation().apply {
			inheritClassPath = true
			symbolProcessorProviders = listOf(AutoConfigureSymbolProcessorProvider())
		}
	}

	@Test
	fun annotatedClass() {
		val properties: Properties = compileAndGetResultProperties("TestClassConfiguration.kt")!!

		assertThat(properties).hasSize(7)
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.ConditionalOnClass",
				"java.io.InputStream,org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.Nested,org.springframework.foo"
		)
		assertThat(properties).containsKey("org.springframework.boot.autoconfigureprocessor.TestClassConfiguration")
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.Nested",
				""
		)
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.Nested.AutoConfigureOrder",
				"0"
		)
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.ConditionalOnBean",
				"java.io.OutputStream"
		)
		// TODO IN KSP annotations args include default value
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.ConditionalOnSingleCandidate",
				"java.io.OutputStream,kotlin.Any"
		)

		// TODO IN KSP ENUM IS KSType, but IN JAP IS NOT DeclaredType
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestClassConfiguration.ConditionalOnWebApplication",
				"org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET"
		)
	}

	@Test
	fun annotatedClassWithOnBeanThatHasName() {
		val properties: Properties = compileAndGetResultProperties("TestOnBeanWithNameClassConfiguration.kt")!!

		assertThat(properties).hasSize(2)
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestOnBeanWithNameClassConfiguration.ConditionalOnBean",
				""
		)
	}

	@Test
	fun annotatedMethod() {
		val properties: Properties? = compileAndGetResultProperties("TestMethodConfiguration.kt")

		assertThat(properties).isNull()
	}

	@Test
	fun annotatedClassWithOrder() {
		val properties: Properties = compileAndGetResultProperties("TestOrderedClassConfiguration.kt")!!

		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestOrderedClassConfiguration.ConditionalOnClass",
				"java.io.InputStream,java.io.OutputStream"
		)
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestOrderedClassConfiguration.AutoConfigureBefore",
				"test.before1,test.before2"
		)
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestOrderedClassConfiguration.AutoConfigureAfter",
				"java.io.ObjectInputStream"
		)
		assertThat(properties).containsEntry(
				"org.springframework.boot.autoconfigureprocessor.TestOrderedClassConfiguration.AutoConfigureOrder",
				"123"
		)
	}

	@Test// gh-19370
	fun propertiesAreFullRepeatable() {
		val first = String(copyToByteArray(compileAndGetResultFile("TestOrderedClassConfiguration.kt")!!))
		val second = String(copyToByteArray(compileAndGetResultFile("TestOrderedClassConfiguration.kt")!!))

		assertThat(first).isEqualTo(second).doesNotContain("#")
	}

	private fun compileAndGetResultFile(fileName: String): File? {
		compiler?.apply {
			sources = listOf(kotlin(fileName, loadFileContent(fileName)))
		}

		return compiler?.compile()?.getResultFile()
	}

	private fun compileAndGetResultProperties(fileName: String): Properties? {
		return compileAndGetResultFile(fileName)?.let {
			loadProperties(it)
		}
	}

	private fun Result.getResultFile(): File {
		val workingDir: File = outputDirectory.parentFile
		val resultFile = workingDir.resolve("ksp")
				.resolve("sources")
				.resolve("resources")
				.resolve(PROPERTIES_FILE_FULL_NAME)


		if (!resultFile.exists()) {
			throw IllegalArgumentException("Not found result file: $PROPERTIES_FILE_FULL_NAME")
		}
		return resultFile
	}

	private fun loadProperties(file: File): Properties {
		return FileInputStream(file).use { inputStream ->
			Properties().also {
				it.load(inputStream)
			}
		}
	}

	private fun loadFileContent(name: String): String {
		return javaClass.classLoader.getResourceAsStream(name).use {
			String(it.readBytes(), StandardCharsets.UTF_8)
		}
	}


}