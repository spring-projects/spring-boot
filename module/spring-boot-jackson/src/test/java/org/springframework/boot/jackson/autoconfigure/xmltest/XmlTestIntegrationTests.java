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

package org.springframework.boot.jackson.autoconfigure.xmltest;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.SerializationFeature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.XmlMapperBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.xmltest.app.ExampleBasicObject;
import org.springframework.boot.jackson.autoconfigure.xmltest.app.ExampleCustomObject;
import org.springframework.boot.jackson.autoconfigure.xmltest.app.ExampleXmlApplication;
import org.springframework.boot.test.autoconfigure.xml.XmlTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.xml.JacksonXmlTester;
import org.springframework.boot.test.xml.XmlContent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link XmlTest @XmlTest}.
 *
 * @author Tiziano Basile
 */
@XmlTest
@ContextConfiguration(classes = ExampleXmlApplication.class)
@Import(XmlTestIntegrationTests.XmlMapperCustomizerConfiguration.class)
class XmlTestIntegrationTests {

	@Autowired
	private JacksonXmlTester<ExampleBasicObject> basicXml;

	@Autowired
	private JacksonXmlTester<ExampleCustomObject> customXml;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextWhenXmlTestThenJsonTestersAreNotRegistered() {
		assertThat(this.applicationContext.getBeanNamesForType(JacksonTester.class)).isEmpty();
		assertThat(this.applicationContext.getBeanNamesForType(BasicJsonTester.class)).isEmpty();
	}

	@Test
	void testerWhenContextStartsThenIsInitialized() {
		assertThat(this.basicXml).isNotNull();
		assertThat(this.customXml).isNotNull();
	}

	@Test
	void writeWhenBasicObjectThenMatchesExpectedXml() throws Exception {
		ExampleBasicObject object = new ExampleBasicObject();
		object.setValue("spring");
		assertThat(this.basicXml.write(object)).isSimilarToXml("example.xml");
	}

	@Test
	void readWhenBasicObjectResourceThenReturnsObject() throws Exception {
		ExampleBasicObject expected = new ExampleBasicObject();
		expected.setValue("spring");
		assertThat(this.basicXml.read("example.xml")).isEqualTo(expected);
	}

	@Test
	void writeWhenJacksonComponentIsScannedThenUsesCustomSerializer() throws Exception {
		XmlContent<ExampleCustomObject> content = this.customXml.write(new ExampleCustomObject("spring"));
		assertThat(content).extractingXPathStringValue("/example/custom").isEqualTo("spring");
	}

	@Test
	void writeWhenCustomizerIsDefinedThenCustomizerIsApplied() throws Exception {
		ExampleBasicObject object = new ExampleBasicObject();
		object.setValue("spring");
		assertThat(this.basicXml.write(object).getXml()).contains("\n");
	}

	@Configuration(proxyBeanMethods = false)
	static class XmlMapperCustomizerConfiguration {

		@Bean
		XmlMapperBuilderCustomizer indentOutputXmlMapperBuilderCustomizer() {
			return (builder) -> builder.configure(SerializationFeature.INDENT_OUTPUT, true);
		}

	}

}
