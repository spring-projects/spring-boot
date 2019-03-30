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

import org.junit.Test;

import org.springframework.boot.configurationsample.simple.DeprecatedProperties;
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
public class JavaBeanPropertyDescriptorTests extends PropertyDescriptorTests {

	@Test
	public void javaBeanSimpleProperty() throws IOException {
		process(SimpleTypeProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv
					.getRootElement(SimpleTypeProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"myString");
			assertThat(property.getName()).isEqualTo("myString");
			assertThat(property.getSource()).isSameAs(property.getGetter());
			assertThat(property.getGetter().getSimpleName()).hasToString("getMyString");
			assertThat(property.getSetter().getSimpleName()).hasToString("setMyString");
			assertThat(property.isProperty(metadataEnv)).isTrue();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	public void javaBeanCollectionProperty() throws IOException {
		process(SimpleCollectionProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv
					.getRootElement(SimpleCollectionProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"doubles");
			assertThat(property.getName()).isEqualTo("doubles");
			assertThat(property.getGetter().getSimpleName()).hasToString("getDoubles");
			assertThat(property.getSetter()).isNull();
			assertThat(property.isProperty(metadataEnv)).isTrue();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	public void javaBeanNestedPropertySameClass() throws IOException {
		process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv
					.getRootElement(InnerClassProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"first");
			assertThat(property.getName()).isEqualTo("first");
			assertThat(property.getGetter().getSimpleName()).hasToString("getFirst");
			assertThat(property.getSetter()).isNull();
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isTrue();
		});
	}

	@Test
	public void javaBeanNestedPropertyWithAnnotation() throws IOException {
		process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv
					.getRootElement(InnerClassProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"third");
			assertThat(property.getName()).isEqualTo("third");
			assertThat(property.getGetter().getSimpleName()).hasToString("getThird");
			assertThat(property.getSetter()).isNull();
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isTrue();
		});
	}

	@Test
	public void javaBeanSimplePropertyWithOnlyGetterShouldNotBeExposed()
			throws IOException {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			ExecutableElement getter = getMethod(ownerElement, "getSize");
			VariableElement field = getField(ownerElement, "size");
			JavaBeanPropertyDescriptor property = new JavaBeanPropertyDescriptor(
					ownerElement, getter, getter, "size", field.asType(), field, null);
			assertThat(property.getName()).isEqualTo("size");
			assertThat(property.getSource()).isSameAs(property.getGetter());
			assertThat(property.getGetter().getSimpleName()).hasToString("getSize");
			assertThat(property.getSetter()).isNull();
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	public void javaBeanSimplePropertyWithOnlySetterShouldNotBeExposed()
			throws IOException {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			VariableElement field = getField(ownerElement, "counter");
			JavaBeanPropertyDescriptor property = new JavaBeanPropertyDescriptor(
					ownerElement, null, null, "counter", field.asType(), field,
					getMethod(ownerElement, "setCounter"));
			assertThat(property.getName()).isEqualTo("counter");
			assertThat(property.getSource()).isSameAs(property.getGetter());
			assertThat(property.getGetter()).isNull();
			assertThat(property.getSetter().getSimpleName()).hasToString("setCounter");
			assertThat(property.isProperty(metadataEnv)).isFalse();
			assertThat(property.isNested(metadataEnv)).isFalse();
		});
	}

	@Test
	public void javaBeanMetadataSimpleProperty() throws IOException {
		process(SimpleTypeProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv
					.getRootElement(SimpleTypeProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"myString");
			assertItemMetadata(metadataEnv, property).isProperty()
					.hasName("test.my-string").hasType(String.class)
					.hasSourceType(SimpleTypeProperties.class).hasNoDescription()
					.isNotDeprecated();
		});
	}

	@Test
	public void javaBeanMetadataCollectionProperty() throws IOException {
		process(SimpleCollectionProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv
					.getRootElement(SimpleCollectionProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"doubles");
			assertItemMetadata(metadataEnv, property).isProperty().hasName("test.doubles")
					.hasType("java.util.List<java.lang.Double>")
					.hasSourceType(SimpleCollectionProperties.class).hasNoDescription()
					.isNotDeprecated();
		});
	}

	@Test
	public void javaBeanMetadataNestedGroup() throws IOException {
		process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv
					.getRootElement(InnerClassProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"first");
			assertItemMetadata(metadataEnv, property).isGroup().hasName("test.first")
					.hasType(
							"org.springframework.boot.configurationsample.specific.InnerClassProperties$Foo")
					.hasSourceType(InnerClassProperties.class)
					.hasSourceMethod("getFirst()").hasNoDescription().isNotDeprecated();
		});
	}

	@Test
	public void javaBeanMetadataNotACandidatePropertyShouldReturnNull()
			throws IOException {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			VariableElement field = getField(ownerElement, "counter");
			JavaBeanPropertyDescriptor property = new JavaBeanPropertyDescriptor(
					ownerElement, null, null, "counter", field.asType(), field,
					getMethod(ownerElement, "setCounter"));
			assertThat(property.resolveItemMetadata("test", metadataEnv)).isNull();
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	public void javaBeanDeprecatedPropertyOnClass() throws IOException {
		process(DeprecatedProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv
					.getRootElement(DeprecatedProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"name");
			assertItemMetadata(metadataEnv, property).isProperty()
					.isDeprecatedWithNoInformation();
		});
	}

	@Test
	public void javaBeanMetadataDeprecatedPropertyWithAnnotation() throws IOException {
		process(DeprecatedSingleProperty.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv
					.getRootElement(DeprecatedSingleProperty.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"name");
			assertItemMetadata(metadataEnv, property).isProperty()
					.isDeprecatedWithReason("renamed")
					.isDeprecatedWithReplacement("singledeprecated.new-name");
		});
	}

	@Test
	public void javaBeanDeprecatedPropertyOnGetter() throws IOException {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"flag", "isFlag", "setFlag");
			assertItemMetadata(metadataEnv, property).isProperty()
					.isDeprecatedWithNoInformation();
		});
	}

	@Test
	public void javaBeanDeprecatedPropertyOnSetter() throws IOException {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"theName");
			assertItemMetadata(metadataEnv, property).isProperty()
					.isDeprecatedWithNoInformation();
		});
	}

	@Test
	public void javaBeanPropertyWithDescription() throws IOException {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"theName");
			assertItemMetadata(metadataEnv, property).isProperty()
					.hasDescription("The name of this simple properties.");
		});
	}

	@Test
	public void javaBeanPropertyWithDefaultValue() throws IOException {
		process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
			TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
			JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement,
					"theName");
			assertItemMetadata(metadataEnv, property).isProperty()
					.hasDefaultValue("boot");
		});
	}

	protected JavaBeanPropertyDescriptor createPropertyDescriptor(
			TypeElement ownerElement, String name) {
		return createPropertyDescriptor(ownerElement, name,
				createAccessorMethodName("get", name),
				createAccessorMethodName("set", name));
	}

	protected JavaBeanPropertyDescriptor createPropertyDescriptor(
			TypeElement ownerElement, String name, String getterName, String setterName) {
		ExecutableElement getter = getMethod(ownerElement, getterName);
		ExecutableElement setter = getMethod(ownerElement, setterName);
		VariableElement field = getField(ownerElement, name);
		return new JavaBeanPropertyDescriptor(ownerElement, null, getter, name,
				getter.getReturnType(), field, setter);
	}

}
