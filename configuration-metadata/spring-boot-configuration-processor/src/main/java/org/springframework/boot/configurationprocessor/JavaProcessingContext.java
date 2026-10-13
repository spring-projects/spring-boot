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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.springframework.boot.configurationprocessor.fieldvalues.FieldValuesParser;
import org.springframework.boot.configurationprocessor.fieldvalues.javac.JavaCompilerFieldValuesParser;
import org.springframework.boot.configurationprocessor.model.AnnotationReference;
import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;

/**
 * {@link ProcessingContext} backed by a {@link ProcessingEnvironment}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Areg Iazychian
 */
class JavaProcessingContext implements ProcessingContext {

	private static final String RESOURCES_DIRECTORY = "resources";

	private static final String CLASSES_DIRECTORY = "classes";

	private final ProcessingEnvironment environment;

	private final TypeUtils typeUtils;

	private final FieldValuesParser fieldValuesParser;

	JavaProcessingContext(ProcessingEnvironment environment) {
		this(environment, new TypeUtils(environment));
	}

	JavaProcessingContext(ProcessingEnvironment environment, TypeUtils typeUtils) {
		this.environment = environment;
		this.typeUtils = typeUtils;
		this.fieldValuesParser = resolveFieldValuesParser(environment);
	}

	private static FieldValuesParser resolveFieldValuesParser(ProcessingEnvironment env) {
		try {
			return new JavaCompilerFieldValuesParser(env);
		}
		catch (Throwable ex) {
			return FieldValuesParser.NONE;
		}
	}

	TypeUtils getTypeUtils() {
		return this.typeUtils;
	}

	FieldValuesParser getFieldValuesParser() {
		return this.fieldValuesParser;
	}

	TypeElement getTypeElement(String name) {
		return this.environment.getElementUtils().getTypeElement(name);
	}

	@Override
	public TypeDeclaration getTypeDeclaration(String name) {
		TypeElement typeElement = getTypeElement(name);
		return (typeElement != null) ? new JavaTypeDeclaration(this, typeElement) : null;
	}

	@Override
	public Map<String, String> getOptions() {
		return this.environment.getOptions();
	}

	@Override
	public void error(String message, Declaration declaration, AnnotationReference annotation) {
		Messager messager = this.environment.getMessager();
		Element element = (declaration instanceof JavaDeclaration<?> javaDeclaration) ? javaDeclaration.getElement()
				: null;
		AnnotationMirror annotationMirror = (annotation instanceof JavaAnnotationReference javaAnnotation)
				? javaAnnotation.getAnnotation() : null;
		if (element == null) {
			messager.printMessage(Kind.ERROR, message);
		}
		else if (annotationMirror == null) {
			messager.printMessage(Kind.ERROR, message, element);
		}
		else {
			messager.printMessage(Kind.ERROR, message, element, annotationMirror);
		}
	}

	@Override
	public void warn(String message) {
		this.environment.getMessager().printMessage(Kind.WARNING, message);
	}

	@Override
	public InputStream openResource(String location) throws IOException {
		return this.environment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", location).openInputStream();
	}

	@Override
	public OutputStream createResource(String location) throws IOException {
		return this.environment.getFiler()
			.createResource(StandardLocation.CLASS_OUTPUT, "", location)
			.openOutputStream();
	}

	@Override
	public InputStream openAdditionalMetadata(String location) throws IOException {
		// Most build systems will have copied the file to the class output location
		FileObject fileObject = this.environment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", location);
		InputStream inputStream = getMetadataStream(fileObject);
		if (inputStream != null) {
			return inputStream;
		}
		try {
			File file = locateAdditionalMetadataFile(new File(fileObject.toUri()), location);
			return (file.exists() ? new FileInputStream(file) : fileObject.toUri().toURL().openStream());
		}
		catch (Exception ex) {
			throw new FileNotFoundException();
		}
	}

	private InputStream getMetadataStream(FileObject fileObject) {
		try {
			return fileObject.openInputStream();
		}
		catch (IOException ex) {
			return null;
		}
	}

	File locateAdditionalMetadataFile(File standardLocation, String additionalMetadataLocation) throws IOException {
		if (standardLocation.exists()) {
			return standardLocation;
		}
		String locations = this.environment.getOptions()
			.get(ConfigurationMetadataAnnotationProcessor.ADDITIONAL_METADATA_LOCATIONS_OPTION);
		if (locations != null) {
			for (String location : locations.split(",")) {
				File candidate = new File(location, additionalMetadataLocation);
				if (candidate.isFile()) {
					return candidate;
				}
			}
		}
		return new File(locateGradleResourcesDirectory(standardLocation), additionalMetadataLocation);
	}

	private File locateGradleResourcesDirectory(File standardAdditionalMetadataLocation) throws FileNotFoundException {
		String path = standardAdditionalMetadataLocation.getPath();
		int index = path.lastIndexOf(CLASSES_DIRECTORY);
		if (index < 0) {
			throw new FileNotFoundException();
		}
		String buildDirectoryPath = path.substring(0, index);
		File classOutputLocation = standardAdditionalMetadataLocation.getParentFile().getParentFile();
		return new File(buildDirectoryPath, RESOURCES_DIRECTORY + '/' + classOutputLocation.getName());
	}

	@Override
	public InputStream openClasspathResource(String location) throws IOException {
		return this.environment.getFiler().getResource(StandardLocation.CLASS_PATH, "", location).openInputStream();
	}

}
