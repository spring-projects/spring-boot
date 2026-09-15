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

package org.springframework.boot.configurationprocessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;
import org.springframework.boot.configurationprocessor.test.TestConfigurationMetadataAnnotationProcessor;
import org.springframework.boot.configurationsample.incremental.BarProperties;
import org.springframework.boot.configurationsample.incremental.FooProperties;
import org.springframework.boot.configurationsample.record.ExampleRecord;
import org.springframework.core.test.tools.SourceFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DescriptionCache} verifying that property descriptions survive
 * Gradle-style incremental compilation where unchanged types arrive as {@code .class}
 * files (without javadoc).
 *
 * @author Agustin Palazzo
 */
class DescriptionCacheTests {

	private static final String METADATA_PATH = "META-INF/spring-configuration-metadata.json";

	@Test
	void incrementalBuildWithoutCacheLosesDescription(@TempDir Path tempDir) throws Exception {
		Path classOutput = tempDir.resolve("classes");
		Path sourceOutput = tempDir.resolve("sources");
		Files.createDirectories(classOutput);
		Files.createDirectories(sourceOutput);

		String classpath = System.getProperty("java.class.path");
		String fooSource = SourceFile.forTestClass(FooProperties.class).getContent();
		String barSource = SourceFile.forTestClass(BarProperties.class).getContent();

		List<JavaFileObject> allSources = List.of(
				inMemorySource("org.springframework.boot.configurationsample.incremental.FooProperties", fooSource),
				inMemorySource("org.springframework.boot.configurationsample.incremental.BarProperties", barSource));

		boolean ok = compile(classOutput, sourceOutput, classpath, null, allSources, null);
		assertThat(ok).as("Full compilation").isTrue();
		assertThat(descriptionOf(readMetadata(classOutput), "foo.counter")).isEqualTo("A nice counter description.");
		assertThat(descriptionOf(readMetadata(classOutput), "bar.counter")).isEqualTo("A nice counter description.");

		Files.deleteIfExists(classOutput.resolve(METADATA_PATH));

		String incrementalCp = classpath + File.pathSeparator + classOutput;
		List<JavaFileObject> barOnly = List.of(
				inMemorySource("org.springframework.boot.configurationsample.incremental.BarProperties", barSource));
		List<String> fooAsClass = List
			.of("org.springframework.boot.configurationsample.incremental.FooProperties");

		ok = compile(classOutput, sourceOutput, incrementalCp, null, barOnly, fooAsClass);
		assertThat(ok).as("Incremental compilation").isTrue();

		ConfigurationMetadata incremental = readMetadata(classOutput);
		assertThat(descriptionOf(incremental, "bar.counter")).isEqualTo("A nice counter description.");
		assertThat(descriptionOf(incremental, "foo.counter")).as("BUG: description lost without cache").isNull();
	}

	@Test
	void incrementalBuildWithCachePreservesDescription(@TempDir Path tempDir) throws Exception {
		Path classOutput = tempDir.resolve("classes");
		Path sourceOutput = tempDir.resolve("sources");
		Path cacheFile = tempDir.resolve("description-cache.json");
		Files.createDirectories(classOutput);
		Files.createDirectories(sourceOutput);

		String classpath = System.getProperty("java.class.path");
		String fooSource = SourceFile.forTestClass(FooProperties.class).getContent();
		String barSource = SourceFile.forTestClass(BarProperties.class).getContent();

		List<JavaFileObject> allSources = List.of(
				inMemorySource("org.springframework.boot.configurationsample.incremental.FooProperties", fooSource),
				inMemorySource("org.springframework.boot.configurationsample.incremental.BarProperties", barSource));

		boolean ok = compile(classOutput, sourceOutput, classpath, cacheFile, allSources, null);
		assertThat(ok).as("Full compilation").isTrue();
		assertThat(descriptionOf(readMetadata(classOutput), "foo.counter")).isEqualTo("A nice counter description.");
		assertThat(descriptionOf(readMetadata(classOutput), "bar.counter")).isEqualTo("A nice counter description.");
		assertThat(Files.exists(cacheFile)).as("Cache file created").isTrue();

		Files.deleteIfExists(classOutput.resolve(METADATA_PATH));

		String incrementalCp = classpath + File.pathSeparator + classOutput;
		List<JavaFileObject> barOnly = List.of(
				inMemorySource("org.springframework.boot.configurationsample.incremental.BarProperties", barSource));
		List<String> fooAsClass = List
			.of("org.springframework.boot.configurationsample.incremental.FooProperties");

		ok = compile(classOutput, sourceOutput, incrementalCp, cacheFile, barOnly, fooAsClass);
		assertThat(ok).as("Incremental compilation").isTrue();

		ConfigurationMetadata incremental = readMetadata(classOutput);
		assertThat(descriptionOf(incremental, "bar.counter")).isEqualTo("A nice counter description.");
		assertThat(descriptionOf(incremental, "foo.counter")).as("Description preserved from cache")
			.isEqualTo("A nice counter description.");
	}

