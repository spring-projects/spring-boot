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
import javax.lang.model.type.DeclaredType;
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
 * @author Scott Frederick
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
		excludes.add("jakarta.jms.XAConnectionFactory");
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

	private final String autowiredAnnotation;

	/**
     * Constructs a new MetadataGenerationEnvironment object.
     * 
     * @param environment the ProcessingEnvironment object used for processing annotations
     * @param configurationPropertiesAnnotation the fully qualified name of the annotation used for configuration properties
     * @param nestedConfigurationPropertyAnnotation the fully qualified name of the annotation used for nested configuration properties
     * @param deprecatedConfigurationPropertyAnnotation the fully qualified name of the annotation used for deprecated configuration properties
     * @param constructorBindingAnnotation the fully qualified name of the annotation used for constructor binding
     * @param autowiredAnnotation the fully qualified name of the annotation used for autowiring
     * @param defaultValueAnnotation the fully qualified name of the annotation used for default values
     * @param endpointAnnotations a set of fully qualified names of annotations used for defining endpoints
     * @param readOperationAnnotation the fully qualified name of the annotation used for read operations
     * @param nameAnnotation the fully qualified name of the annotation used for defining names
     */
    MetadataGenerationEnvironment(ProcessingEnvironment environment, String configurationPropertiesAnnotation,
			String nestedConfigurationPropertyAnnotation, String deprecatedConfigurationPropertyAnnotation,
			String constructorBindingAnnotation, String autowiredAnnotation, String defaultValueAnnotation,
			Set<String> endpointAnnotations, String readOperationAnnotation, String nameAnnotation) {
		this.typeUtils = new TypeUtils(environment);
		this.elements = environment.getElementUtils();
		this.messager = environment.getMessager();
		this.fieldValuesParser = resolveFieldValuesParser(environment);
		this.configurationPropertiesAnnotation = configurationPropertiesAnnotation;
		this.nestedConfigurationPropertyAnnotation = nestedConfigurationPropertyAnnotation;
		this.deprecatedConfigurationPropertyAnnotation = deprecatedConfigurationPropertyAnnotation;
		this.constructorBindingAnnotation = constructorBindingAnnotation;
		this.autowiredAnnotation = autowiredAnnotation;
		this.defaultValueAnnotation = defaultValueAnnotation;
		this.endpointAnnotations = endpointAnnotations;
		this.readOperationAnnotation = readOperationAnnotation;
		this.nameAnnotation = nameAnnotation;
	}

	/**
     * Resolves the field values parser based on the provided processing environment.
     * 
     * @param env the processing environment
     * @return the field values parser
     * @throws NullPointerException if the processing environment is null
     */
    private static FieldValuesParser resolveFieldValuesParser(ProcessingEnvironment env) {
		try {
			return new JavaCompilerFieldValuesParser(env);
		}
		catch (Throwable ex) {
			return FieldValuesParser.NONE;
		}
	}

	/**
     * Returns the TypeUtils object associated with this MetadataGenerationEnvironment.
     * 
     * @return the TypeUtils object
     */
    TypeUtils getTypeUtils() {
		return this.typeUtils;
	}

	/**
     * Returns the Messager object associated with this MetadataGenerationEnvironment.
     * 
     * @return the Messager object
     */
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

	/**
     * Checks if the given TypeMirror is excluded.
     * 
     * @param type the TypeMirror to check
     * @return true if the TypeMirror is excluded, false otherwise
     */
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

	/**
     * Checks if the given element is deprecated.
     * 
     * @param element the element to check
     * @return true if the element is deprecated, false otherwise
     */
    boolean isDeprecated(Element element) {
		if (isElementDeprecated(element)) {
			return true;
		}
		if (element instanceof VariableElement || element instanceof ExecutableElement) {
			return isElementDeprecated(element.getEnclosingElement());
		}
		return false;
	}

	/**
     * Resolves the deprecation information for an element.
     * 
     * @param element the element to resolve deprecation information for
     * @return the deprecation information for the element
     */
    ItemDeprecation resolveItemDeprecation(Element element) {
		AnnotationMirror annotation = getAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
		String reason = null;
		String replacement = null;
		String since = null;
		if (annotation != null) {
			reason = getAnnotationElementStringValue(annotation, "reason");
			replacement = getAnnotationElementStringValue(annotation, "replacement");
			since = getAnnotationElementStringValue(annotation, "since");
		}
		return new ItemDeprecation(reason, replacement, since);
	}

	/**
     * Checks if the given ExecutableElement has the constructor binding annotation.
     * 
     * @param element the ExecutableElement to check
     * @return true if the ExecutableElement has the constructor binding annotation, false otherwise
     */
    boolean hasConstructorBindingAnnotation(ExecutableElement element) {
		return hasAnnotation(element, this.constructorBindingAnnotation, true);
	}

	/**
     * Checks if the given ExecutableElement has the @Autowired annotation.
     * 
     * @param element the ExecutableElement to check
     * @return true if the ExecutableElement has the @Autowired annotation, false otherwise
     */
    boolean hasAutowiredAnnotation(ExecutableElement element) {
		return hasAnnotation(element, this.autowiredAnnotation);
	}

	/**
     * Checks if the given element has the specified annotation type.
     * 
     * @param element the element to check for annotation
     * @param type the fully qualified name of the annotation type
     * @return true if the element has the specified annotation type, false otherwise
     */
    boolean hasAnnotation(Element element, String type) {
		return hasAnnotation(element, type, false);
	}

	/**
     * Checks if the given element has an annotation of the specified type.
     * 
     * @param element              the element to check for annotations
     * @param type                 the fully qualified name of the annotation type
     * @param considerMetaAnnotations  flag indicating whether to consider meta-annotations
     * @return                     true if the element has the specified annotation, false otherwise
     */
    boolean hasAnnotation(Element element, String type, boolean considerMetaAnnotations) {
		if (element != null) {
			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if (type.equals(annotation.getAnnotationType().toString())) {
					return true;
				}
			}
			if (considerMetaAnnotations) {
				Set<Element> seen = new HashSet<>();
				for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
					if (hasMetaAnnotation(annotation.getAnnotationType().asElement(), type, seen)) {
						return true;
					}
				}

			}
		}
		return false;
	}

	/**
     * Checks if the given annotation element or any of its meta-annotations have the specified type.
     * 
     * @param annotationElement the annotation element to check
     * @param type the type of the meta-annotation to look for
     * @param seen a set of already seen elements to avoid infinite recursion
     * @return true if the annotation element or any of its meta-annotations have the specified type, false otherwise
     */
    private boolean hasMetaAnnotation(Element annotationElement, String type, Set<Element> seen) {
		if (seen.add(annotationElement)) {
			for (AnnotationMirror annotation : annotationElement.getAnnotationMirrors()) {
				DeclaredType annotationType = annotation.getAnnotationType();
				if (type.equals(annotationType.toString())
						|| hasMetaAnnotation(annotationType.asElement(), type, seen)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
     * Retrieves the annotation mirror of the specified type for the given element.
     * 
     * @param element the element to retrieve the annotation mirror from
     * @param type the fully qualified name of the annotation type
     * @return the annotation mirror of the specified type, or null if not found
     */
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

	/**
     * Recursively collects elements that are annotated or meta-annotated with the specified annotation type.
     * 
     * @param annotationType the type of annotation to search for
     * @param stack the stack of elements being processed
     * @return true if an element annotated with the specified annotation type is found, false otherwise
     */
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

	/**
     * Retrieves the element values of the given annotation.
     * 
     * @param annotation the annotation mirror to retrieve the element values from
     * @return a map containing the element names and their corresponding values
     */
    Map<String, Object> getAnnotationElementValues(AnnotationMirror annotation) {
		Map<String, Object> values = new LinkedHashMap<>();
		annotation.getElementValues()
			.forEach((name, value) -> values.put(name.getSimpleName().toString(), getAnnotationValue(value)));
		return values;
	}

	/**
     * Retrieves the string value of the specified element in the given annotation.
     *
     * @param annotation the annotation mirror to retrieve the value from
     * @param name the name of the element to retrieve the value for
     * @return the string value of the specified element, or null if not found
     */
    String getAnnotationElementStringValue(AnnotationMirror annotation, String name) {
		return annotation.getElementValues()
			.entrySet()
			.stream()
			.filter((element) -> element.getKey().getSimpleName().toString().equals(name))
			.map((element) -> asString(getAnnotationValue(element.getValue())))
			.findFirst()
			.orElse(null);
	}

	/**
     * Retrieves the value of an annotation.
     * 
     * @param annotationValue the annotation value to retrieve
     * @return the value of the annotation
     */
    private Object getAnnotationValue(AnnotationValue annotationValue) {
		Object value = annotationValue.getValue();
		if (value instanceof List) {
			List<Object> values = new ArrayList<>();
			((List<?>) value).forEach((v) -> values.add(((AnnotationValue) v).getValue()));
			return values;
		}
		return value;
	}

	/**
     * Converts an object to a string representation.
     * 
     * @param value the object to be converted
     * @return the string representation of the object, or null if the object is null or empty
     */
    private String asString(Object value) {
		return (value == null || value.toString().isEmpty()) ? null : (String) value;
	}

	/**
     * Returns the TypeElement representing the annotation used for configuration properties.
     * 
     * @return the TypeElement representing the configuration properties annotation
     */
    TypeElement getConfigurationPropertiesAnnotationElement() {
		return this.elements.getTypeElement(this.configurationPropertiesAnnotation);
	}

	/**
     * Retrieves the annotation mirror for the configuration properties annotation from the given element.
     * 
     * @param element the element from which to retrieve the annotation mirror
     * @return the annotation mirror for the configuration properties annotation, or null if not found
     */
    AnnotationMirror getConfigurationPropertiesAnnotation(Element element) {
		return getAnnotation(element, this.configurationPropertiesAnnotation);
	}

	/**
     * Retrieves the nested configuration property annotation mirror for the given element.
     * 
     * @param element the element for which to retrieve the annotation mirror
     * @return the nested configuration property annotation mirror, or null if not found
     */
    AnnotationMirror getNestedConfigurationPropertyAnnotation(Element element) {
		return getAnnotation(element, this.nestedConfigurationPropertyAnnotation);
	}

	/**
     * Retrieves the annotation mirror for the default value annotation of the specified element.
     * 
     * @param element the element for which to retrieve the default value annotation
     * @return the annotation mirror for the default value annotation, or null if not found
     */
    AnnotationMirror getDefaultValueAnnotation(Element element) {
		return getAnnotation(element, this.defaultValueAnnotation);
	}

	/**
     * Returns a set of TypeElement objects representing the endpoint annotations used in the MetadataGenerationEnvironment.
     * 
     * @return a set of TypeElement objects representing the endpoint annotations
     */
    Set<TypeElement> getEndpointAnnotationElements() {
		return this.endpointAnnotations.stream()
			.map(this.elements::getTypeElement)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	/**
     * Retrieves the annotation mirror for the read operation annotation on the specified element.
     * 
     * @param element the element to retrieve the annotation mirror from
     * @return the annotation mirror for the read operation annotation, or null if not found
     */
    AnnotationMirror getReadOperationAnnotation(Element element) {
		return getAnnotation(element, this.readOperationAnnotation);
	}

	/**
     * Retrieves the annotation mirror for the specified element's name annotation.
     * 
     * @param element the element for which to retrieve the annotation mirror
     * @return the annotation mirror for the element's name annotation, or null if not found
     */
    AnnotationMirror getNameAnnotation(Element element) {
		return getAnnotation(element, this.nameAnnotation);
	}

	/**
     * Checks if the given element has a nullable annotation.
     *
     * @param element the element to check
     * @return true if the element has a nullable annotation, false otherwise
     */
    boolean hasNullableAnnotation(Element element) {
		return getAnnotation(element, NULLABLE_ANNOTATION) != null;
	}

	/**
     * Checks if the given element is deprecated.
     * 
     * @param element the element to check
     * @return true if the element is deprecated, false otherwise
     */
    private boolean isElementDeprecated(Element element) {
		return hasAnnotation(element, "java.lang.Deprecated")
				|| hasAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
	}

	/**
     * Resolves the field values for the given TypeElement.
     * 
     * @param element the TypeElement for which to resolve the field values
     * @return a Map containing the resolved field values, with field names as keys and field values as values
     */
    private Map<String, Object> resolveFieldValues(TypeElement element) {
		Map<String, Object> values = new LinkedHashMap<>();
		resolveFieldValuesFor(values, element);
		return values;
	}

	/**
     * Resolves the field values for a given element and adds them to the provided map.
     * 
     * @param values the map to add the field values to
     * @param element the element to resolve the field values for
     */
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
