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

package org.springframework.boot.test.autoconfigure.xml;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.autoconfigure.xml.XmlTestersAutoConfiguration.XmlMarshalTestersBeanPostProcessor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link XmlTestersAutoConfiguration}.
 *
 * @author Tiziano Basile
 */
class XmlTestersAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(XmlTestersAutoConfiguration.class));

	private final XmlMarshalTestersBeanPostProcessor postProcessor = new XmlMarshalTestersBeanPostProcessor();

	@Test
	void contextWhenTestersAreEnabledThenBeanPostProcessorIsRegistered() {
		this.contextRunner.withPropertyValues("spring.test.xmltesters.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(XmlMarshalTestersBeanPostProcessor.class));
	}

	@Test
	void contextWhenTestersAreNotEnabledThenBeanPostProcessorIsNotRegistered() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(XmlMarshalTestersBeanPostProcessor.class));
	}

	@Test
	void contextWhenTestersAreDisabledThenBeanPostProcessorIsNotRegistered() {
		this.contextRunner.withPropertyValues("spring.test.xmltesters.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(XmlMarshalTestersBeanPostProcessor.class));
	}

	@Test
	void postProcessWhenFieldHasGenericTypeThenTesterIsInitialized() {
		TypedTesterBean bean = new TypedTesterBean();
		this.postProcessor.postProcessAfterInitialization(bean, "typedTesterBean");
		assertThat(bean.xml.isInitialized()).isTrue();
		assertThat(bean.xml.getTypeUnderTest().resolve()).isEqualTo(String.class);
	}

	@Test
	void postProcessWhenFieldIsNullThenTesterIsNotInitialized() {
		NoTesterBean bean = new NoTesterBean();
		this.postProcessor.postProcessAfterInitialization(bean, "noTesterBean");
		assertThat(bean.xml).isNull();
	}

	@Test
	void postProcessWhenFieldIsRawThenFailsNamingTheField() {
		RawTesterBean bean = new RawTesterBean();
		assertThatIllegalStateException()
			.isThrownBy(() -> this.postProcessor.postProcessAfterInitialization(bean, "rawTesterBean"))
			.withMessageContaining("Unable to determine the type under test for field 'xml'")
			.withMessageContaining(RawTesterBean.class.getName())
			.withMessageContaining("ExampleXmlMarshalTester<MyType> xml;");
	}

	@Test
	void postProcessWhenFieldIsWildcardThenFailsNamingTheField() {
		WildcardTesterBean bean = new WildcardTesterBean();
		assertThatIllegalStateException()
			.isThrownBy(() -> this.postProcessor.postProcessAfterInitialization(bean, "wildcardTesterBean"))
			.withMessageContaining("Unable to determine the type under test for field 'xml'")
			.withMessageContaining(WildcardTesterBean.class.getName());
	}

	static class TypedTesterBean {

		private final ExampleXmlMarshalTester<String> xml = new ExampleXmlMarshalTester<>();

	}

	static class NoTesterBean {

		private @Nullable ExampleXmlMarshalTester<String> xml;

	}

	@SuppressWarnings("rawtypes")
	static class RawTesterBean {

		private final ExampleXmlMarshalTester xml = new ExampleXmlMarshalTester<>();

	}

	static class WildcardTesterBean {

		private final ExampleXmlMarshalTester<?> xml = new ExampleXmlMarshalTester<String>();

	}

}
