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
 * Parser to create {@link MockDefinition} from {@link MockBean @MockBean} annotations
 * declared on or in a class.
 *
 * @author Phillip Webb
 */
class MockDefinitionsParser {

	private final Set<MockDefinition> definitions;

	private final Map<MockDefinition, Field> fields;

	MockDefinitionsParser() {
		this(Collections.<MockDefinition>emptySet());
	}

	MockDefinitionsParser(Collection<? extends MockDefinition> existing) {
		this.definitions = new LinkedHashSet<MockDefinition>();
		this.fields = new LinkedHashMap<MockDefinition, Field>();
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
			parseAnnotation(annotation, element);
		}
	}

	private void parseAnnotation(MockBean annotation, AnnotatedElement element) {
		Set<Class<?>> classesToMock = getOrDeduceClassesToMock(annotation, element);
		Assert.state(!classesToMock.isEmpty(),
				"Unable to deduce class to mock from " + element);
		if (StringUtils.hasLength(annotation.name())) {
			Assert.state(classesToMock.size() == 1,
					"The name attribute can only be used when mocking a single class");
		}
		for (Class<?> classToMock : classesToMock) {
			MockDefinition definition = new MockDefinition(annotation.name(), classToMock,
					annotation.extraInterfaces(), annotation.answer(),
					annotation.serializable(), annotation.reset());
			boolean isNewDefinition = this.definitions.add(definition);
			Assert.state(isNewDefinition, "Duplicate mock definition " + definition);
			if (element instanceof Field) {
				this.fields.put(definition, (Field) element);
			}
		}
	}

	private Set<Class<?>> getOrDeduceClassesToMock(MockBean annotation,
			AnnotatedElement element) {
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		classes.addAll(Arrays.asList(annotation.value()));
		if (classes.isEmpty() && element instanceof Field) {
			classes.add(((Field) element).getType());
		}
		return classes;
	}

	public Set<MockDefinition> getDefinitions() {
		return Collections.unmodifiableSet(this.definitions);
	}

	public Field getField(MockDefinition definition) {
		return this.fields.get(definition);
	}

}
