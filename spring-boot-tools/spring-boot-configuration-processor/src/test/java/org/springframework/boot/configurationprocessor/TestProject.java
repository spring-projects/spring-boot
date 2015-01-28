/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.configurationprocessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.configurationprocessor.TestCompiler.TestCompilationTask;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.NestedConfigurationProperty;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import static org.springframework.boot.configurationprocessor.TestCompiler.ORIGINAL_SOURCE_FOLDER;
import static org.springframework.boot.configurationprocessor.TestCompiler.sourcePathFor;

/**
 * A TestProject contains a copy of a subset of test sample code.
 * <p>
 * Why a copy? Because when doing incremental build testing, we need to make modifications
 * to the contents of the 'test project'. But we don't want to actually modify the
 * original content itself.
 *
 * @author Kris De Volder
 */
public class TestProject {

	private static final Class<?>[] ALWAYS_INCLUDE = { ConfigurationProperties.class,
			NestedConfigurationProperty.class };

	/**
	 * Contains copies of the original source so we can modify it safely to test
	 * incremental builds.
	 */
	private File sourceFolder;
	private TestCompiler compiler;

	private Set<File> sourceFiles = new LinkedHashSet<File>();

	public TestProject(TemporaryFolder tempFolder, Class<?>... classes)
			throws IOException {
		this.sourceFolder = tempFolder.newFolder();
		this.compiler = new TestCompiler(tempFolder) {
			@Override
			protected File getSourceFolder() {
				return TestProject.this.sourceFolder;
			}
		};
		Set<Class<?>> contents = new HashSet<Class<?>>(Arrays.asList(classes));
		contents.addAll(Arrays.asList(ALWAYS_INCLUDE));
		copySources(contents);
	}

	private void copySources(Set<Class<?>> contents) throws IOException {
		for (Class<?> klass : contents) {
			copySources(klass);
		}
	}

	private void copySources(Class<?> klass) throws IOException {
		File original = getOriginalSourceFile(klass);
		File target = getSourceFile(klass);
		target.getParentFile().mkdirs();
		FileCopyUtils.copy(original, target);
		this.sourceFiles.add(target);
	}

	public File getSourceFile(Class<?> klass) {
		return new File(this.sourceFolder, sourcePathFor(klass));
	}

	public ConfigurationMetadata fullBuild() {
		TestConfigurationMetadataAnnotationProcessor processor = new TestConfigurationMetadataAnnotationProcessor(
				this.compiler.getOutputLocation());
		TestCompilationTask task = this.compiler.getTask(this.sourceFiles);
		deleteFolderContents(this.compiler.getOutputLocation());
		task.call(processor);
		return processor.getMetadata();
	}

	public ConfigurationMetadata incrementalBuild(Class<?>... toRecompile) {
		TestConfigurationMetadataAnnotationProcessor processor = new TestConfigurationMetadataAnnotationProcessor(
				this.compiler.getOutputLocation());
		TestCompilationTask task = this.compiler.getTask(toRecompile);
		task.call(processor);
		return processor.getMetadata();
	}

	private void deleteFolderContents(File outputFolder) {
		FileSystemUtils.deleteRecursively(outputFolder);
		outputFolder.mkdirs();
	}

	/**
	 * Retrieve File relative to project's output folder.
	 */
	public File getOutputFile(String relativePath) {
		Assert.assertFalse(new File(relativePath).isAbsolute());
		return new File(this.compiler.getOutputLocation(), relativePath);
	}

	/**
	 * Add source code at the end of file, just before last '}'
	 */
	public void addSourceCode(Class<?> target, InputStream snippetStream)
			throws Exception {
		File targetFile = getSourceFile(target);
		String contents = getContents(targetFile);
		int insertAt = contents.lastIndexOf('}');
		String additionalSource = FileCopyUtils.copyToString(new InputStreamReader(
				snippetStream));
		contents = contents.substring(0, insertAt) + additionalSource
				+ contents.substring(insertAt);
		putContents(targetFile, contents);
	}

	/**
	 * Delete source file for given class from project.
	 */
	public void delete(Class<?> klass) {
		File target = getSourceFile(klass);
		target.delete();
		this.sourceFiles.remove(target);
	}

	/**
	 * Restore source code of given class to its original contents.
	 */
	public void revert(Class<?> klass) throws IOException {
		Assert.assertTrue(getSourceFile(klass).exists());
		copySources(klass);
	}

	/**
	 * Add source code of given class to this project.
	 */
	public void add(Class<?> klass) throws IOException {
		Assert.assertFalse(getSourceFile(klass).exists());
		copySources(klass);
	}

	public void replaceText(Class<?> klass, String find, String replace) throws Exception {
		File target = getSourceFile(klass);
		String contents = getContents(target);
		contents = contents.replace(find, replace);
		putContents(target, contents);
	}

	/**
	 * Find the 'original' source code for given test class. Clients or subclasses should
	 * have no need to know about these. They should work only with the copied source
	 * code.
	 */
	private File getOriginalSourceFile(Class<?> klass) {
		return new File(ORIGINAL_SOURCE_FOLDER, sourcePathFor(klass));
	}

	private static void putContents(File targetFile, String contents)
			throws FileNotFoundException, IOException, UnsupportedEncodingException {
		FileCopyUtils.copy(new StringReader(contents), new FileWriter(targetFile));
	}

	private static String getContents(File file) throws Exception {
		return FileCopyUtils.copyToString(new FileReader(file));
	}
}
