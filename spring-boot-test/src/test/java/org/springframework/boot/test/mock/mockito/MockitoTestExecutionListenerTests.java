/*
 * Copyright 2012-2018 the original author or authors.
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

import java.io.InputStream;
import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link MockitoTestExecutionListener}.
 *
 * @author Phillip Webb
 */
public class MockitoTestExecutionListenerTests {

	private MockitoTestExecutionListener listener = new MockitoTestExecutionListener();

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	private MockitoPostProcessor postProcessor;

	@Captor
	private ArgumentCaptor<Field> fieldCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.applicationContext.getBean(MockitoPostProcessor.class))
				.willReturn(this.postProcessor);
	}

	@Test
	public void prepareTestInstanceShouldInitMockitoAnnotations() throws Exception {
		WithMockitoAnnotations instance = new WithMockitoAnnotations();
		this.listener.prepareTestInstance(mockTestContext(instance));
		assertThat(instance.mock).isNotNull();
		assertThat(instance.captor).isNotNull();
	}

	@Test
	public void prepareTestInstanceShouldInjectMockBean() throws Exception {
		WithMockBean instance = new WithMockBean();
		this.listener.prepareTestInstance(mockTestContext(instance));
		verify(this.postProcessor).inject(this.fieldCaptor.capture(), eq(instance),
				(MockDefinition) any());
		assertThat(this.fieldCaptor.getValue().getName()).isEqualTo("mockBean");
	}

	@Test
	public void beforeTestMethodShouldDoNothingWhenDirtiesContextAttributeIsNotSet()
			throws Exception {
		WithMockBean instance = new WithMockBean();
		this.listener.beforeTestMethod(mockTestContext(instance));
		verifyNoMoreInteractions(this.postProcessor);
	}

	@Test
	public void beforeTestMethodShouldInjectMockBeanWhenDirtiesContextAttributeIsSet()
			throws Exception {
		WithMockBean instance = new WithMockBean();
		TestContext mockTestContext = mockTestContext(instance);
		given(mockTestContext.getAttribute(
				DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))
						.willReturn(Boolean.TRUE);
		this.listener.beforeTestMethod(mockTestContext);
		verify(this.postProcessor).inject(this.fieldCaptor.capture(), eq(instance),
				(MockDefinition) any());
		assertThat(this.fieldCaptor.getValue().getName()).isEqualTo("mockBean");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TestContext mockTestContext(Object instance) {
		TestContext testContext = mock(TestContext.class);
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestClass()).willReturn((Class) instance.getClass());
		given(testContext.getApplicationContext()).willReturn(this.applicationContext);
		return testContext;
	}

	static class WithMockitoAnnotations {

		@Mock
		InputStream mock;

		@Captor
		ArgumentCaptor<InputStream> captor;

	}

	static class WithMockBean {

		@MockBean
		InputStream mockBean;

	}

}
