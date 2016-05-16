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

package org.springframework.boot.test.autoconfigure.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.context.TestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JsonTesterInitializationTestExecutionListener}.
 *
 * @author Phillip Webb
 */
public class JsonTesterInitializationTestExecutionListenerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private JsonTesterInitializationTestExecutionListener listener = new JsonTesterInitializationTestExecutionListener();

	@Test
	public void prepareTestContextShouldInitializeBasicJsonTester() throws Exception {
		WithBasicJsonTester instance = new WithBasicJsonTester();
		this.listener.prepareTestInstance(mockTestContext(instance));
		assertThat(instance.tester).isNotNull();
	}

	@Test
	public void prepareTestContextShouldInitializeJacksonTester() throws Exception {
		WithJacksonTester instance = new WithJacksonTester();
		this.listener.prepareTestInstance(mockTestContext(instance, new ObjectMapper()));
		assertThat(instance.tester).isNotNull();
	}

	@Test
	public void prepareTestContextShouldInitializeGsonTester() throws Exception {
		WithGsonTester instance = new WithGsonTester();
		this.listener.prepareTestInstance(
				mockTestContext(instance, new GsonBuilder().create()));
		assertThat(instance.tester).isNotNull();
	}

	@Test
	public void prepareTestContextWhenInitFieldsFalseShouldNotInitializeTesters()
			throws Exception {
		WithInitFieldsFalse instance = new WithInitFieldsFalse();
		this.listener.prepareTestInstance(mockTestContext(instance, new ObjectMapper()));
		assertThat(instance.basicTester).isNull();
		assertThat(instance.jacksonTester).isNull();
		assertThat(instance.gsonTester).isNull();
	}

	@Test
	public void prepareTestContextWhenInitFieldsTrueShouldInitializeTesters()
			throws Exception {
		WithInitFieldsTrue instance = new WithInitFieldsTrue();
		this.listener.prepareTestInstance(mockTestContext(instance));
		assertThat(instance.tester).isNotNull();
	}

	@Test
	public void prepareTestContextWhenMissingAnnotationShouldNotInitializeTesters()
			throws Exception {
		WithoutAnnotation instance = new WithoutAnnotation();
		this.listener.prepareTestInstance(mockTestContext(instance));
		assertThat(instance.basicTester).isNull();
		assertThat(instance.jacksonTester).isNull();
		assertThat(instance.gsonTester).isNull();
	}

	@Test
	public void prepareTestContextWhenHasJacksonTesterButNoObjectMapperBeanShouldThrowException()
			throws Exception {
		WithJacksonTester instance = new WithJacksonTester();
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("ObjectMapper");
		this.listener.prepareTestInstance(mockTestContext(instance));
	}

	@Test
	public void prepareTestContextWhenHasJacksonTesterButNoGsonBeanShouldThrowException()
			throws Exception {
		WithGsonTester instance = new WithGsonTester();
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Gson");
		this.listener.prepareTestInstance(mockTestContext(instance));
	}

	private TestContext mockTestContext(Object testInstance) {
		return mockTestContext(testInstance, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TestContext mockTestContext(Object testInstance, Object bean) {
		TestContext testContext = mock(TestContext.class);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		if (bean != null) {
			applicationContext.getBeanFactory().registerSingleton("bean", bean);
		}
		given(testContext.getApplicationContext()).willReturn(applicationContext);
		given(testContext.getTestClass()).willReturn((Class) testInstance.getClass());
		given(testContext.getTestInstance()).willReturn(testInstance);
		return testContext;
	}

	@AutoConfigureJsonTesters
	static class WithBasicJsonTester {

		private BasicJsonTester tester;

	}

	@AutoConfigureJsonTesters
	static class WithJacksonTester {

		private JacksonTester<Object> tester;
	}

	@AutoConfigureJsonTesters
	static class WithGsonTester {

		private GsonTester<Object> tester;
	}

	@AutoConfigureJsonTesters(initFields = false)
	static class WithInitFieldsFalse {

		private BasicJsonTester basicTester;

		private JacksonTester<Object> jacksonTester;

		private GsonTester<Object> gsonTester;

	}

	@AutoConfigureJsonTesters(initFields = true)
	static class WithInitFieldsTrue {

		private BasicJsonTester tester;

	}

	static class WithoutAnnotation {

		private BasicJsonTester basicTester;

		private JacksonTester<Object> jacksonTester;

		private GsonTester<Object> gsonTester;

	}

}
