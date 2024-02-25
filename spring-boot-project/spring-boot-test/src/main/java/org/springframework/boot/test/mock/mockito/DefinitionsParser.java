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

package org.springframework.boot.test.mock.mockito;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Parser to create {@link MockDefinition} and {@link SpyDefinition} instances from
 * {@link MockBean @MockBean} and {@link SpyBean @SpyBean} annotations declared on or in a
 * class.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DefinitionsParser {

	private final Set<Definition> definitions;

	private final Map<Definition, Field> definitionFields;

	/**
     * Constructs a new DefinitionsParser object with an empty set of definitions.
     */
    DefinitionsParser() {
		this(Collections.emptySet());
	}

	/**
     * Constructs a new DefinitionsParser object with the given existing definitions.
     * 
     * @param existing the collection of existing definitions to be added to the parser (optional)
     */
    DefinitionsParser(Collection<? extends Definition> existing) {
		this.definitions = new LinkedHashSet<>();
		this.definitionFields = new LinkedHashMap<>();
		if (existing != null) {
			this.definitions.addAll(existing);
		}
	}

	/**
     * Parses the given source class and its fields.
     * 
     * @param source the class to be parsed
     */
    void parse(Class<?> source) {
		parseElement(source, null);
		ReflectionUtils.doWithFields(source, (element) -> parseElement(element, source));
	}

	/**
     * Parses the given annotated element and extracts the annotations of type MockBean and SpyBean.
     * 
     * @param element the annotated element to parse
     * @param source the source class from which the element is derived
     */
    private void parseElement(AnnotatedElement element, Class<?> source) {
		MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.SUPERCLASS);
		annotations.stream(MockBean.class)
			.map(MergedAnnotation::synthesize)
			.forEach((annotation) -> parseMockBeanAnnotation(annotation, element, source));
		annotations.stream(SpyBean.class)
			.map(MergedAnnotation::synthesize)
			.forEach((annotation) -> parseSpyBeanAnnotation(annotation, element, source));
	}

	/**
     * Parses the {@code MockBean} annotation and creates mock definitions based on the provided parameters.
     * 
     * @param annotation the {@code MockBean} annotation to parse
     * @param element the annotated element
     * @param source the source class
     * @throws IllegalStateException if unable to deduce the type to mock from the annotated element
     * @throws IllegalStateException if the name attribute is used when mocking multiple classes
     */
    private void parseMockBeanAnnotation(MockBean annotation, AnnotatedElement element, Class<?> source) {
		Set<ResolvableType> typesToMock = getOrDeduceTypes(element, annotation.value(), source);
		Assert.state(!typesToMock.isEmpty(), () -> "Unable to deduce type to mock from " + element);
		if (StringUtils.hasLength(annotation.name())) {
			Assert.state(typesToMock.size() == 1, "The name attribute can only be used when mocking a single class");
		}
		for (ResolvableType typeToMock : typesToMock) {
			MockDefinition definition = new MockDefinition(annotation.name(), typeToMock, annotation.extraInterfaces(),
					annotation.answer(), annotation.serializable(), annotation.reset(),
					QualifierDefinition.forElement(element));
			addDefinition(element, definition, "mock");
		}
	}

	/**
     * Parses the {@code @SpyBean} annotation and creates spy definitions for the annotated element.
     *
     * @param annotation the {@code @SpyBean} annotation
     * @param element the annotated element
     * @param source the source class
     * @throws IllegalStateException if unable to deduce the type to spy from the element
     * @throws IllegalStateException if the name attribute is used when spying multiple classes
     */
    private void parseSpyBeanAnnotation(SpyBean annotation, AnnotatedElement element, Class<?> source) {
		Set<ResolvableType> typesToSpy = getOrDeduceTypes(element, annotation.value(), source);
		Assert.state(!typesToSpy.isEmpty(), () -> "Unable to deduce type to spy from " + element);
		if (StringUtils.hasLength(annotation.name())) {
			Assert.state(typesToSpy.size() == 1, "The name attribute can only be used when spying a single class");
		}
		for (ResolvableType typeToSpy : typesToSpy) {
			SpyDefinition definition = new SpyDefinition(annotation.name(), typeToSpy, annotation.reset(),
					annotation.proxyTargetAware(), QualifierDefinition.forElement(element));
			addDefinition(element, definition, "spy");
		}
	}

	/**
     * Adds a definition to the DefinitionsParser.
     * 
     * @param element the annotated element to which the definition belongs
     * @param definition the definition to be added
     * @param type the type of the definition
     * 
     * @throws IllegalStateException if a duplicate definition is found
     */
    private void addDefinition(AnnotatedElement element, Definition definition, String type) {
		boolean isNewDefinition = this.definitions.add(definition);
		Assert.state(isNewDefinition, () -> "Duplicate " + type + " definition " + definition);
		if (element instanceof Field field) {
			this.definitionFields.put(definition, field);
		}
	}

	/**
     * Retrieves or deduces the types based on the provided annotated element, value, and source class.
     * 
     * @param element the annotated element to retrieve or deduce types from
     * @param value the array of classes to retrieve or deduce types from
     * @param source the source class to retrieve or deduce types from
     * @return a set of ResolvableType objects representing the retrieved or deduced types
     */
    private Set<ResolvableType> getOrDeduceTypes(AnnotatedElement element, Class<?>[] value, Class<?> source) {
		Set<ResolvableType> types = new LinkedHashSet<>();
		for (Class<?> clazz : value) {
			types.add(ResolvableType.forClass(clazz));
		}
		if (types.isEmpty() && element instanceof Field field) {
			types.add((field.getGenericType() instanceof TypeVariable) ? ResolvableType.forField(field, source)
					: ResolvableType.forField(field));
		}
		return types;
	}

	/**
     * Returns an unmodifiable set of definitions.
     * 
     * @return an unmodifiable set of definitions
     */
    Set<Definition> getDefinitions() {
		return Collections.unmodifiableSet(this.definitions);
	}

	/**
     * Retrieves the field associated with the given definition.
     * 
     * @param definition the definition for which the field is to be retrieved
     * @return the field associated with the given definition
     */
    Field getField(Definition definition) {
		return this.definitionFields.get(definition);
	}

}
