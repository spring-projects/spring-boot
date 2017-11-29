/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.jta.atomikos;

import com.atomikos.jdbc.AtomikosSQLException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AtomikosDataSourceBean}.
 *
 * @author Phillip Webb
 */
public class AtomikosDataSourceBeanTests {

	@Test
	public void beanMethods() throws Exception {
		MockAtomikosDataSourceBean bean = spy(new MockAtomikosDataSourceBean());
		bean.setBeanName("bean");
		bean.afterPropertiesSet();
		assertThat(bean.getUniqueResourceName()).isEqualTo("bean");
		verify(bean).init();
		verify(bean, never()).close();
		bean.destroy();
		verify(bean).close();
	}

	@SuppressWarnings("serial")
	private static class MockAtomikosDataSourceBean extends AtomikosDataSourceBean {

		@Override
		public synchronized void init() throws AtomikosSQLException {
		}

		@Override
		public void close() {
		}

	}

}
