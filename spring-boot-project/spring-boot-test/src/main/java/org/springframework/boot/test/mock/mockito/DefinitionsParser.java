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

	DefinitionsParser() {
		this(Collections.emptySet());
	}

	DefinitionsParser(Collection<? extends Definition> existing) {
		this.definitions = new LinkedHashSet<>();
		this.definitionFields = new LinkedHashMap<>();
		if (existing != null) {
			this.definitions.addAll(existing);
		}
	}

	void parse(Class<?> source) {
		parseElement(source, null);
		ReflectionUtils.doWithFields(source, (element) -> parseElement(element, source));
	}

	private void parseElement(AnnotatedElement element, Class<?> source) {
		MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.SUPERCLASS);
		annotations.stream(MockBean.class)
			.map(MergedAnnotation::synthesize)
			.forEach((annotation) -> parseMockBeanAnnotation(annotation, element, source));
		annotations.stream(SpyBean.class)
			.map(MergedAnnotation::synthesize)
			.forEach((annotation) -> parseSpyBeanAnnotation(annotation, element, source));
	}

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

	private void addDefinition(AnnotatedElement element, Definition definition, String type) {
		boolean isNewDefinition = this.definitions.add(definition);
		Assert.state(isNewDefinition, () -> "Duplicate " + type + " definition " + definition);
		if (element instanceof Field field) {
			this.definitionFields.put(definition, field);
		}
	}

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

	Set<Definition> getDefinitions() {
		return Collections.unmodifiableSet(this.definitions);
	}

	Field getField(Definition definition) {
		return this.definitionFields.get(definition);
	}

}
