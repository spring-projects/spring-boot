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

package org.springframework.boot.test.xml;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.boot.test.json.ExampleObject;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JacksonXmlTester}.
 *
 * @author Tiziano Basile
 */
class JacksonXmlTesterTests {

	private static final String EXAMPLE_XML = "<ExampleObject><name>Spring</name><age>100</age></ExampleObject>";

	@SuppressWarnings("NullAway.Init")
	private JacksonXmlTester<ExampleObject> xml;

	@BeforeEach
	void setup() {
		this.xml = new JacksonXmlTester<>(JacksonXmlTesterTests.class, ResolvableType.forClass(ExampleObject.class),
				new XmlMapper());
	}

	@Test
	void writeWhenObjectIsGivenShouldReturnSimilarXml() throws Exception {
		assertThat(this.xml.write(createExampleObject())).isSimilarToXml(EXAMPLE_XML);
	}

	@Test
	void writeWhenObjectIsGivenShouldMatchResource() throws Exception {
		assertThat(this.xml.write(createExampleObject())).isSimilarToXml("example-object.xml");
	}

	@Test
	void writeWhenObjectIsGivenShouldHaveXPathValues() throws Exception {
		XmlContent<ExampleObject> content = this.xml.write(createExampleObject());
		assertThat(content).extractingXPathStringValue("/ExampleObject/name").isEqualTo("Spring");
		assertThat(content).extractingXPathNumberValue("/ExampleObject/age").isEqualTo(100);
	}

	@Test
	void writeWhenObjectIsGivenShouldReturnContentWithXml() throws Exception {
		XmlContent<ExampleObject> content = this.xml.write(createExampleObject());
		assertThat(content.getXml()).contains("<name>Spring</name>");
		assertThat(content.toString()).startsWith("XmlContent ");
	}

	@Test
	void readWhenResourceIsGivenShouldReturnObject() throws Exception {
		assertThat(this.xml.read("example-object.xml")).isEqualTo(createExampleObject());
	}

	@Test
	void readObjectWhenResourceIsGivenShouldReturnObject() throws Exception {
		assertThat(this.xml.readObject("example-object.xml")).isEqualTo(createExampleObject());
	}

	@Test
	void parseWhenStringIsGivenShouldReturnObject() throws Exception {
		assertThat(this.xml.parse(EXAMPLE_XML)).isEqualTo(createExampleObject());
	}

	@Test
	void parseObjectWhenBytesAreGivenShouldReturnObject() throws Exception {
		assertThat(this.xml.parseObject(EXAMPLE_XML.getBytes())).isEqualTo(createExampleObject());
	}

	@Test
	void writeWhenTesterIsUninitializedShouldThrowException() {
		JacksonXmlTester<ExampleObject> uninitialized = new UninitializedTester(new XmlMapper());
		assertThatIllegalStateException().isThrownBy(() -> uninitialized.write(createExampleObject()))
			.withMessageContaining("Uninitialized XmlMarshalTester");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void initFieldsWhenTestIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> JacksonXmlTester.initFields(null, new XmlMapper()))
			.withMessageContaining("'testInstance' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void initFieldsWhenMarshallerIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> JacksonXmlTester.initFields(new InitFieldsTestClass(), (XmlMapper) null))
			.withMessageContaining("'marshaller' must not be null");
	}

	@Test
	void initFieldsShouldSetNullFields() {
		InitFieldsTestClass test = new InitFieldsTestClass();
		assertThat(test.test).isNull();
		assertThat(test.base).isNull();
		JacksonXmlTester.initFields(test, new XmlMapper());
		assertThat(test.test).isNotNull();
		assertThat(test.base).isNotNull();
		ResolvableType type = test.test.getType();
		assertThat(type).isNotNull();
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.resolveGeneric()).isEqualTo(ExampleObject.class);
	}

	private ExampleObject createExampleObject() {
		ExampleObject exampleObject = new ExampleObject();
		exampleObject.setName("Spring");
		exampleObject.setAge(100);
		return exampleObject;
	}

	static class UninitializedTester extends JacksonXmlTester<ExampleObject> {

		UninitializedTester(XmlMapper xmlMapper) {
			super(xmlMapper);
		}

	}

	abstract static class InitFieldsBaseClass {

		public @Nullable JacksonXmlTester<ExampleObject> base;

		public JacksonXmlTester<ExampleObject> baseSet = new JacksonXmlTester<>(InitFieldsBaseClass.class,
				ResolvableType.forClass(ExampleObject.class), new XmlMapper());

	}

	static class InitFieldsTestClass extends InitFieldsBaseClass {

		public @Nullable JacksonXmlTester<List<ExampleObject>> test;

		public JacksonXmlTester<ExampleObject> testSet = new JacksonXmlTester<>(InitFieldsBaseClass.class,
				ResolvableType.forClass(ExampleObject.class), new XmlMapper());

	}

}
