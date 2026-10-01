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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.configurationprocessor.ConfigurationPropertiesSourceResolver.SourceMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.model.AnnotationReference;
import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;

/**
 * Provide utilities to detect and validate configuration properties.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class MetadataGenerationEnvironment {

	private static final String INHERITED_ANNOTATION = "java.lang.annotation.Inherited";

	private static final Set<String> TYPE_EXCLUDES = Set.of("com.zaxxer.hikari.IConnectionCustomizer",
			"groovy.lang.MetaClass", "groovy.text.markup.MarkupTemplateEngine", "java.io.Writer", "java.io.PrintWriter",
			"java.lang.ClassLoader", "java.util.concurrent.ThreadFactory", "jakarta.jms.XAConnectionFactory",
			"javax.sql.DataSource", "javax.sql.XADataSource", "org.apache.tomcat.jdbc.pool.PoolConfiguration",
			"org.apache.tomcat.jdbc.pool.Validator", "org.flywaydb.core.api.callback.FlywayCallback",
			"org.flywaydb.core.api.resolver.MigrationResolver");

	private static final Set<String> DEPRECATION_EXCLUDES = Set.of(
			"org.apache.commons.dbcp2.BasicDataSource#getPassword",
			"org.apache.commons.dbcp2.BasicDataSource#getUsername");

	private final ProcessingContext context;

	private final ConfigurationPropertiesSourceResolver sourceResolver;

	private final Map<TypeDeclaration, Map<String, Object>> defaultValues = new HashMap<>();

	private final Map<TypeDeclaration, SourceMetadata> sources = new HashMap<>();

	private final String configurationPropertiesAnnotation;

	private final String nestedConfigurationPropertyAnnotation;

	private final String configurationPropertiesSourceAnnotation;

	private final String deprecatedConfigurationPropertyAnnotation;

	private final String constructorBindingAnnotation;

	private final String defaultValueAnnotation;

	private final Set<String> endpointAnnotations;

	private final String readOperationAnnotation;

	private final String nameAnnotation;

	private final String autowiredAnnotation;

	MetadataGenerationEnvironment(ProcessingContext context, String configurationPropertiesAnnotation,
			String configurationPropertiesSourceAnnotation, String nestedConfigurationPropertyAnnotation,
			String deprecatedConfigurationPropertyAnnotation, String constructorBindingAnnotation,
			String autowiredAnnotation, String defaultValueAnnotation, Set<String> endpointAnnotations,
			String readOperationAnnotation, String nameAnnotation) {
		this.context = context;
		this.sourceResolver = new ConfigurationPropertiesSourceResolver(context);
		this.configurationPropertiesAnnotation = configurationPropertiesAnnotation;
		this.configurationPropertiesSourceAnnotation = configurationPropertiesSourceAnnotation;
		this.nestedConfigurationPropertyAnnotation = nestedConfigurationPropertyAnnotation;
		this.deprecatedConfigurationPropertyAnnotation = deprecatedConfigurationPropertyAnnotation;
		this.constructorBindingAnnotation = constructorBindingAnnotation;
		this.autowiredAnnotation = autowiredAnnotation;
		this.defaultValueAnnotation = defaultValueAnnotation;
		this.endpointAnnotations = endpointAnnotations;
		this.readOperationAnnotation = readOperationAnnotation;
		this.nameAnnotation = nameAnnotation;
	}

	ProcessingContext getContext() {
		return this.context;
	}

	/**
	 * Return the default value of the given {@code field}.
	 * @param type the type to consider
	 * @param field the field or {@code null} if it is not available
	 * @return the default value or {@code null} if the field does not exist or no default
	 * value has been detected
	 */
	Object getFieldDefaultValue(TypeDeclaration type, VariableDeclaration field) {
		return (field != null) ? this.defaultValues.computeIfAbsent(type, this::resolveFieldValues).get(field.getName())
				: null;
	}

	/**
	 * Resolve the {@link SourceMetadata} for the specified property.
	 * @param field the field of the property (can be {@code null})
	 * @param getter the getter of the property (can be {@code null})
	 * @return the {@link SourceMetadata} for the specified property
	 */
	SourceMetadata resolveSourceMetadata(VariableDeclaration field, MethodDeclaration getter) {
		if (field != null && field.getEnclosingType() != null) {
			return this.sources.computeIfAbsent(field.getEnclosingType(), this.sourceResolver::resolveSource);
		}
		if (getter != null && getter.getEnclosingType() != null) {
			return this.sources.computeIfAbsent(getter.getEnclosingType(), this.sourceResolver::resolveSource);
		}
		return SourceMetadata.EMPTY;
	}

	/**
	 * Return the description of the given {@code element}.
	 * @param element the element or {@code null} if it is not available
	 * @return the description or {@code null} if the element is not documented
	 */
	String getDescription(Declaration element) {
		String javadoc = (element != null) ? element.getDocComment() : null;
		javadoc = (javadoc != null) ? cleanUpJavaDoc(javadoc) : null;
		return (javadoc == null || javadoc.isEmpty()) ? null : javadoc;
	}

	private String cleanUpJavaDoc(String javadoc) {
		StringBuilder result = new StringBuilder(javadoc.length());
		char lastChar = '.';
		for (int i = 0; i < javadoc.length(); i++) {
			char ch = javadoc.charAt(i);
			ch = (ch == '\r' || ch == '\n') ? ' ' : ch;
			boolean repeatedSpace = (ch == ' ' && lastChar == ' ');
			if (!repeatedSpace) {
				result.append(ch);
				lastChar = ch;
			}
		}
		return result.toString().trim();
	}

	boolean isExcluded(TypeReference type) {
		if (type == null) {
			return false;
		}
		String typeName = type.toString();
		if (typeName.endsWith("[]")) {
			typeName = typeName.substring(0, typeName.length() - 2);
		}
		return TYPE_EXCLUDES.contains(typeName);
	}

	boolean isDeprecated(Declaration element) {
		if (element == null) {
			return false;
		}
		TypeDeclaration enclosingType = element.getEnclosingType();
		if (enclosingType != null) {
			String elementName = enclosingType.getQualifiedName() + "#" + element.getName();
			if (DEPRECATION_EXCLUDES.contains(elementName)) {
				return false;
			}
		}
		if (isElementDeprecated(element)) {
			return true;
		}
		if (element instanceof VariableDeclaration || element instanceof MethodDeclaration) {
			return isElementDeprecated(enclosingType);
		}
		return false;
	}

	ItemDeprecation resolveItemDeprecation(Declaration element) {
		AnnotationReference annotation = getAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
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

	boolean hasConstructorBindingAnnotation(MethodDeclaration element) {
		return hasAnnotation(element, this.constructorBindingAnnotation, true);
	}

	boolean hasAutowiredAnnotation(MethodDeclaration element) {
		return hasAnnotation(element, this.autowiredAnnotation);
	}

	boolean hasAnnotation(Declaration element, String type) {
		return hasAnnotation(element, type, false);
	}

	boolean hasAnnotation(Declaration element, String type, boolean considerMetaAnnotations) {
		if (element != null) {
			if (getAnnotation(element, type) != null) {
				return true;
			}
			if (considerMetaAnnotations) {
				Set<TypeDeclaration> seen = new HashSet<>();
				for (AnnotationReference annotation : element.getAnnotations()) {
					if (hasMetaAnnotation(annotation.getType(), type, seen)) {
						return true;
					}
				}

			}
		}
		return false;
	}

	private boolean hasMetaAnnotation(TypeDeclaration annotationElement, String type, Set<TypeDeclaration> seen) {
		if (seen.add(annotationElement)) {
			for (AnnotationReference annotation : annotationElement.getAnnotations()) {
				TypeDeclaration annotationType = annotation.getType();
				if (type.equals(annotationType.getQualifiedName()) || hasMetaAnnotation(annotationType, type, seen)) {
					return true;
				}
			}
		}
		return false;
	}

	AnnotationReference getAnnotation(Declaration element, String type) {
		if (element != null) {
			for (AnnotationReference annotation : element.getAnnotations()) {
				if (type.equals(annotation.getType().getQualifiedName())) {
					return annotation;
				}
			}
		}
		return null;
	}

	/**
	 * Collect the annotations that are annotated or meta-annotated with the specified
	 * annotation.
	 * @param element the element to inspect
	 * @param annotationType the annotation to discover
	 * @return the annotations that are annotated or meta-annotated with this annotation
	 */
	List<TypeDeclaration> getElementsAnnotatedOrMetaAnnotatedWith(TypeDeclaration element, String annotationType) {
		LinkedList<TypeDeclaration> stack = new LinkedList<>();
		stack.push(element);
		collectElementsAnnotatedOrMetaAnnotatedWith(annotationType, stack);
		stack.removeFirst();
		return Collections.unmodifiableList(stack);
	}

	private boolean collectElementsAnnotatedOrMetaAnnotatedWith(String annotationType,
			LinkedList<TypeDeclaration> stack) {
		TypeDeclaration element = stack.peekLast();
		for (AnnotationReference annotation : getAllAnnotations(element)) {
			TypeDeclaration annotationElement = annotation.getType();
			if (!stack.contains(annotationElement)) {
				stack.addLast(annotationElement);
				if (annotationElement.getQualifiedName().equals(annotationType)) {
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
	 * Return the annotations that are present on the given {@code element}, either
	 * directly or because they are inherited from a superclass.
	 * @param element the element to inspect
	 * @return the annotations of the element
	 */
	private List<AnnotationReference> getAllAnnotations(TypeDeclaration element) {
		List<AnnotationReference> annotations = new ArrayList<>(element.getAnnotations());
		Set<TypeDeclaration> annotationTypes = new HashSet<>();
		annotations.forEach((annotation) -> annotationTypes.add(annotation.getType()));
		TypeDeclaration superType = element.getSuperclass();
		while (superType != null) {
			for (AnnotationReference annotation : superType.getAnnotations()) {
				TypeDeclaration annotationType = annotation.getType();
				if (hasAnnotation(annotationType, INHERITED_ANNOTATION) && annotationTypes.add(annotationType)) {
					annotations.add(annotation);
				}
			}
			superType = superType.getSuperclass();
		}
		return annotations;
	}

	Map<String, Object> getAnnotationElementValues(AnnotationReference annotation) {
		return annotation.getValues();
	}

	String getAnnotationElementStringValue(AnnotationReference annotation, String name) {
		return asString(annotation.getValues().get(name));
	}

	private String asString(Object value) {
		return (value == null || value.toString().isEmpty()) ? null : (String) value;
	}

	String getConfigurationPropertiesAnnotationName() {
		return this.configurationPropertiesAnnotation;
	}

	AnnotationReference getConfigurationPropertiesAnnotation(Declaration element) {
		return getAnnotation(element, this.configurationPropertiesAnnotation);
	}

	String getConfigurationPropertiesSourceAnnotationName() {
		return this.configurationPropertiesSourceAnnotation;
	}

	AnnotationReference getNestedConfigurationPropertyAnnotation(Declaration element) {
		return getAnnotation(element, this.nestedConfigurationPropertyAnnotation);
	}

	AnnotationReference getDefaultValueAnnotation(Declaration element) {
		return getAnnotation(element, this.defaultValueAnnotation);
	}

	Set<String> getEndpointAnnotationNames() {
		return this.endpointAnnotations;
	}

	AnnotationReference getReadOperationAnnotation(Declaration element) {
		return getAnnotation(element, this.readOperationAnnotation);
	}

	AnnotationReference getNameAnnotation(Declaration element) {
		return getAnnotation(element, this.nameAnnotation);
	}

	private boolean isElementDeprecated(Declaration element) {
		return element != null
				&& (element.isDeprecated() || hasAnnotation(element, this.deprecatedConfigurationPropertyAnnotation));
	}

	private Map<String, Object> resolveFieldValues(TypeDeclaration element) {
		Map<String, Object> values = new LinkedHashMap<>();
		resolveFieldValuesFor(values, element);
		return values;
	}

	private void resolveFieldValuesFor(Map<String, Object> values, TypeDeclaration element) {
		element.getFieldValues().forEach((name, value) -> {
			if (!values.containsKey(name)) {
				values.put(name, value);
			}
		});
		TypeDeclaration superType = element.getSuperclass();
		if (superType != null) {
			resolveFieldValuesFor(values, superType);
		}
	}

}
