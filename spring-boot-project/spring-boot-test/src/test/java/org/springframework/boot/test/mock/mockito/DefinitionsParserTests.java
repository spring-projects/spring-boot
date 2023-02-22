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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.example.ExampleExtraInterface;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller;
import org.springframework.boot.test.mock.mockito.example.RealExampleService;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DefinitionsParser}.
 *
 * @author Phillip Webb
 */
class DefinitionsParserTests {

	private final DefinitionsParser parser = new DefinitionsParser();

	@Test
	void parseSingleMockBean() {
		this.parser.parse(SingleMockBean.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getMockDefinition(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
	}

	@Test
	void parseRepeatMockBean() {
		this.parser.parse(RepeatMockBean.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getMockDefinition(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
		assertThat(getMockDefinition(1).getTypeToMock().resolve()).isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	void parseMockBeanAttributes() {
		this.parser.parse(MockBeanAttributes.class);
		assertThat(getDefinitions()).hasSize(1);
		MockDefinition definition = getMockDefinition(0);
		assertThat(definition.getName()).isEqualTo("Name");
		assertThat(definition.getTypeToMock().resolve()).isEqualTo(ExampleService.class);
		assertThat(definition.getExtraInterfaces()).containsExactly(ExampleExtraInterface.class);
		assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
		assertThat(definition.isSerializable()).isTrue();
		assertThat(definition.getReset()).isEqualTo(MockReset.NONE);
		assertThat(definition.getQualifier()).isNull();
	}

	@Test
	void parseMockBeanOnClassAndField() {
		this.parser.parse(MockBeanOnClassAndField.class);
		assertThat(getDefinitions()).hasSize(2);
		MockDefinition classDefinition = getMockDefinition(0);
		assertThat(classDefinition.getTypeToMock().resolve()).isEqualTo(ExampleService.class);
		assertThat(classDefinition.getQualifier()).isNull();
		MockDefinition fieldDefinition = getMockDefinition(1);
		assertThat(fieldDefinition.getTypeToMock().resolve()).isEqualTo(ExampleServiceCaller.class);
		QualifierDefinition qualifier = QualifierDefinition
			.forElement(ReflectionUtils.findField(MockBeanOnClassAndField.class, "caller"));
		assertThat(fieldDefinition.getQualifier()).isNotNull().isEqualTo(qualifier);
	}

	@Test
	void parseMockBeanInferClassToMock() {
		this.parser.parse(MockBeanInferClassToMock.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getMockDefinition(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
	}

	@Test
	void parseMockBeanMissingClassToMock() {
		assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(MockBeanMissingClassToMock.class))
			.withMessageContaining("Unable to deduce type to mock");
	}

	@Test
	void parseMockBeanMultipleClasses() {
		this.parser.parse(MockBeanMultipleClasses.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getMockDefinition(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
		assertThat(getMockDefinition(1).getTypeToMock().resolve()).isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	void parseMockBeanMultipleClassesWithName() {
		assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(MockBeanMultipleClassesWithName.class))
			.withMessageContaining("The name attribute can only be used when mocking a single class");
	}

	@Test
	void parseSingleSpyBean() {
		this.parser.parse(SingleSpyBean.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getSpyDefinition(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
	}

	@Test
	void parseRepeatSpyBean() {
		this.parser.parse(RepeatSpyBean.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getSpyDefinition(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
		assertThat(getSpyDefinition(1).getTypeToSpy().resolve()).isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	void parseSpyBeanAttributes() {
		this.parser.parse(SpyBeanAttributes.class);
		assertThat(getDefinitions()).hasSize(1);
		SpyDefinition definition = getSpyDefinition(0);
		assertThat(definition.getName()).isEqualTo("Name");
		assertThat(definition.getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
		assertThat(definition.getReset()).isEqualTo(MockReset.NONE);
		assertThat(definition.getQualifier()).isNull();
	}

	@Test
	void parseSpyBeanOnClassAndField() {
		this.parser.parse(SpyBeanOnClassAndField.class);
		assertThat(getDefinitions()).hasSize(2);
		SpyDefinition classDefinition = getSpyDefinition(0);
		assertThat(classDefinition.getQualifier()).isNull();
		assertThat(classDefinition.getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
		SpyDefinition fieldDefinition = getSpyDefinition(1);
		QualifierDefinition qualifier = QualifierDefinition
			.forElement(ReflectionUtils.findField(SpyBeanOnClassAndField.class, "caller"));
		assertThat(fieldDefinition.getQualifier()).isNotNull().isEqualTo(qualifier);
		assertThat(fieldDefinition.getTypeToSpy().resolve()).isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	void parseSpyBeanInferClassToMock() {
		this.parser.parse(SpyBeanInferClassToMock.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getSpyDefinition(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
	}

	@Test
	void parseSpyBeanMissingClassToMock() {
		assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(SpyBeanMissingClassToMock.class))
			.withMessageContaining("Unable to deduce type to spy");
	}

	@Test
	void parseSpyBeanMultipleClasses() {
		this.parser.parse(SpyBeanMultipleClasses.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getSpyDefinition(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
		assertThat(getSpyDefinition(1).getTypeToSpy().resolve()).isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	void parseSpyBeanMultipleClassesWithName() {
		assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(SpyBeanMultipleClassesWithName.class))
			.withMessageContaining("The name attribute can only be used when spying a single class");
	}

	private MockDefinition getMockDefinition(int index) {
		return (MockDefinition) getDefinitions().get(index);
	}

	private SpyDefinition getSpyDefinition(int index) {
		return (SpyDefinition) getDefinitions().get(index);
	}

	private List<Definition> getDefinitions() {
		return new ArrayList<>(this.parser.getDefinitions());
	}

	@MockBean(ExampleService.class)
	static class SingleMockBean {

	}

	@MockBeans({ @MockBean(ExampleService.class), @MockBean(ExampleServiceCaller.class) })
	static class RepeatMockBean {

	}

	@MockBean(name = "Name", classes = ExampleService.class, extraInterfaces = ExampleExtraInterface.class,
			answer = Answers.RETURNS_SMART_NULLS, serializable = true, reset = MockReset.NONE)
	static class MockBeanAttributes {

	}

	@MockBean(ExampleService.class)
	static class MockBeanOnClassAndField {

		@MockBean(ExampleServiceCaller.class)
		@Qualifier("test")
		private Object caller;

	}

	@MockBean({ ExampleService.class, ExampleServiceCaller.class })
	static class MockBeanMultipleClasses {

	}

	@MockBean(name = "name", classes = { ExampleService.class, ExampleServiceCaller.class })
	static class MockBeanMultipleClassesWithName {

	}

	static class MockBeanInferClassToMock {

		@MockBean
		private ExampleService exampleService;

	}

	@MockBean
	static class MockBeanMissingClassToMock {

	}

	@SpyBean(RealExampleService.class)
	static class SingleSpyBean {

	}

	@SpyBeans({ @SpyBean(RealExampleService.class), @SpyBean(ExampleServiceCaller.class) })
	static class RepeatSpyBean {

	}

	@SpyBean(name = "Name", classes = RealExampleService.class, reset = MockReset.NONE)
	static class SpyBeanAttributes {

	}

	@SpyBean(RealExampleService.class)
	static class SpyBeanOnClassAndField {

		@SpyBean(ExampleServiceCaller.class)
		@Qualifier("test")
		private Object caller;

	}

	@SpyBean({ RealExampleService.class, ExampleServiceCaller.class })
	static class SpyBeanMultipleClasses {

	}

	@SpyBean(name = "name", classes = { RealExampleService.class, ExampleServiceCaller.class })
	static class SpyBeanMultipleClassesWithName {

	}

	static class SpyBeanInferClassToMock {

		@SpyBean
		private RealExampleService exampleService;

	}

	@SpyBean
	static class SpyBeanMissingClassToMock {

	}

}
