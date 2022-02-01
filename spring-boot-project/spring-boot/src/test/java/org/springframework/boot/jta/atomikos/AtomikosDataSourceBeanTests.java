/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.jta.atomikos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link AtomikosDataSourceBean}.
 *
 * @author Phillip Webb
 */
class AtomikosDataSourceBeanTests {

	@Test
	void beanMethods() throws Exception {
		MockAtomikosDataSourceBean bean = spy(new MockAtomikosDataSourceBean());
		bean.setBeanName("bean");
		bean.afterPropertiesSet();
		assertThat(bean.getUniqueResourceName()).isEqualTo("bean");
		then(bean).should().init();
		then(bean).should(never()).close();
		bean.destroy();
		then(bean).should().close();
	}

	static class MockAtomikosDataSourceBean extends AtomikosDataSourceBean {

		@Override
		public synchronized void init() {
		}

		@Override
		public void close() {
		}

	}

}
