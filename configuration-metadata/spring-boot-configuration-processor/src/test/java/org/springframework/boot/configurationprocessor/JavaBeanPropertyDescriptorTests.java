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

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;
import org.springframework.boot.configurationsample.simple.DeprecatedSingleProperty;
import org.springframework.boot.configurationsample.simple.SimpleCollectionProperties;
import org.springframework.boot.configurationsample.simple.SimpleProperties;
import org.springframework.boot.configurationsample.simple.SimpleTypeProperties;
import org.springframework.boot.configurationsample.specific.InnerClassProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JavaBeanPropertyDescriptor}.
 *
 * @author Stephane Nicoll
 */
class JavaBeanPropertyDescriptorTests extends PropertyDescriptorTests {

	@Test
	void javaBeanSimpleProperty() {
		process(SimpleTypeProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleTypeProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "myString");
			assertThat(property.getName()).isEqualTo("myString");
			assertThat(property.getGetter().getName()).isEqualTo("getMyString");
			assertThat(property.getSetter().getName()).isEqualTo("setMyString");
			assertThat(property.isProperty(metadataEnv)).isTrue();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void getSourceElementReturnsPublicGetter() {
		process(SimpleTypeProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleTypeProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "myString");
			assertThat(property.getSourceElement()).isInstanceOf(MethodDeclaration.class);
			assertThat(property.getSourceElement()).isSameAs(property.getGetter());
		});
	}

	@Test
	void javaBeanCollectionProperty() {
		process(SimpleCollectionProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleCollectionProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "doubles");
			assertThat(property.getName()).isEqualTo("doubles");
			assertThat(property.getGetter().getName()).isEqualTo("getDoubles");
			assertThat(property.getSetter()).isNull();
			assertThat(property.isProperty(metadataEnv)).isTrue();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void javaBeanNestedPropertySameClass() {
		process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(InnerClassProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
			assertThat(property.getName()).isEqualTo("first");
			assertThat(property.getGetter().getName()).isEqualTo("getFirst");
			assertThat(property.getSetter()).isNull();
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isTrue();
		});
	}

	@Test
	void javaBeanNestedPropertyWithAnnotation() {
		process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(InnerClassProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "third");
			assertThat(property.getName()).isEqualTo("third");
			assertThat(property.getGetter().getName()).isEqualTo("getThird");
			assertThat(property.getSetter()).isNull();
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isTrue();
		});
	}

	@Test
	void javaBeanSimplePropertyWithOnlyGetterShouldNotBeExposed() {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			MethodDeclaration getter = getMethod(ownerElement, "getSize");
			VariableDeclaration field = getField(ownerElement, "size");
			JavaBeanPropertyDescriptor property = new JavaBeanPropertyDescriptor("size", field.getType(), ownerElement,
					getter, null, field, getter);
			assertThat(property.getName()).isEqualTo("size");
			assertThat(property.getGetter().getName()).isEqualTo("getSize");
			assertThat(property.getSetter()).isNull();
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void javaBeanSimplePropertyWithOnlySetterShouldNotBeExposed() {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			VariableDeclaration field = getField(ownerElement, "counter");
			JavaBeanPropertyDescriptor property = new JavaBeanPropertyDescriptor("counter", field.getType(),
					ownerElement, null, getMethod(ownerElement, "setCounter"), field, null);
			assertThat(property.getName()).isEqualTo("counter");
			assertThat(property.getGetter()).isNull();
			assertThat(property.getSetter().getName()).isEqualTo("setCounter");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	void javaBeanMetadataSimpleProperty() {
		process(SimpleTypeProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleTypeProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "myString");
			assertItemMetadata(metadataEnv, property).isProperty()
				.hasName("test.my-string")
				.hasType(String.class)
				.hasSourceType(SimpleTypeProperties.class)
				.hasNoDescription()
				.isNotDeprecated();
		});
	}

	@Test
	void javaBeanMetadataCollectionProperty() {
		process(SimpleCollectionProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleCollectionProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "doubles");
			assertItemMetadata(metadataEnv, property).isProperty()
				.hasName("test.doubles")
				.hasType("java.util.List<java.lang.Double>")
				.hasSourceType(SimpleCollectionProperties.class)
				.hasNoDescription()
				.isNotDeprecated();
		});
	}

	@Test
	void javaBeanMetadataNestedGroup() {
		process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(InnerClassProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
			assertItemMetadata(metadataEnv, property).isGroup()
				.hasName("test.first")
				.hasType("org.springframework.boot.configurationsample.specific.InnerClassProperties$Foo")
				.hasSourceType(InnerClassProperties.class)
				.hasSourceMethod("getFirst()")
				.hasNoDescription()
				.isNotDeprecated();
		});
	}

	@Test
	void javaBeanMetadataNotACandidatePropertyShouldReturnNull() {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			VariableDeclaration field = getField(ownerElement, "counter");
			JavaBeanPropertyDescriptor property = new JavaBeanPropertyDescriptor("counter", field.getType(),
					ownerElement, null, getMethod(ownerElement, "setCounter"), field, null);
			assertThat(property.resolveItemMetadata("test", metadataEnv)).isNull();
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	void javaBeanDeprecatedPropertyOnClass() {
		process(org.springframework.boot.configurationsample.simple.DeprecatedProperties.class,
				(roundEnv, metadataEnv) -> {
					TypeDeclaration ownerElement = roundEnv
						.getRootElement(org.springframework.boot.configurationsample.simple.DeprecatedProperties.class);
					JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
					assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
				});
	}

	@Test
	void javaBeanMetadataDeprecatedPropertyWithAnnotation() {
		process(DeprecatedSingleProperty.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(DeprecatedSingleProperty.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
			assertItemMetadata(metadataEnv, property).isProperty()
				.isDeprecatedWithReason("renamed")
				.isDeprecatedWithReplacement("singledeprecated.new-name");
		});
	}

	@Test
	void javaBeanDeprecatedPropertyOnGetter() {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "flag", "isFlag", "setFlag");
			assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
		});
	}

	@Test
	void javaBeanDeprecatedPropertyOnSetter() {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "theName");
			assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
		});
	}

	@Test
	void javaBeanPropertyWithDescription() {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "theName");
			assertItemMetadata(metadataEnv, property).isProperty()
				.hasDescription("The name of this simple properties.");
		});
	}

	@Test
	void javaBeanPropertyWithDefaultValue() {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeDeclaration ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "theName");
			assertItemMetadata(metadataEnv, property).isProperty().hasDefaultValue("boot");
		});
	}

	protected JavaBeanPropertyDescriptor createPropertyDescriptor(TypeDeclaration ownerElement, String name) {
		return createPropertyDescriptor(ownerElement, name, createAccessorMethodName("get", name),
				createAccessorMethodName("set", name));
	}

	protected JavaBeanPropertyDescriptor createPropertyDescriptor(TypeDeclaration ownerElement, String name,
			String getterName, String setterName) {
		MethodDeclaration getter = getMethod(ownerElement, getterName);
		MethodDeclaration setter = getMethod(ownerElement, setterName);
		VariableDeclaration field = getField(ownerElement, name);
		return new JavaBeanPropertyDescriptor(name, getter.getReturnType(), ownerElement, getter, setter, field, null);
	}

}
