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

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;

import org.springframework.boot.test.mock.mockito.example.ExampleExtraInterface;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller;
import org.springframework.boot.test.mock.mockito.example.RealExampleService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefinitionsParser}.
 *
 * @author Phillip Webb
 */
public class DefinitionsParserTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private DefinitionsParser parser = new DefinitionsParser();

	@Test
	public void parseSingleMockBean() {
		this.parser.parse(SingleMockBean.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getMockDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
	}

	@Test
	public void parseRepeatMockBean() {
		this.parser.parse(RepeatMockBean.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getMockDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(getMockDefinition(1).getClassToMock())
				.isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	public void parseMockBeanAttributes() throws Exception {
		this.parser.parse(MockBeanAttributes.class);
		assertThat(getDefinitions()).hasSize(1);
		MockDefinition definition = getMockDefinition(0);
		assertThat(definition.getName()).isEqualTo("Name");
		assertThat(definition.getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(definition.getExtraInterfaces())
				.containsExactly(ExampleExtraInterface.class);
		assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
		assertThat(definition.isSerializable()).isEqualTo(true);
		assertThat(definition.getReset()).isEqualTo(MockReset.NONE);
	}

	@Test
	public void parseMockBeanOnClassAndField() throws Exception {
		this.parser.parse(MockBeanOnClassAndField.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getMockDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(getMockDefinition(1).getClassToMock())
				.isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	public void parseMockBeanInferClassToMock() throws Exception {
		this.parser.parse(MockBeanInferClassToMock.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getMockDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
	}

	@Test
	public void parseMockBeanMissingClassToMock() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unable to deduce class to mock");
		this.parser.parse(MockBeanMissingClassToMock.class);
	}

	@Test
	public void parseMockBeanMultipleClasses() throws Exception {
		this.parser.parse(MockBeanMultipleClasses.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getMockDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(getMockDefinition(1).getClassToMock())
				.isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	public void parseMockBeanMultipleClassesWithName() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(
				"The name attribute can only be used when mocking a single class");
		this.parser.parse(MockBeanMultipleClassesWithName.class);
	}

	@Test
	public void parseSingleSpyBean() {
		this.parser.parse(SingleSpyBean.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getSpyDefinition(0).getClassToSpy())
				.isEqualTo(RealExampleService.class);
	}

	@Test
	public void parseRepeatSpyBean() {
		this.parser.parse(RepeatSpyBean.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getSpyDefinition(0).getClassToSpy())
				.isEqualTo(RealExampleService.class);
		assertThat(getSpyDefinition(1).getClassToSpy())
				.isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	public void parseSpyBeanAttributes() throws Exception {
		this.parser.parse(SpyBeanAttributes.class);
		assertThat(getDefinitions()).hasSize(1);
		SpyDefinition definition = getSpyDefinition(0);
		assertThat(definition.getName()).isEqualTo("Name");
		assertThat(definition.getClassToSpy()).isEqualTo(RealExampleService.class);
		assertThat(definition.getReset()).isEqualTo(MockReset.NONE);
	}

	@Test
	public void parseSpyBeanOnClassAndField() throws Exception {
		this.parser.parse(SpyBeanOnClassAndField.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getSpyDefinition(0).getClassToSpy())
				.isEqualTo(RealExampleService.class);
		assertThat(getSpyDefinition(1).getClassToSpy())
				.isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	public void parseSpyBeanInferClassToMock() throws Exception {
		this.parser.parse(SpyBeanInferClassToMock.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getSpyDefinition(0).getClassToSpy())
				.isEqualTo(RealExampleService.class);
	}

	@Test
	public void parseSpyBeanMissingClassToMock() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unable to deduce class to spy");
		this.parser.parse(SpyBeanMissingClassToMock.class);
	}

	@Test
	public void parseSpyBeanMultipleClasses() throws Exception {
		this.parser.parse(SpyBeanMultipleClasses.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getSpyDefinition(0).getClassToSpy())
				.isEqualTo(RealExampleService.class);
		assertThat(getSpyDefinition(1).getClassToSpy())
				.isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	public void parseSpyBeanMultipleClassesWithName() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(
				"The name attribute can only be used when spying a single class");
		this.parser.parse(SpyBeanMultipleClassesWithName.class);
	}

	private MockDefinition getMockDefinition(int index) {
		return (MockDefinition) getDefinitions().get(index);
	}

	private SpyDefinition getSpyDefinition(int index) {
		return (SpyDefinition) getDefinitions().get(index);
	}

	private List<Definition> getDefinitions() {
		return new ArrayList<Definition>(this.parser.getDefinitions());
	}

	@MockBean(ExampleService.class)
	static class SingleMockBean {

	}

	@MockBeans({ @MockBean(ExampleService.class), @MockBean(ExampleServiceCaller.class) })
	static class RepeatMockBean {

	}

	@MockBean(name = "Name", classes = ExampleService.class, extraInterfaces = ExampleExtraInterface.class, answer = Answers.RETURNS_SMART_NULLS, serializable = true, reset = MockReset.NONE)
	static class MockBeanAttributes {

	}

	@MockBean(ExampleService.class)
	static class MockBeanOnClassAndField {

		@MockBean(ExampleServiceCaller.class)
		private Object caller;

	}

	@MockBean({ ExampleService.class, ExampleServiceCaller.class })
	static class MockBeanMultipleClasses {

	}

	@MockBean(name = "name", classes = { ExampleService.class,
			ExampleServiceCaller.class })
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

	@SpyBeans({ @SpyBean(RealExampleService.class),
			@SpyBean(ExampleServiceCaller.class) })
	static class RepeatSpyBean {

	}

	@SpyBean(name = "Name", classes = RealExampleService.class, reset = MockReset.NONE)
	static class SpyBeanAttributes {

	}

	@SpyBean(RealExampleService.class)
	static class SpyBeanOnClassAndField {

		@SpyBean(ExampleServiceCaller.class)
		private Object caller;

	}

	@SpyBean({ RealExampleService.class, ExampleServiceCaller.class })
	static class SpyBeanMultipleClasses {

	}

	@SpyBean(name = "name", classes = { RealExampleService.class,
			ExampleServiceCaller.class })
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
