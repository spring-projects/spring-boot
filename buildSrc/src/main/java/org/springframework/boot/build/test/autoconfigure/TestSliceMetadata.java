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

package org.springframework.boot.build.test.autoconfigure;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

	/**
     * This method is used to test the slice metadata.
     * It sets the input directory to the resources directory of the source set's output.
     * The path sensitivity is set to RELATIVE.
     * The property name is set to "resources".
     * It depends on the process resources task of the source set.
     * It sets the input files to the classes directories of the source set's output.
     * The path sensitivity is set to RELATIVE.
     * The property name is set to "classes".
     */
    public TestSliceMetadata() {
		getInputs().dir((Callable<File>) () -> this.sourceSet.getOutput().getResourcesDir())
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName("resources");
		dependsOn((Callable<String>) () -> this.sourceSet.getProcessResourcesTaskName());
		getInputs().files((Callable<FileCollection>) () -> this.sourceSet.getOutput().getClassesDirs())
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName("classes");
	}

	/**
     * Sets the source set for the TestSliceMetadata.
     * 
     * @param sourceSet the source set to be set
     */
    public void setSourceSet(SourceSet sourceSet) {
		this.sourceSet = sourceSet;
	}

	/**
     * Returns the output file.
     *
     * @return the output file
     */
    @OutputFile
	public File getOutputFile() {
		return this.outputFile;
	}

	/**
     * Sets the output file for the TestSliceMetadata.
     * 
     * @param outputFile the output file to be set
     */
    public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
		Configuration testSliceMetadata = getProject().getConfigurations().maybeCreate("testSliceMetadata");
		getProject().getArtifacts()
			.add(testSliceMetadata.getName(), getProject().provider((Callable<File>) this::getOutputFile),
					(artifact) -> artifact.builtBy(this));
	}

	/**
     * This method is used to document the test slices.
     * It reads the test slices from a properties file and stores them in the output file.
     * If the output file's parent directory does not exist, it creates the directory.
     * 
     * @throws IOException if there is an error reading or writing the files
     */
    @TaskAction
	void documentTestSlices() throws IOException {
		Properties testSlices = readTestSlices();
		getOutputFile().getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(getOutputFile())) {
			testSlices.store(writer, null);
		}
	}

	/**
     * Reads the test slices from the source set's runtime classpath and returns them as properties.
     * 
     * @return the test slices as properties
     * @throws IOException if an I/O error occurs while reading the test slices
     */
    private Properties readTestSlices() throws IOException {
		Properties testSlices = CollectionFactory.createSortedProperties(true);
		try (URLClassLoader classLoader = new URLClassLoader(
				StreamSupport.stream(this.sourceSet.getRuntimeClasspath().spliterator(), false)
					.map(this::toURL)
					.toArray(URL[]::new))) {
			MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory(classLoader);
			Properties springFactories = readSpringFactories(
					new File(this.sourceSet.getOutput().getResourcesDir(), "META-INF/spring.factories"));
			readTestSlicesDirectory(springFactories,
					new File(this.sourceSet.getOutput().getResourcesDir(), "META-INF/spring/"));
			for (File classesDir : this.sourceSet.getOutput().getClassesDirs()) {
				addTestSlices(testSlices, classesDir, metadataReaderFactory, springFactories);
			}
		}
		return testSlices;
	}

	/**
	 * Reads files from the given directory and puts them in springFactories. The key is
	 * the file name, the value is the file contents, split by line, delimited with comma.
	 * This is done to mimic the spring.factories structure.
	 * @param springFactories spring.factories parsed as properties
	 * @param directory directory to scan
	 */
	private void readTestSlicesDirectory(Properties springFactories, File directory) {
		File[] files = directory.listFiles((dir, name) -> name.endsWith(".imports"));
		if (files == null) {
			return;
		}
		for (File file : files) {
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

	/**
     * Removes comments from a list of lines.
     * 
     * @param lines the list of lines containing comments
     * @return a new list of lines without comments
     */
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

	/**
     * Converts a File object to a URL object.
     * 
     * @param file the File object to be converted
     * @return the URL object representing the file's location
     * @throws RuntimeException if the file's URI cannot be converted to a URL
     */
    private URL toURL(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
     * Reads the Spring factories from the given file.
     * 
     * @param file the file to read the Spring factories from
     * @return the properties containing the Spring factories
     * @throws IOException if an I/O error occurs while reading the file
     */
    private Properties readSpringFactories(File file) throws IOException {
		Properties springFactories = new Properties();
		try (Reader in = new FileReader(file)) {
			springFactories.load(in);
		}
		return springFactories;
	}

	/**
     * Adds test slices to the provided properties.
     * 
     * @param testSlices            the properties to add the test slices to
     * @param classesDir            the directory containing the test classes
     * @param metadataReaderFactory the factory for creating metadata readers
     * @param springFactories       the properties containing the spring factories
     * @throws IOException if an I/O error occurs while walking the classes directory
     */
    private void addTestSlices(Properties testSlices, File classesDir, MetadataReaderFactory metadataReaderFactory,
			Properties springFactories) throws IOException {
		try (Stream<Path> classes = Files.walk(classesDir.toPath())) {
			classes.filter((path) -> path.toString().endsWith("Test.class"))
				.map((path) -> getMetadataReader(path, metadataReaderFactory))
				.filter((metadataReader) -> metadataReader.getClassMetadata().isAnnotation())
				.forEach((metadataReader) -> addTestSlice(testSlices, springFactories, metadataReader));
		}

	}

	/**
     * Retrieves the metadata reader for the given file path using the provided metadata reader factory.
     * 
     * @param path The path of the file for which the metadata reader is to be retrieved.
     * @param metadataReaderFactory The metadata reader factory used to create the metadata reader.
     * @return The metadata reader for the given file path.
     * @throws RuntimeException if an I/O error occurs while retrieving the metadata reader.
     */
    private MetadataReader getMetadataReader(Path path, MetadataReaderFactory metadataReaderFactory) {
		try {
			return metadataReaderFactory.getMetadataReader(new FileSystemResource(path));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
     * Adds a test slice to the given test slices properties.
     * 
     * @param testSlices the properties object to add the test slice to
     * @param springFactories the properties object containing the spring factories
     * @param metadataReader the metadata reader for the test slice
     */
    private void addTestSlice(Properties testSlices, Properties springFactories, MetadataReader metadataReader) {
		testSlices.setProperty(metadataReader.getClassMetadata().getClassName(),
				StringUtils.collectionToCommaDelimitedString(
						getImportedAutoConfiguration(springFactories, metadataReader.getAnnotationMetadata())));
	}

	/**
     * Retrieves the imported auto-configuration classes based on the provided Spring factories and annotation metadata.
     * 
     * @param springFactories The Spring factories properties.
     * @param annotationMetadata The annotation metadata.
     * @return A sorted set of imported auto-configuration classes.
     */
    private SortedSet<String> getImportedAutoConfiguration(Properties springFactories,
			AnnotationMetadata annotationMetadata) {
		Stream<String> importers = findMetaImporters(annotationMetadata);
		if (annotationMetadata.isAnnotated("org.springframework.boot.autoconfigure.ImportAutoConfiguration")) {
			importers = Stream.concat(importers, Stream.of(annotationMetadata.getClassName()));
		}
		return importers
			.flatMap((importer) -> StringUtils.commaDelimitedListToSet(springFactories.getProperty(importer)).stream())
			.collect(Collectors.toCollection(TreeSet::new));
	}

	/**
     * Finds the meta importers for the given annotation metadata.
     * 
     * @param annotationMetadata the annotation metadata to find meta importers for
     * @return a stream of meta importers as strings
     */
    private Stream<String> findMetaImporters(AnnotationMetadata annotationMetadata) {
		return annotationMetadata.getAnnotationTypes()
			.stream()
			.filter((annotationType) -> isAutoConfigurationImporter(annotationType, annotationMetadata));
	}

	/**
     * Checks if the given annotation type is an auto configuration importer.
     * 
     * @param annotationType the annotation type to check
     * @param metadata the annotation metadata
     * @return true if the annotation type is an auto configuration importer, false otherwise
     */
    private boolean isAutoConfigurationImporter(String annotationType, AnnotationMetadata metadata) {
		return metadata.getMetaAnnotationTypes(annotationType)
			.contains("org.springframework.boot.autoconfigure.ImportAutoConfiguration");
	}

}
