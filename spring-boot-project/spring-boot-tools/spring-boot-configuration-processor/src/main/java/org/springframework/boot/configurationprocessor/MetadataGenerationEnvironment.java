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

package org.springframework.boot.configurationprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import org.springframework.boot.configurationprocessor.fieldvalues.FieldValuesParser;
import org.springframework.boot.configurationprocessor.fieldvalues.javac.JavaCompilerFieldValuesParser;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;

/**
 * Provide utilities to detect and validate configuration properties.
 *
 * @author Stephane Nicoll
 */
class MetadataGenerationEnvironment {

	private static final String NULLABLE_ANNOTATION = "org.springframework.lang.Nullable";

	private static final Set<String> TYPE_EXCLUDES;
	static {
		Set<String> excludes = new HashSet<>();
		excludes.add("com.zaxxer.hikari.IConnectionCustomizer");
		excludes.add("groovy.lang.MetaClass");
		excludes.add("groovy.text.markup.MarkupTemplateEngine");
		excludes.add("java.io.Writer");
		excludes.add("java.io.PrintWriter");
		excludes.add("java.lang.ClassLoader");
		excludes.add("java.util.concurrent.ThreadFactory");
		excludes.add("javax.jms.XAConnectionFactory");
		excludes.add("javax.sql.DataSource");
		excludes.add("javax.sql.XADataSource");
		excludes.add("org.apache.tomcat.jdbc.pool.PoolConfiguration");
		excludes.add("org.apache.tomcat.jdbc.pool.Validator");
		excludes.add("org.flywaydb.core.api.callback.FlywayCallback");
		excludes.add("org.flywaydb.core.api.resolver.MigrationResolver");
		TYPE_EXCLUDES = Collections.unmodifiableSet(excludes);
	}

	private final TypeUtils typeUtils;

	private final Elements elements;

	private final Messager messager;

	private final FieldValuesParser fieldValuesParser;

	private final Map<TypeElement, Map<String, Object>> defaultValues = new HashMap<>();

	private final String configurationPropertiesAnnotation;

	private final String nestedConfigurationPropertyAnnotation;

	private final String deprecatedConfigurationPropertyAnnotation;

	private final String constructorBindingAnnotation;

	private final String defaultValueAnnotation;

	private final Set<String> endpointAnnotations;

	private final String readOperationAnnotation;

	private final String nameAnnotation;

	MetadataGenerationEnvironment(ProcessingEnvironment environment, String configurationPropertiesAnnotation,
			String nestedConfigurationPropertyAnnotation, String deprecatedConfigurationPropertyAnnotation,
			String constructorBindingAnnotation, String defaultValueAnnotation, Set<String> endpointAnnotations,
			String readOperationAnnotation, String nameAnnotation) {
		this.typeUtils = new TypeUtils(environment);
		this.elements = environment.getElementUtils();
		this.messager = environment.getMessager();
		this.fieldValuesParser = resolveFieldValuesParser(environment);
		this.configurationPropertiesAnnotation = configurationPropertiesAnnotation;
		this.nestedConfigurationPropertyAnnotation = nestedConfigurationPropertyAnnotation;
		this.deprecatedConfigurationPropertyAnnotation = deprecatedConfigurationPropertyAnnotation;
		this.constructorBindingAnnotation = constructorBindingAnnotation;
		this.defaultValueAnnotation = defaultValueAnnotation;
		this.endpointAnnotations = endpointAnnotations;
		this.readOperationAnnotation = readOperationAnnotation;
		this.nameAnnotation = nameAnnotation;
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

	Messager getMessager() {
		return this.messager;
	}

	/**
	 * Return the default value of the field with the specified {@code name}.
	 * @param type the type to consider
	 * @param name the name of the field
	 * @return the default value or {@code null} if the field does not exist or no default
	 * value has been detected
	 */
	Object getFieldDefaultValue(TypeElement type, String name) {
		return this.defaultValues.computeIfAbsent(type, this::resolveFieldValues).get(name);
	}

	boolean isExcluded(TypeMirror type) {
		if (type == null) {
			return false;
		}
		String typeName = type.toString();
		if (typeName.endsWith("[]")) {
			typeName = typeName.substring(0, typeName.length() - 2);
		}
		return TYPE_EXCLUDES.contains(typeName);
	}

	boolean isDeprecated(Element element) {
		if (isElementDeprecated(element)) {
			return true;
		}
		if (element instanceof VariableElement || element instanceof ExecutableElement) {
			return isElementDeprecated(element.getEnclosingElement());
		}
		return false;
	}

	ItemDeprecation resolveItemDeprecation(Element element) {
		AnnotationMirror annotation = getAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
		String reason = null;
		String replacement = null;
		if (annotation != null) {
			Map<String, Object> elementValues = getAnnotationElementValues(annotation);
			reason = (String) elementValues.get("reason");
			replacement = (String) elementValues.get("replacement");
		}
		reason = (reason == null || reason.isEmpty()) ? null : reason;
		replacement = (replacement == null || replacement.isEmpty()) ? null : replacement;
		return new ItemDeprecation(reason, replacement);
	}

	boolean hasConstructorBindingAnnotation(TypeElement typeElement) {
		return hasAnnotationRecursive(typeElement, this.constructorBindingAnnotation);
	}

	boolean hasConstructorBindingAnnotation(ExecutableElement element) {
		return hasAnnotation(element, this.constructorBindingAnnotation);
	}

	boolean hasAnnotation(Element element, String type) {
		return getAnnotation(element, type) != null;
	}

	AnnotationMirror getAnnotation(Element element, String type) {
		if (element != null) {
			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if (type.equals(annotation.getAnnotationType().toString())) {
					return annotation;
				}
			}
		}
		return null;
	}

