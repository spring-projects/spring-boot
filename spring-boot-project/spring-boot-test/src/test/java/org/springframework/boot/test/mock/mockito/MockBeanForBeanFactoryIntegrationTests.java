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

package org.springframework.boot.test.mock.mockito;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test {@link MockBean} for a factory bean.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
public class MockBeanForBeanFactoryIntegrationTests {

	// gh-7439

	@MockBean
	private TestFactoryBean testFactoryBean;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testName() {
		TestBean testBean = mock(TestBean.class);
		given(testBean.hello()).willReturn("amock");
		given(this.testFactoryBean.getObjectType()).willReturn((Class) TestBean.class);
		given(this.testFactoryBean.getObject()).willReturn(testBean);
		TestBean bean = this.applicationContext.getBean(TestBean.class);
		assertThat(bean.hello()).isEqualTo("amock");
	}

	@Configuration
	static class Config {

		@Bean
		public TestFactoryBean testFactoryBean() {
			return new TestFactoryBean();
		}

	}

	static class TestFactoryBean implements FactoryBean<TestBean> {

		@Override
		public TestBean getObject() {
			return () -> "normal";
		}

		@Override
		public Class<?> getObjectType() {
			return TestBean.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

	}

	interface TestBean {

		String hello();

	}

}
