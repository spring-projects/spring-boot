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

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

/**
 * Runs the [ConfigurationMetadataSymbolProcessor] over Kotlin sources using KSP itself and
 * gives access to the metadata that it produced.
 *
 * @author Areg Iazychian
 */
internal class MetadataCompiler {

	private lateinit var resourceOutputDir: File

	/**
	 * Compile the given [sources] and return the metadata that the processor produced,
	 * which is empty when the processor contributed no metadata at all.
	 */
	fun compile(vararg sources: SourceFile): ConfigurationMetadata = compile(emptyMap(), *sources)

	/**
	 * Compile the given [sources] and return the metadata that the processor produced for
	 * the `@ConfigurationPropertiesSource` type with the given [type] name.
	 */
	fun compileSourceMetadata(type: String, vararg sources: SourceFile): ConfigurationMetadata {
		compile(*sources)
		return readMetadata(File(this.resourceOutputDir, "META-INF/spring/configuration-metadata/$type.json"))
	}

	/**
	 * Compile the given [sources] with the given processor [options] and return the
	 * metadata that the processor produced.
	 */
	fun compile(options: Map<String, String>, vararg sources: SourceFile): ConfigurationMetadata {
		val workingDir = Files.createTempDirectory("symbol-processor").toFile()
		workingDir.deleteOnExit()
		val config = createConfig(workingDir, writeSources(workingDir, sources), options)
		this.resourceOutputDir = config.resourceOutputDir
		val logger = RecordingLogger()
		val processing = KotlinSymbolProcessing(config, listOf(ConfigurationMetadataSymbolProcessorProvider()), logger)
		val exitCode = processing.execute()
		check(exitCode == KotlinSymbolProcessing.ExitCode.OK) {
			"Processing failed:\n${logger.errors.joinToString(separator = "\n")}"
		}
		return readMetadata(File(config.resourceOutputDir, METADATA_PATH))
	}

	private fun writeSources(workingDir: File, sources: Array<out SourceFile>): File {
		val sourceRoot = File(workingDir, "src")
		sourceRoot.mkdirs()
		sources.forEach { File(sourceRoot, it.name).writeText(it.content) }
		return sourceRoot
	}

	private fun createConfig(workingDir: File, sourceRoot: File, options: Map<String, String>): KSPJvmConfig =
		KSPJvmConfig.Builder().apply {
			this.moduleName = "test"
			this.sourceRoots = listOf(sourceRoot)
			this.projectBaseDir = workingDir
			this.outputBaseDir = File(workingDir, "out")
			this.cachesDir = File(workingDir, "caches")
			this.classOutputDir = File(workingDir, "out/classes")
			this.kotlinOutputDir = File(workingDir, "out/kotlin")
			this.javaOutputDir = File(workingDir, "out/java")
			this.resourceOutputDir = File(workingDir, "out/resources")
			this.libraries = classpath()
			this.processorOptions = options
			this.jdkHome = File(System.getProperty("java.home"))
			this.jvmTarget = JVM_TARGET
			this.languageVersion = LANGUAGE_VERSION
			this.apiVersion = LANGUAGE_VERSION
		}.build()

	private fun classpath(): List<File> = System.getProperty("java.class.path")
		.split(File.pathSeparator)
		.map(::File)
		.filter(File::exists)

	private fun readMetadata(file: File): ConfigurationMetadata =
		if (file.isFile) file.inputStream().use { JsonMarshaller().read(it) } else ConfigurationMetadata()

	/**
	 * Return the metadata that the processor produced, rendered as JSON.
	 */
	fun compileToJson(vararg sources: SourceFile): String = compile(*sources).let { metadata ->
		ByteArrayOutputStream().also { JsonMarshaller().write(metadata, it) }.toString(Charsets.UTF_8)
	}

	private companion object {

		private const val METADATA_PATH = "META-INF/spring-configuration-metadata.json"

		private const val JVM_TARGET = "17"

		private const val LANGUAGE_VERSION = "2.2"

	}

}
