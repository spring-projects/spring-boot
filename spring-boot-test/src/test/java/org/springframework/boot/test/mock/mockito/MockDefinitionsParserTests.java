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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;

import org.springframework.boot.test.mock.mockito.example.ExampleExtraInterface;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller;
import org.springframework.boot.test.mock.mockito.example.MyMockBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockDefinitionsParser}.
 *
 * @author Phillip Webb
 */
public class MockDefinitionsParserTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private MockDefinitionsParser parser = new MockDefinitionsParser();

	@Test
	public void parseSingleMockBean() {
		this.parser.parse(SingleMockBean.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
	}

	@Test
	public void parseRepeatMockBean() {
		this.parser.parse(RepeatMockBean.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(getDefinition(1).getClassToMock())
				.isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	@Ignore // See SPR-13973
	public void parseMetaMockBean() {
		this.parser.parse(MetaMockBean.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
	}

	@Test
	public void parseMockBeanAttributes() throws Exception {
		this.parser.parse(MockBeanAttributes.class);
		assertThat(getDefinitions()).hasSize(1);
		MockDefinition definition = getDefinition(0);
		assertThat(definition.getName()).isEqualTo("Name");
		assertThat(definition.getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(definition.getExtraInterfaces())
				.containsExactly(ExampleExtraInterface.class);
		assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
		assertThat(definition.isSerializable()).isEqualTo(true);
		assertThat(definition.getReset()).isEqualTo(MockReset.NONE);
	}

	@Test
	public void parseOnClassAndField() throws Exception {
		this.parser.parse(OnClassAndField.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(getDefinition(1).getClassToMock())
				.isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	public void parseInferClassToMock() throws Exception {
		this.parser.parse(InferClassToMock.class);
		assertThat(getDefinitions()).hasSize(1);
		assertThat(getDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
	}

	@Test
	public void parseMissingClassToMock() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unable to deduce class to mock");
		this.parser.parse(MissingClassToMock.class);
	}

	@Test
	public void parseMultipleClasses() throws Exception {
		this.parser.parse(MultipleClasses.class);
		assertThat(getDefinitions()).hasSize(2);
		assertThat(getDefinition(0).getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(getDefinition(1).getClassToMock())
				.isEqualTo(ExampleServiceCaller.class);
	}

	@Test
	public void parseMultipleClassesWithName() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(
				"The name attribute can only be used when mocking a single class");
		this.parser.parse(MultipleClassesWithName.class);
	}

	private MockDefinition getDefinition(int index) {
		return getDefinitions().get(index);
	}

	private List<MockDefinition> getDefinitions() {
		return new ArrayList<MockDefinition>(this.parser.getDefinitions());
	}

	@MockBean(ExampleService.class)
	static class SingleMockBean {

	}

	@MockBeans({ @MockBean(ExampleService.class), @MockBean(ExampleServiceCaller.class) })
	static class RepeatMockBean {

	}

	@MyMockBean(ExampleService.class)
	static class MetaMockBean {

	}

	@MockBean(name = "Name", classes = ExampleService.class, extraInterfaces = ExampleExtraInterface.class, answer = Answers.RETURNS_SMART_NULLS, serializable = true, reset = MockReset.NONE)
	static class MockBeanAttributes {

	}

	@MockBean(ExampleService.class)
	static class OnClassAndField {

		@MockBean(ExampleServiceCaller.class)
		private Object caller;

	}

	@MockBean({ ExampleService.class, ExampleServiceCaller.class })
	static class MultipleClasses {

	}

	@MockBean(name = "name", classes = { ExampleService.class,
			ExampleServiceCaller.class })
	static class MultipleClassesWithName {

	}

	static class InferClassToMock {

		@MockBean
		private ExampleService exampleService;

	}

	@MockBean
	static class MissingClassToMock {

	}

}
