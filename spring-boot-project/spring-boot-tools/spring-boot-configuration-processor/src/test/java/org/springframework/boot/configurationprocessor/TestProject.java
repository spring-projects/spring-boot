/*
 * Copyright 2012-2023 the original author or authors.
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.test.CompiledMetadataReader;
import org.springframework.boot.configurationprocessor.test.TestConfigurationMetadataAnnotationProcessor;
import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.NestedConfigurationProperty;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.SourceFiles;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * A TestProject contains a copy of a subset of test sample code.
 * <p>
 * Why a copy? Because when doing incremental build testing, we need to make modifications
 * to the contents of the 'test project'. But we don't want to actually modify the
 * original content itself.
 *
 * @author Kris De Volder
 * @author Scott Frederick
 */
public class TestProject {

	private static final Class<?>[] ALWAYS_INCLUDE = { ConfigurationProperties.class,
			NestedConfigurationProperty.class };

	private SourceFiles sources;

	public TestProject(Class<?>... classes) {
		this.sources = SourceFiles.none().and(sourceFilesOf(ALWAYS_INCLUDE)).and(sourceFilesOf(classes));
	}

	public ConfigurationMetadata compile() {
		TestConfigurationMetadataAnnotationProcessor processor = new TestConfigurationMetadataAnnotationProcessor();
		TestCompiler compiler = TestCompiler.forSystem().withProcessors(processor);
		AtomicReference<ConfigurationMetadata> configurationMetadata = new AtomicReference<>();
		compiler.compile(this.sources,
				(compiled) -> configurationMetadata.set(CompiledMetadataReader.getMetadata(compiled)));
		return configurationMetadata.get();
	}

	/**
	 * Add source code at the end of file, just before last '}'
	 * @param target the target
	 * @param snippetStream the snippet stream
	 * @throws Exception if the source cannot be added
	 */
	public void addSourceCode(Class<?> target, InputStream snippetStream) throws Exception {
		SourceFile sourceFile = SourceFile.forTestClass(target);
		String contents = sourceFile.getContent();
		int insertAt = contents.lastIndexOf('}');
		String additionalSource = FileCopyUtils.copyToString(new InputStreamReader(snippetStream));
		contents = contents.substring(0, insertAt) + additionalSource + contents.substring(insertAt);
		this.sources = this.sources.and(SourceFile.of(contents));
	}

	/**
	 * Delete source file for given class from project.
	 * @param type the class to delete
	 */
	public void delete(Class<?> type) {
		SourceFile[] newSources = this.sources.stream()
			.filter((sourceFile) -> !sourceFile.getPath().equals(SourceFile.forTestClass(type).getPath()))
			.toArray(SourceFile[]::new);
		this.sources = SourceFiles.of(newSources);
	}

	/**
	 * Restore source code of given class to its original contents.
	 * @param type the class to revert
	 */
	public void revert(Class<?> type) {
		Assert.isTrue(this.sources.stream().anyMatch((sourceFile) -> sourceFile.getClassName().equals(type.getName())),
				"Source file for type '" + type + "' does not exist");
		this.sources = this.sources.and(SourceFile.forTestClass(type));
	}

	/**
	 * Add source code of given class to this project.
	 * @param type the class to add
	 */
	public void add(Class<?> type) {
		Assert.isTrue(this.sources.stream().noneMatch((sourceFile) -> sourceFile.getClassName().equals(type.getName())),
				"Source file for type '" + type + "' already exists");
		this.sources = this.sources.and(SourceFile.forTestClass(type));
	}

	public void replaceText(Class<?> type, String find, String replace) {
		SourceFile sourceFile = SourceFile.forTestClass(type);
		String contents = sourceFile.getContent().replace(find, replace);
		this.sources = this.sources.and(SourceFile.of(contents));
	}

	private List<SourceFile> sourceFilesOf(Class<?>... types) {
		return Arrays.stream(types).map(SourceFile::forTestClass).toList();
	}

}
