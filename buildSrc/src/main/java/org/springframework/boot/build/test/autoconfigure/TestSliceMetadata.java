/*
 * Copyright 2012-2021 the original author or authors.
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import org.springframework.core.CollectionFactory;
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
public class TestSliceMetadata extends DefaultTask {

	private SourceSet sourceSet;

	private File outputFile;

	public TestSliceMetadata() {
		getInputs().dir((Callable<File>) () -> this.sourceSet.getOutput().getResourcesDir())
				.withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("resources");
		getInputs().files((Callable<FileCollection>) () -> this.sourceSet.getOutput().getClassesDirs())
				.withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("classes");
	}

	public void setSourceSet(SourceSet sourceSet) {
		this.sourceSet = sourceSet;
	}

	@OutputFile
	public File getOutputFile() {
		return this.outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
		Configuration testSliceMetadata = getProject().getConfigurations().maybeCreate("testSliceMetadata");
		getProject().getArtifacts().add(testSliceMetadata.getName(),
				getProject().provider((Callable<File>) this::getOutputFile), (artifact) -> artifact.builtBy(this));
	}

	@TaskAction
	void documentTestSlices() throws IOException {
		Properties testSlices = readTestSlices();
		getOutputFile().getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(getOutputFile())) {
			testSlices.store(writer, null);
		}
	}

	private Properties readTestSlices() throws IOException {
		Properties testSlices = CollectionFactory.createSortedProperties(true);
		try (URLClassLoader classLoader = new URLClassLoader(
				StreamSupport.stream(this.sourceSet.getRuntimeClasspath().spliterator(), false).map(this::toURL)
						.toArray(URL[]::new))) {
			MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory(classLoader);
			Properties springFactories = readSpringFactories(
					new File(this.sourceSet.getOutput().getResourcesDir(), "META-INF/spring.factories"));
			for (File classesDir : this.sourceSet.getOutput().getClassesDirs()) {
				addTestSlices(testSlices, classesDir, metadataReaderFactory, springFactories);
			}
		}
		return testSlices;
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
		try (Reader in = new FileReader(file)) {
			springFactories.load(in);
		}
		return springFactories;
	}

	private void addTestSlices(Properties testSlices, File classesDir, MetadataReaderFactory metadataReaderFactory,
			Properties springFactories) throws IOException {
		try (Stream<Path> classes = Files.walk(classesDir.toPath())) {
			classes.filter((path) -> path.toString().endsWith("Test.class"))
					.map((path) -> getMetadataReader(path, metadataReaderFactory))
					.filter((metadataReader) -> metadataReader.getClassMetadata().isAnnotation())
					.forEach((metadataReader) -> addTestSlice(testSlices, springFactories, metadataReader));
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

	private void addTestSlice(Properties testSlices, Properties springFactories, MetadataReader metadataReader) {
		testSlices.setProperty(metadataReader.getClassMetadata().getClassName(),
				StringUtils.collectionToCommaDelimitedString(
						getImportedAutoConfiguration(springFactories, metadataReader.getAnnotationMetadata())));
	}

	private SortedSet<String> getImportedAutoConfiguration(Properties springFactories,
			AnnotationMetadata annotationMetadata) {
		Stream<String> importers = findMetaImporters(annotationMetadata);
		if (annotationMetadata.isAnnotated("org.springframework.boot.autoconfigure.ImportAutoConfiguration")) {
			importers = Stream.concat(importers, Stream.of(annotationMetadata.getClassName()));
		}
		return importers.flatMap(
				(importer) -> StringUtils.commaDelimitedListToSet(springFactories.getProperty(importer)).stream())
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private Stream<String> findMetaImporters(AnnotationMetadata annotationMetadata) {
		return annotationMetadata.getAnnotationTypes().stream()
				.filter((annotationType) -> isAutoConfigurationImporter(annotationType, annotationMetadata));
	}

	private boolean isAutoConfigurationImporter(String annotationType, AnnotationMetadata metadata) {
		return metadata.getMetaAnnotationTypes(annotationType)
				.contains("org.springframework.boot.autoconfigure.ImportAutoConfiguration");
	}

}
