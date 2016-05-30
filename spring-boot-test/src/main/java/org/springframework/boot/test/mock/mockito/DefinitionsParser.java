/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.StringUtils;

/**
 * Parser to create {@link MockDefinition} and {@link SpyDefinition} instances from
 * {@link MockBean @MockBean} and {@link SpyBean @SpyBean} annotations declared on or in a
 * class.
 *
 * @author Phillip Webb
 */
class DefinitionsParser {

	private final Set<Definition> definitions;

	private final Map<Definition, Field> definitionFields;

	DefinitionsParser() {
		this(Collections.<Definition>emptySet());
	}

	DefinitionsParser(Collection<? extends Definition> existing) {
		this.definitions = new LinkedHashSet<Definition>();
		this.definitionFields = new LinkedHashMap<Definition, Field>();
		if (existing != null) {
			this.definitions.addAll(existing);
		}
	}

	public void parse(Class<?> source) {
		parseElement(source);
		ReflectionUtils.doWithFields(source, new FieldCallback() {

			@Override
			public void doWith(Field field)
					throws IllegalArgumentException, IllegalAccessException {
				parseElement(field);
			}

		});
	}

	private void parseElement(AnnotatedElement element) {
		for (MockBean annotation : AnnotationUtils.getRepeatableAnnotations(element,
				MockBean.class, MockBeans.class)) {
			parseMockBeanAnnotation(annotation, element);
		}
		for (SpyBean annotation : AnnotationUtils.getRepeatableAnnotations(element,
				SpyBean.class, SpyBeans.class)) {
			parseSpyBeanAnnotation(annotation, element);
		}
	}

	private void parseMockBeanAnnotation(MockBean annotation, AnnotatedElement element) {
		Set<Class<?>> classesToMock = getOrDeduceClasses(element, annotation.value());
		Assert.state(!classesToMock.isEmpty(),
				"Unable to deduce class to mock from " + element);
		if (StringUtils.hasLength(annotation.name())) {
			Assert.state(classesToMock.size() == 1,
					"The name attribute can only be used when mocking a single class");
		}
		for (Class<?> classToMock : classesToMock) {
			MockDefinition definition = new MockDefinition(annotation.name(), classToMock,
					annotation.extraInterfaces(), annotation.answer(),
					annotation.serializable(), annotation.reset(),
					annotation.proxyTargetAware());
			addDefinition(element, definition, "mock");
		}
	}

	private void parseSpyBeanAnnotation(SpyBean annotation, AnnotatedElement element) {
		Set<Class<?>> classesToSpy = getOrDeduceClasses(element, annotation.value());
		Assert.state(!classesToSpy.isEmpty(),
				"Unable to deduce class to spy from " + element);
		if (StringUtils.hasLength(annotation.name())) {
			Assert.state(classesToSpy.size() == 1,
					"The name attribute can only be used when spying a single class");
		}
		for (Class<?> classToSpy : classesToSpy) {
			SpyDefinition definition = new SpyDefinition(annotation.name(), classToSpy,
					annotation.reset(), annotation.proxyTargetAware());
			addDefinition(element, definition, "spy");
		}
	}

	private void addDefinition(AnnotatedElement element, Definition definition,
			String type) {
		boolean isNewDefinition = this.definitions.add(definition);
		Assert.state(isNewDefinition, "Duplicate " + type + " definition " + definition);
		if (element instanceof Field) {
			Field field = (Field) element;
			this.definitionFields.put(definition, field);
		}
	}

	private Set<Class<?>> getOrDeduceClasses(AnnotatedElement element, Class<?>[] value) {
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		classes.addAll(Arrays.asList(value));
		if (classes.isEmpty() && element instanceof Field) {
			classes.add(((Field) element).getType());
		}
		return classes;
	}

	public Set<Definition> getDefinitions() {
		return Collections.unmodifiableSet(this.definitions);
	}

	public Field getField(Definition definition) {
		return this.definitionFields.get(definition);
	}

}