	@Test
	void cacheIsPrunedWhenTypeIsRemoved(@TempDir Path tempDir) throws Exception {
		Path classOutput = tempDir.resolve("classes");
		Path sourceOutput = tempDir.resolve("sources");
		Path cacheFile = tempDir.resolve("description-cache.json");
		Files.createDirectories(classOutput);
		Files.createDirectories(sourceOutput);

		String classpath = System.getProperty("java.class.path");
		String fooSource = SourceFile.forTestClass(FooProperties.class).getContent();
		String barSource = SourceFile.forTestClass(BarProperties.class).getContent();

		List<JavaFileObject> allSources = List.of(
				inMemorySource("org.springframework.boot.configurationsample.incremental.FooProperties", fooSource),
				inMemorySource("org.springframework.boot.configurationsample.incremental.BarProperties", barSource));

		boolean ok = compile(classOutput, sourceOutput, classpath, cacheFile, allSources, null);
		assertThat(ok).as("Full compilation").isTrue();

		ConfigurationMetadata cache1 = readJson(cacheFile);
		assertThat(descriptionOf(cache1, "foo.counter")).isEqualTo("A nice counter description.");
		assertThat(descriptionOf(cache1, "bar.counter")).isEqualTo("A nice counter description.");

		Files.deleteIfExists(classOutput.resolve(METADATA_PATH));

		List<JavaFileObject> barOnly = List.of(
				inMemorySource("org.springframework.boot.configurationsample.incremental.BarProperties", barSource));
		ok = compile(classOutput, sourceOutput, classpath, cacheFile, barOnly, null);
		assertThat(ok).as("Compile without FooProperties").isTrue();

		ConfigurationMetadata cache2 = readJson(cacheFile);
		assertThat(descriptionOf(cache2, "bar.counter")).isEqualTo("A nice counter description.");
		assertThat(descriptionOf(cache2, "foo.counter")).as("Pruned from cache").isNull();
	}

	@Test
	void incrementalBuildWithCachePreservesRecordDescription(@TempDir Path tempDir) throws Exception {
		Path classOutput = tempDir.resolve("classes");
		Path sourceOutput = tempDir.resolve("sources");
		Path cacheFile = tempDir.resolve("description-cache.json");
		Files.createDirectories(classOutput);
		Files.createDirectories(sourceOutput);

		String classpath = System.getProperty("java.class.path");
		String recordSource = SourceFile.forTestClass(ExampleRecord.class).getContent();
		String barSource = SourceFile.forTestClass(BarProperties.class).getContent();

		List<JavaFileObject> allSources = List.of(
				inMemorySource("org.springframework.boot.configurationsample.record.ExampleRecord", recordSource),
				inMemorySource("org.springframework.boot.configurationsample.incremental.BarProperties", barSource));

		boolean ok = compile(classOutput, sourceOutput, classpath, cacheFile, allSources, null);
		assertThat(ok).as("Full compilation").isTrue();
		assertThat(descriptionOf(readMetadata(classOutput), "record.descriptions.some-string"))
			.isEqualTo("very long description that doesn't fit single line and is indented");

		Files.deleteIfExists(classOutput.resolve(METADATA_PATH));

		String incrementalCp = classpath + File.pathSeparator + classOutput;
		List<JavaFileObject> barOnly = List.of(
				inMemorySource("org.springframework.boot.configurationsample.incremental.BarProperties", barSource));
		List<String> recordAsClass = List
			.of("org.springframework.boot.configurationsample.record.ExampleRecord");

		ok = compile(classOutput, sourceOutput, incrementalCp, cacheFile, barOnly, recordAsClass);
		assertThat(ok).as("Incremental compilation").isTrue();

		ConfigurationMetadata incremental = readMetadata(classOutput);
		assertThat(descriptionOf(incremental, "bar.counter")).isEqualTo("A nice counter description.");
		assertThat(descriptionOf(incremental, "record.descriptions.some-string"))
			.as("Record description preserved from cache")
			.isEqualTo("very long description that doesn't fit single line and is indented");
	}

