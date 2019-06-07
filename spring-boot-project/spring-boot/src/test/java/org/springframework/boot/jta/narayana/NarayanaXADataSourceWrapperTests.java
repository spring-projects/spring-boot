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

package org.springframework.boot.jta.narayana;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link NarayanaXADataSourceWrapper}.
 *
 * @author Gytis Trikleris
 */
public class NarayanaXADataSourceWrapperTests {

	private XADataSource dataSource = mock(XADataSource.class);

	private NarayanaRecoveryManagerBean recoveryManager = mock(NarayanaRecoveryManagerBean.class);

	private NarayanaProperties properties = mock(NarayanaProperties.class);

	private NarayanaXADataSourceWrapper wrapper = new NarayanaXADataSourceWrapper(this.recoveryManager,
			this.properties);

	@Test
	public void wrap() {
		DataSource wrapped = this.wrapper.wrapDataSource(this.dataSource);
		assertThat(wrapped).isInstanceOf(NarayanaDataSourceBean.class);
		verify(this.recoveryManager, times(1))
				.registerXAResourceRecoveryHelper(any(DataSourceXAResourceRecoveryHelper.class));
		verify(this.properties, times(1)).getRecoveryDbUser();
		verify(this.properties, times(1)).getRecoveryDbPass();
	}

	@Test
	public void wrapWithCredentials() {
		given(this.properties.getRecoveryDbUser()).willReturn("userName");
		given(this.properties.getRecoveryDbPass()).willReturn("password");
		DataSource wrapped = this.wrapper.wrapDataSource(this.dataSource);
		assertThat(wrapped).isInstanceOf(NarayanaDataSourceBean.class);
		verify(this.recoveryManager, times(1))
				.registerXAResourceRecoveryHelper(any(DataSourceXAResourceRecoveryHelper.class));
		verify(this.properties, times(2)).getRecoveryDbUser();
		verify(this.properties, times(1)).getRecoveryDbPass();
	}

}
