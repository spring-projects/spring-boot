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

package org.springframework.boot.autoconfigureprocessor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Annotation processor to store certain annotations from auto-configuration classes in a
 * property file.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 1.5.0
 */
@SupportedAnnotationTypes({ "org.springframework.context.annotation.Configuration",
		"org.springframework.boot.autoconfigure.condition.ConditionalOnClass",
		"org.springframework.boot.autoconfigure.AutoConfigureBefore",
		"org.springframework.boot.autoconfigure.AutoConfigureAfter",
		"org.springframework.boot.autoconfigure.AutoConfigureOrder" })
public class AutoConfigureAnnotationProcessor extends AbstractProcessor {

	protected static final String PROPERTIES_PATH = "META-INF/" + "spring-autoconfigure-metadata.properties";

	private Map<String, String> annotations;

	private final Properties properties = new Properties();

	public AutoConfigureAnnotationProcessor() {
		Map<String, String> annotations = new LinkedHashMap<>();
		addAnnotations(annotations);
		this.annotations = Collections.unmodifiableMap(annotations);
	}

	protected void addAnnotations(Map<String, String> annotations) {
		annotations.put("Configuration", "org.springframework.context.annotation.Configuration");
		annotations.put("ConditionalOnClass", "org.springframework.boot.autoconfigure.condition.ConditionalOnClass");
		annotations.put("AutoConfigureBefore", "org.springframework.boot.autoconfigure.AutoConfigureBefore");
		annotations.put("AutoConfigureAfter", "org.springframework.boot.autoconfigure.AutoConfigureAfter");
		annotations.put("AutoConfigureOrder", "org.springframework.boot.autoconfigure.AutoConfigureOrder");
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (Map.Entry<String, String> entry : this.annotations.entrySet()) {
			process(roundEnv, entry.getKey(), entry.getValue());
		}
		if (roundEnv.processingOver()) {
			try {
				writeProperties();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
	}

	private void process(RoundEnvironment roundEnv, String propertyKey, String annotationName) {
		TypeElement annotationType = this.processingEnv.getElementUtils().getTypeElement(annotationName);
		if (annotationType != null) {
			for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
				Element enclosingElement = element.getEnclosingElement();
				if (enclosingElement != null && enclosingElement.getKind() == ElementKind.PACKAGE) {
					processElement(element, propertyKey, annotationName);
				}
			}
		}
	}

	private void processElement(Element element, String propertyKey, String annotationName) {
		try {
			String qualifiedName = getQualifiedName(element);
			AnnotationMirror annotation = getAnnotation(element, annotationName);
			if (qualifiedName != null && annotation != null) {
				List<Object> values = getValues(annotation);
				this.properties.put(qualifiedName + "." + propertyKey, toCommaDelimitedString(values));
				this.properties.put(qualifiedName, "");
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private AnnotationMirror getAnnotation(Element element, String type) {
		if (element != null) {
			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if (type.equals(annotation.getAnnotationType().toString())) {
					return annotation;
				}
			}
		}
		return null;
	}

	private String toCommaDelimitedString(List<Object> list) {
		StringBuilder result = new StringBuilder();
		for (Object item : list) {
			result.append((result.length() != 0) ? "," : "");
			result.append(item);
		}
		return result.toString();
	}

	private List<Object> getValues(AnnotationMirror annotation) {
		return annotation.getElementValues().entrySet().stream().filter(this::isNameOrValueAttribute)
				.flatMap(this::getValues).collect(Collectors.toList());
	}

	private boolean isNameOrValueAttribute(Entry<? extends ExecutableElement, ?> entry) {
		String attributeName = entry.getKey().getSimpleName().toString();
		return "name".equals(attributeName) || "value".equals(attributeName);
	}

	@SuppressWarnings("unchecked")
	private Stream<Object> getValues(Entry<?, ? extends AnnotationValue> entry) {
		Object value = entry.getValue().getValue();
		if (value instanceof List) {
			return ((List<AnnotationValue>) value).stream().map((annotation) -> processValue(annotation.getValue()));
		}
		return Stream.of(processValue(value));
	}

	private Object processValue(Object value) {
		if (value instanceof DeclaredType) {
			return getQualifiedName(((DeclaredType) value).asElement());
		}
		return value;
	}

	private String getQualifiedName(Element element) {
		if (element != null) {
			TypeElement enclosingElement = getEnclosingTypeElement(element.asType());
			if (enclosingElement != null) {
				return getQualifiedName(enclosingElement) + "$"
						+ ((DeclaredType) element.asType()).asElement().getSimpleName().toString();
			}
			if (element instanceof TypeElement) {
				return ((TypeElement) element).getQualifiedName().toString();
			}
		}
		return null;
	}

	private TypeElement getEnclosingTypeElement(TypeMirror type) {
		if (type instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) type;
			Element enclosingElement = declaredType.asElement().getEnclosingElement();
			if (enclosingElement != null && enclosingElement instanceof TypeElement) {
				return (TypeElement) enclosingElement;
			}
		}
		return null;
	}

	private void writeProperties() throws IOException {
		if (!this.properties.isEmpty()) {
			FileObject file = this.processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
					PROPERTIES_PATH);
			try (OutputStream outputStream = file.openOutputStream()) {
				this.properties.store(outputStream, null);
			}
		}
	}

}