	@Test
	void corruptCacheFileIsIgnored(@TempDir Path tempDir) throws Exception {
		Path classOutput = tempDir.resolve("classes");
		Path sourceOutput = tempDir.resolve("sources");
		Path cacheFile = tempDir.resolve("description-cache.json");
		Files.createDirectories(classOutput);
		Files.createDirectories(sourceOutput);
		Files.writeString(cacheFile, "{ this is not valid json !!!");

		String classpath = System.getProperty("java.class.path");
		String fooSource = SourceFile.forTestClass(FooProperties.class).getContent();

		List<JavaFileObject> sources = List.of(
				inMemorySource("org.springframework.boot.configurationsample.incremental.FooProperties", fooSource));

		boolean ok = compile(classOutput, sourceOutput, classpath, cacheFile, sources, null);
		assertThat(ok).as("Compilation with corrupt cache").isTrue();

		ConfigurationMetadata metadata = readMetadata(classOutput);
		assertThat(metadata).isNotNull();
		assertThat(descriptionOf(metadata, "foo.counter")).isEqualTo("A nice counter description.");
	}

	private boolean compile(Path classOutput, Path sourceOutput, String classpath, Path cacheFile,
			List<JavaFileObject> sources, List<String> classes) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
			fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classOutput.toFile()));
			fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(sourceOutput.toFile()));
			fileManager.setLocation(StandardLocation.CLASS_PATH, classpathFiles(classpath));

			List<String> options = (cacheFile != null)
					? List.of("-A" + ConfigurationMetadataAnnotationProcessor.DESCRIPTION_CACHE_LOCATION_OPTION + "="
							+ cacheFile.toAbsolutePath())
					: Collections.emptyList();

			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, classes, sources);
			task.setProcessors(List.of(new TestConfigurationMetadataAnnotationProcessor()));
			return task.call();
		}
	}

	private List<File> classpathFiles(String classpath) {
		return Arrays.stream(classpath.split(File.pathSeparator)).map(File::new).toList();
	}

	private ConfigurationMetadata readJson(Path file) throws Exception {
		if (!Files.exists(file)) {
			return null;
		}
		try (InputStream in = Files.newInputStream(file)) {
			return new JsonMarshaller().read(in);
		}
	}

	private ConfigurationMetadata readMetadata(Path classOutput) throws Exception {
		return readJson(classOutput.resolve(METADATA_PATH));
	}

	private String descriptionOf(ConfigurationMetadata metadata, String propertyName) {
		if (metadata == null) {
			return null;
		}
		return metadata.getItems()
			.stream()
			.filter((item) -> item.isOfItemType(ItemMetadata.ItemType.PROPERTY))
			.filter((item) -> propertyName.equals(item.getName()))
			.findFirst()
			.map(ItemMetadata::getDescription)
			.orElse(null);
	}

	private JavaFileObject inMemorySource(String className, String content) {
		URI uri = URI.create("string:///" + className.replace('.', '/') + ".java");
		return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
			@Override
			public CharSequence getCharContent(boolean ignoreEncodingErrors) {
				return content;
			}
		};
	}

}
