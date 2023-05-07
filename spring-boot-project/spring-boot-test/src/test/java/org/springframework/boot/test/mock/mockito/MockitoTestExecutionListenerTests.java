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

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockitoTestExecutionListener}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class MockitoTestExecutionListenerTests {

	private final MockitoTestExecutionListener listener = new MockitoTestExecutionListener();

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	private MockitoPostProcessor postProcessor;

	@Test
	void prepareTestInstanceShouldInitMockitoAnnotations() throws Exception {
		WithMockitoAnnotations instance = new WithMockitoAnnotations();
		this.listener.prepareTestInstance(mockTestContext(instance));
		assertThat(instance.mock).isNotNull();
		assertThat(instance.captor).isNotNull();
	}

	@Test
	void prepareTestInstanceShouldInjectMockBean() throws Exception {
		given(this.applicationContext.getBean(MockitoPostProcessor.class)).willReturn(this.postProcessor);
		WithMockBean instance = new WithMockBean();
		TestContext testContext = mockTestContext(instance);
		given(testContext.getApplicationContext()).willReturn(this.applicationContext);
		this.listener.prepareTestInstance(testContext);
		then(this.postProcessor).should()
			.inject(assertArg((field) -> assertThat(field.getName()).isEqualTo("mockBean")), eq(instance),
					any(MockDefinition.class));
	}

	@Test
	void beforeTestMethodShouldDoNothingWhenDirtiesContextAttributeIsNotSet() throws Exception {
		this.listener.beforeTestMethod(mock(TestContext.class));
		then(this.postProcessor).shouldHaveNoMoreInteractions();
	}

	@Test
	void beforeTestMethodShouldInjectMockBeanWhenDirtiesContextAttributeIsSet() throws Exception {
		given(this.applicationContext.getBean(MockitoPostProcessor.class)).willReturn(this.postProcessor);
		WithMockBean instance = new WithMockBean();
		TestContext mockTestContext = mockTestContext(instance);
		given(mockTestContext.getApplicationContext()).willReturn(this.applicationContext);
		given(mockTestContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))
			.willReturn(Boolean.TRUE);
		this.listener.beforeTestMethod(mockTestContext);
		then(this.postProcessor).should()
			.inject(assertArg((field) -> assertThat(field.getName()).isEqualTo("mockBean")), eq(instance),
					any(MockDefinition.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TestContext mockTestContext(Object instance) {
		TestContext testContext = mock(TestContext.class);
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestClass()).willReturn((Class) instance.getClass());
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
