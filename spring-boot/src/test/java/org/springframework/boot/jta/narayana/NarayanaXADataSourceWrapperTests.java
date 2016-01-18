/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.jta.narayana;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;
import static org.mockito.Matchers.any;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NarayanaXADataSourceWrapperTests {

	private XADataSource dataSource = mock(XADataSource.class);

	private NarayanaRecoveryManagerBean narayanaRecoveryManagerBean = mock(NarayanaRecoveryManagerBean.class);

	private NarayanaProperties narayanaProperties = mock(NarayanaProperties.class);

	private NarayanaXADataSourceWrapper wrapper = new NarayanaXADataSourceWrapper(this.narayanaRecoveryManagerBean,
			this.narayanaProperties);

	@Test
	public void wrap() {
		DataSource wrapped = this.wrapper.wrapDataSource(this.dataSource);

		assertThat(wrapped).isInstanceOf(NarayanaDataSourceBean.class);
		verify(this.narayanaRecoveryManagerBean, times(1))
				.registerXAResourceRecoveryHelper(any(DataSourceXAResourceRecoveryHelper.class));
		verify(this.narayanaProperties, times(1)).getRecoveryDbUser();
		verify(this.narayanaProperties, times(1)).getRecoveryDbPass();
	}

	@Test
	public void wrapWithCredentials() {
		when(this.narayanaProperties.getRecoveryDbUser()).thenReturn("userName");
		when(this.narayanaProperties.getRecoveryDbPass()).thenReturn("password");
		DataSource wrapped = this.wrapper.wrapDataSource(this.dataSource);

		assertThat(wrapped).isInstanceOf(NarayanaDataSourceBean.class);
		verify(this.narayanaRecoveryManagerBean, times(1))
				.registerXAResourceRecoveryHelper(any(DataSourceXAResourceRecoveryHelper.class));
		verify(this.narayanaProperties, times(2)).getRecoveryDbUser();
		verify(this.narayanaProperties, times(1)).getRecoveryDbPass();
	}

}
