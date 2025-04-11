/*
 * Copyright 2012-2025 the original author or authors.
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

import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.test.RoundEnvironmentTester;
import org.springframework.boot.configurationprocessor.test.TestableAnnotationProcessor;
import org.springframework.boot.configurationsample.generic.AbstractGenericProperties;
import org.springframework.boot.configurationsample.generic.AbstractIntermediateGenericProperties;
import org.springframework.boot.configurationsample.generic.MixGenericNameProperties;
import org.springframework.boot.configurationsample.generic.SimpleGenericProperties;
import org.springframework.boot.configurationsample.generic.UnresolvedGenericProperties;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TypeUtils}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class TypeUtilsTests {

	@Test
	void resolveTypeOnConcreteClass() {
		process(SimpleGenericProperties.class, (roundEnv, typeUtils) -> {
			TypeElement typeElement = roundEnv.getRootElement(SimpleGenericProperties.class);
			assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(String.class.getName());
			assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
				.hasToString(constructMapType(Integer.class, Duration.class));
		});
	}

	@Test
	void resolveTypeOnIntermediateClass() {
		process(AbstractIntermediateGenericProperties.class, (roundEnv, typeUtils) -> {
			TypeElement typeElement = roundEnv.getRootElement(AbstractIntermediateGenericProperties.class);
			assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(String.class.getName());
			assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
				.hasToString(constructMapType(Integer.class, Object.class));
		});
	}

	@Test
	void resolveTypeWithOnlyGenerics() {
		process(AbstractGenericProperties.class, (roundEnv, typeUtils) -> {
			TypeElement typeElement = roundEnv.getRootElement(AbstractGenericProperties.class);
			assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(Object.class.getName());
			assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
				.hasToString(constructMapType(Object.class, Object.class));
		});
	}

	@Test
	void resolveTypeWithUnresolvedGenericProperties() {
		process(UnresolvedGenericProperties.class, (roundEnv, typeUtils) -> {
			TypeElement typeElement = roundEnv.getRootElement(UnresolvedGenericProperties.class);
			assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(String.class.getName());
			assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
				.hasToString(constructMapType(Number.class, Object.class));
		});
	}

	@Test
	void resolvedTypeMixGenericNamePropertiesProperties() {
		process(MixGenericNameProperties.class, (roundEnv, typeUtils) -> {
			TypeElement typeElement = roundEnv.getRootElement(MixGenericNameProperties.class);
			assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(String.class.getName());
			assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
				.hasToString(constructMapType(Number.class, Object.class));
		});
	}

	private void process(Class<?> target, BiConsumer<RoundEnvironmentTester, TypeUtils> consumer) {
		TestableAnnotationProcessor<TypeUtils> processor = new TestableAnnotationProcessor<>(consumer, TypeUtils::new);
		TestCompiler compiler = TestCompiler.forSystem()
			.withProcessors(processor)
			.withSources(SourceFile.forTestClass(target));
		compiler.compile((compiled) -> {
		});
	}

	private String constructMapType(Class<?> keyType, Class<?> valueType) {
		return "%s<%s,%s>".formatted(Map.class.getName(), keyType.getName(), valueType.getName());
	}

	private String getTypeOfField(TypeUtils typeUtils, TypeElement typeElement, String name) {
		TypeMirror field = findField(typeUtils, typeElement, name);
		if (field == null) {
			throw new IllegalStateException("Unable to find field '" + name + "' in " + typeElement);
		}
		return typeUtils.getType(typeElement, field);
	}

	private TypeMirror findField(TypeUtils typeUtils, TypeElement typeElement, String name) {
		for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
			if (variableElement.getSimpleName().contentEquals(name)) {
				return variableElement.asType();
			}
		}
		TypeMirror superclass = typeElement.getSuperclass();
		if (superclass != null && !superclass.toString().equals(Object.class.getName())) {
			return findField(typeUtils, (TypeElement) typeUtils.asElement(superclass), name);
		}
		return null;
	}

}
