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

package org.springframework.boot.build.test.autoconfigure;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.test.autoconfigure.TestSliceMetadata.TestSlice;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.StringUtils;

/**
 * A {@link Task} for generating metadata describing a project's test slices.
 *
 * @author Andy Wilkinson
 */
public abstract class GenerateTestSliceMetadata extends DefaultTask {

	private final ObjectFactory objectFactory;

	private FileCollection classpath;

	private FileCollection importsFiles;

	private FileCollection classesDirs;

	@Inject
	public GenerateTestSliceMetadata(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
		this.getModuleName().convention(getProject().getName());
	}

	public void setSourceSet(SourceSet sourceSet) {
		this.classpath = sourceSet.getRuntimeClasspath();
		this.importsFiles = this.objectFactory.fileTree()
			.from(new File(sourceSet.getOutput().getResourcesDir(), "META-INF/spring"));
		this.importsFiles.filter((file) -> file.getName().endsWith(".imports"));
		getSpringFactories().set(new File(sourceSet.getOutput().getResourcesDir(), "META-INF/spring.factories"));
		this.classesDirs = sourceSet.getOutput().getClassesDirs();
	}

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	abstract RegularFileProperty getSpringFactories();

	@Input
	public abstract Property<String> getModuleName();

	@Classpath
	FileCollection getClasspath() {
		return this.classpath;
	}

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	FileCollection getImportFiles() {
		return this.importsFiles;
	}

	@Classpath
	FileCollection getClassesDirs() {
		return this.classesDirs;
	}

	@TaskAction
	void generateTestSliceMetadata() throws IOException {
		TestSliceMetadata metadata = readTestSlices();
		File outputFile = getOutputFile().getAsFile().get();
		outputFile.getParentFile().mkdirs();
		metadata.writeTo(outputFile);
	}

	private TestSliceMetadata readTestSlices() throws IOException {
		List<TestSlice> testSlices = new ArrayList<>();
		try (URLClassLoader classLoader = new URLClassLoader(
				StreamSupport.stream(this.classpath.spliterator(), false).map(this::toURL).toArray(URL[]::new))) {
			MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory(classLoader);
			Properties springFactories = readSpringFactories(getSpringFactories().getAsFile().getOrNull());
			readImportsFiles(springFactories, this.importsFiles);
			for (File classesDir : this.classesDirs) {
				testSlices.addAll(readTestSlices(classesDir, metadataReaderFactory, springFactories));
			}
		}
		return new TestSliceMetadata(getModuleName().get(), testSlices);
	}

	/**
	 * Reads the given imports files and puts them in springFactories. The key is the file
	 * name, the value is the file contents, split by line, delimited with a comma. This
	 * is done to mimic the spring.factories structure.
	 * @param springFactories spring.factories parsed as properties
	 * @param importsFiles the imports files to read
	 */
	private void readImportsFiles(Properties springFactories, FileCollection importsFiles) {
		for (File file : importsFiles.getFiles()) {
			try {
				List<String> lines = removeComments(Files.readAllLines(file.toPath()));
				String fileNameWithoutExtension = file.getName()
					.substring(0, file.getName().length() - ".imports".length());
				springFactories.setProperty(fileNameWithoutExtension,
						StringUtils.collectionToCommaDelimitedString(lines));
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to read file " + file, ex);
			}
		}
	}

	private List<String> removeComments(List<String> lines) {
		List<String> result = new ArrayList<>();
		for (String line : lines) {
			int commentIndex = line.indexOf('#');
			if (commentIndex > -1) {
				line = line.substring(0, commentIndex);
			}
			line = line.trim();
			if (!line.isEmpty()) {
				result.add(line);
			}
		}
		return result;
	}

	private URL toURL(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Properties readSpringFactories(File file) throws IOException {
		Properties springFactories = new Properties();
		if (file.isFile()) {
			try (Reader in = new FileReader(file)) {
				springFactories.load(in);
			}
		}
		return springFactories;
	}

	private List<TestSlice> readTestSlices(File classesDir, MetadataReaderFactory metadataReaderFactory,
			Properties springFactories) throws IOException {
		try (Stream<Path> classes = Files.walk(classesDir.toPath())) {
			return classes.filter((path) -> path.toString().endsWith("Test.class"))
				.map((path) -> getMetadataReader(path, metadataReaderFactory))
				.filter((metadataReader) -> metadataReader.getClassMetadata().isAnnotation())
				.map((metadataReader) -> readTestSlice(metadataReader, springFactories))
				.toList();
		}

	}

	private MetadataReader getMetadataReader(Path path, MetadataReaderFactory metadataReaderFactory) {
		try {
			return metadataReaderFactory.getMetadataReader(new FileSystemResource(path));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private TestSlice readTestSlice(MetadataReader metadataReader, Properties springFactories) {
		String annotationName = metadataReader.getClassMetadata().getClassName();
		List<String> importedAutoConfiguration = getImportedAutoConfiguration(springFactories,
				metadataReader.getAnnotationMetadata());
		return new TestSlice(annotationName, importedAutoConfiguration);
	}

	private List<String> getImportedAutoConfiguration(Properties springFactories,
			AnnotationMetadata annotationMetadata) {
		Stream<String> importers = findMetaImporters(annotationMetadata);
		if (annotationMetadata.isAnnotated("org.springframework.boot.autoconfigure.ImportAutoConfiguration")) {
			importers = Stream.concat(importers, Stream.of(annotationMetadata.getClassName()));
		}
		return importers
			.flatMap((importer) -> StringUtils.commaDelimitedListToSet(springFactories.getProperty(importer)).stream())
			.toList();
	}

	private Stream<String> findMetaImporters(AnnotationMetadata annotationMetadata) {
		return annotationMetadata.getAnnotationTypes()
			.stream()
			.filter((annotationType) -> isAutoConfigurationImporter(annotationType, annotationMetadata));
	}

	private boolean isAutoConfigurationImporter(String annotationType, AnnotationMetadata metadata) {
		return metadata.getMetaAnnotationTypes(annotationType)
			.contains("org.springframework.boot.autoconfigure.ImportAutoConfiguration");
	}

}
