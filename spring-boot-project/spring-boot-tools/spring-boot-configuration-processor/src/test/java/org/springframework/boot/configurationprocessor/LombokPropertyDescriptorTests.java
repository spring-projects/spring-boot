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

package org.springframework.boot.configurationprocessor;

import java.io.IOException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationsample.lombok.LombokDefaultValueProperties;
import org.springframework.boot.configurationsample.lombok.LombokDeprecatedSingleProperty;
import org.springframework.boot.configurationsample.lombok.LombokExplicitProperties;
import org.springframework.boot.configurationsample.lombok.LombokInnerClassProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleDataProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleProperties;
import org.springframework.boot.configurationsample.simple.SimpleProperties;
import org.springframework.boot.configurationsample.specific.InnerClassProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LombokPropertyDescriptor}.
 *
 * @author Stephane Nicoll
 */
class LombokPropertyDescriptorTests extends PropertyDescriptorTests {

	@Test
	void lombokSimpleProperty() throws IOException {
		process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
			assertThat(property.getName()).isEqualTo("name");
			assertThat(property.getSource()).isSameAs(property.getField());
			assertThat(property.getField().getSimpleName()).hasToString("name");
			assertThat(property.isProperty(metadataEnv)).isTrue();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void lombokCollectionProperty() throws IOException {
		process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "items");
			assertThat(property.getName()).isEqualTo("items");
			assertThat(property.getSource()).isSameAs(property.getField());
			assertThat(property.getField().getSimpleName()).hasToString("items");
			assertThat(property.isProperty(metadataEnv)).isTrue();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void lombokNestedPropertySameClass() throws IOException {
		process(LombokInnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokInnerClassProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
			assertThat(property.getName()).isEqualTo("first");
			assertThat(property.getSource()).isSameAs(property.getField());
			assertThat(property.getField().getSimpleName()).hasToString("first");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isTrue();
		});
	}

	@Test
	void lombokNestedPropertyWithAnnotation() throws IOException {
		process(LombokInnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokInnerClassProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "third");
			assertThat(property.getName()).isEqualTo("third");
			assertThat(property.getSource()).isSameAs(property.getField());
			assertThat(property.getField().getSimpleName()).hasToString("third");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isTrue();
		});
	}

	@Test
	void lombokSimplePropertyWithOnlyGetterOnClassShouldNotBeExposed() throws IOException {
		process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignored");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void lombokSimplePropertyWithOnlyGetterOnDataClassShouldNotBeExposed() throws IOException {
		process(LombokSimpleDataProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleDataProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignored");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void lombokSimplePropertyWithOnlyGetterOnFieldShouldNotBeExposed() throws IOException {
		process(LombokExplicitProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokExplicitProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignoredOnlyGetter");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void lombokSimplePropertyWithOnlySetterOnFieldShouldNotBeExposed() throws IOException {
		process(LombokExplicitProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokExplicitProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignoredOnlySetter");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void lombokMetadataSimpleProperty() throws IOException {
		process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "description");
			assertItemMetadata(metadataEnv, property).isProperty().hasName("test.description").hasType(String.class)
					.hasSourceType(LombokSimpleProperties.class).hasNoDescription().isNotDeprecated();
		});
	}

	@Test
	void lombokMetadataCollectionProperty() throws IOException {
		process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "items");
			assertItemMetadata(metadataEnv, property).isProperty().hasName("test.items")
					.hasType("java.util.List<java.lang.String>").hasSourceType(LombokSimpleProperties.class)
					.hasNoDescription().isNotDeprecated();
		});
	}

	@Test
	void lombokMetadataNestedGroup() throws IOException {
		process(LombokInnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokInnerClassProperties.class);
			VariableElement field = getField(ownerElement, "third");
			ExecutableElement getter = getMethod(ownerElement, "getThird");
			LombokPropertyDescriptor property = new LombokPropertyDescriptor(ownerElement, null, field, "third",
					field.asType(), getter, null);
			assertItemMetadata(metadataEnv, property).isGroup().hasName("test.third")
					.hasType("org.springframework.boot.configurationsample.lombok.SimpleLombokPojo")
					.hasSourceType(LombokInnerClassProperties.class).hasSourceMethod("getThird()").hasNoDescription()
					.isNotDeprecated();
		});
	}

	@Test
	void lombokMetadataNestedGroupNoGetter() throws IOException {
		process(LombokInnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokInnerClassProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
			assertItemMetadata(metadataEnv, property).isGroup().hasName("test.first")
					.hasType("org.springframework.boot.configurationsample.lombok.LombokInnerClassProperties$Foo")
					.hasSourceType(LombokInnerClassProperties.class).hasSourceMethod(null).hasNoDescription()
					.isNotDeprecated();
		});
	}

	@Test
	void lombokMetadataNotACandidatePropertyShouldReturnNull() throws IOException {
		process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignored");
			assertThat(property.resolveItemMetadata("test", metadataEnv)).isNull();
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	void lombokDeprecatedPropertyOnClass() throws IOException {
		process(org.springframework.boot.configurationsample.lombok.LombokDeprecatedProperties.class,
				(roundEnv, metadataEnv) -> {
					TypeElement ownerElement = roundEnv.getRootElement(
							org.springframework.boot.configurationsample.lombok.LombokDeprecatedProperties.class);
					LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
					assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
				});
	}

	@Test
	void lombokDeprecatedPropertyOnField() throws IOException {
		process(LombokDeprecatedSingleProperty.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokDeprecatedSingleProperty.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
			assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
		});
	}

	@Test
	void lombokPropertyWithDescription() throws IOException {
		process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
			assertItemMetadata(metadataEnv, property).isProperty().hasDescription("Name description.");
		});
	}

	@Test
	void lombokPropertyWithDefaultValue() throws IOException {
		process(LombokDefaultValueProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(LombokDefaultValueProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "description");
			assertItemMetadata(metadataEnv, property).isProperty().hasDefaultValue("my description");
		});
	}

	@Test
	void lombokPropertyNotCandidate() throws IOException {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "theName");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void lombokNestedPropertyNotCandidate() throws IOException {
		process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(InnerClassProperties.class);
			LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	protected LombokPropertyDescriptor createPropertyDescriptor(TypeElement ownerElement, String name) {
		VariableElement field = getField(ownerElement, name);
		ExecutableElement getter = getMethod(ownerElement, createAccessorMethodName("get", name));
		ExecutableElement setter = getMethod(ownerElement, createAccessorMethodName("set", name));
		return new LombokPropertyDescriptor(ownerElement, null, field, name, field.asType(), getter, setter);
	}

}