	/**
	 * Collect the annotations that are annotated or meta-annotated with the specified
	 * {@link TypeElement annotation}.
	 * @param element the element to inspect
	 * @param annotationType the annotation to discover
	 * @return the annotations that are annotated or meta-annotated with this annotation
	 */
	List<Element> getElementsAnnotatedOrMetaAnnotatedWith(Element element, TypeElement annotationType) {
		LinkedList<Element> stack = new LinkedList<>();
		stack.push(element);
		collectElementsAnnotatedOrMetaAnnotatedWith(annotationType, stack);
		stack.removeFirst();
		return Collections.unmodifiableList(stack);
	}

	private boolean hasAnnotationRecursive(Element element, String type) {
		return !getElementsAnnotatedOrMetaAnnotatedWith(element, this.elements.getTypeElement(type)).isEmpty();
	}

	private boolean collectElementsAnnotatedOrMetaAnnotatedWith(TypeElement annotationType, LinkedList<Element> stack) {
		Element element = stack.peekLast();
		for (AnnotationMirror annotation : this.elements.getAllAnnotationMirrors(element)) {
			Element annotationElement = annotation.getAnnotationType().asElement();
			if (!stack.contains(annotationElement)) {
				stack.addLast(annotationElement);
				if (annotationElement.equals(annotationType)) {
					return true;
				}
				if (!collectElementsAnnotatedOrMetaAnnotatedWith(annotationType, stack)) {
					stack.removeLast();
				}
			}
		}
		return false;
	}

	Map<String, Object> getAnnotationElementValues(AnnotationMirror annotation) {
		Map<String, Object> values = new LinkedHashMap<>();
		annotation.getElementValues()
				.forEach((name, value) -> values.put(name.getSimpleName().toString(), getAnnotationValue(value)));
		return values;
	}

	private Object getAnnotationValue(AnnotationValue annotationValue) {
		Object value = annotationValue.getValue();
		if (value instanceof List) {
			List<Object> values = new ArrayList<>();
			((List<?>) value).forEach((v) -> values.add(((AnnotationValue) v).getValue()));
			return values;
		}
		return value;
	}

	TypeElement getConfigurationPropertiesAnnotationElement() {
		return this.elements.getTypeElement(this.configurationPropertiesAnnotation);
	}

	AnnotationMirror getConfigurationPropertiesAnnotation(Element element) {
		return getAnnotation(element, this.configurationPropertiesAnnotation);
	}

	AnnotationMirror getNestedConfigurationPropertyAnnotation(Element element) {
		return getAnnotation(element, this.nestedConfigurationPropertyAnnotation);
	}

	AnnotationMirror getDefaultValueAnnotation(Element element) {
		return getAnnotation(element, this.defaultValueAnnotation);
	}

	Set<TypeElement> getEndpointAnnotationElements() {
		return this.endpointAnnotations.stream().map(this.elements::getTypeElement).filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	AnnotationMirror getReadOperationAnnotation(Element element) {
		return getAnnotation(element, this.readOperationAnnotation);
	}

	AnnotationMirror getNameAnnotation(Element element) {
		return getAnnotation(element, this.nameAnnotation);
	}

	boolean hasNullableAnnotation(Element element) {
		return getAnnotation(element, NULLABLE_ANNOTATION) != null;
	}

	private boolean isElementDeprecated(Element element) {
		return hasAnnotation(element, "java.lang.Deprecated")
				|| hasAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
	}

	private Map<String, Object> resolveFieldValues(TypeElement element) {
		Map<String, Object> values = new LinkedHashMap<>();
		resolveFieldValuesFor(values, element);
		return values;
	}

	private void resolveFieldValuesFor(Map<String, Object> values, TypeElement element) {
		try {
			this.fieldValuesParser.getFieldValues(element).forEach((name, value) -> {
				if (!values.containsKey(name)) {
					values.put(name, value);
				}
			});
		}
		catch (Exception ex) {
			// continue
		}
		Element superType = this.typeUtils.asElement(element.getSuperclass());
		if (superType instanceof TypeElement && superType.asType().getKind() != TypeKind.NONE) {
			resolveFieldValuesFor(values, (TypeElement) superType);
		}
	}

}
