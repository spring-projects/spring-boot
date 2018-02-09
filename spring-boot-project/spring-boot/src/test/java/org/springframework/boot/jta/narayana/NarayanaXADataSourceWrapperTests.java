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

package org.springframework.boot.jta.narayana;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link NarayanaXADataSourceWrapper}.
 *
 * @author Gytis Trikleris
 */
@RunWith(MockitoJUnitRunner.class)
public class NarayanaXADataSourceWrapperTests {

	@Mock
	private XADataSource dataSource;

	@Mock
	private DataSource wrappedDataSource;

	@Mock
	private DbcpXaDataSourceWrapper delegateWrapper;

	@Mock
	private NarayanaRecoveryManagerBean recoveryManager;

	@Mock
	private NarayanaProperties properties;

	private NarayanaXADataSourceWrapper wrapper;

	@Before
	public void before() {
		this.wrapper = new NarayanaXADataSourceWrapper(this.delegateWrapper, this.recoveryManager, this.properties);
		given(this.delegateWrapper.wrapDataSource(this.dataSource)).willReturn(this.wrappedDataSource);
	}

	@Test
	public void wrap() {
		DataSource wrapped = this.wrapper.wrapDataSource(this.dataSource);
		assertThat(wrapped).isEqualTo(this.wrappedDataSource);
		verify(this.recoveryManager).registerXAResourceRecoveryHelper(
				any(DataSourceXAResourceRecoveryHelper.class));
		verify(this.properties).getRecoveryDbUser();
		verify(this.properties).getRecoveryDbPass();
		verify(this.delegateWrapper).wrapDataSource(this.dataSource);
	}

	@Test
	public void wrapWithCredentials() {
		given(this.properties.getRecoveryDbUser()).willReturn("userName");
		given(this.properties.getRecoveryDbPass()).willReturn("password");
		DataSource wrapped = this.wrapper.wrapDataSource(this.dataSource);
		assertThat(wrapped).isEqualTo(this.wrappedDataSource);
		verify(this.recoveryManager, times(1)).registerXAResourceRecoveryHelper(
				any(DataSourceXAResourceRecoveryHelper.class));
		verify(this.properties, times(2)).getRecoveryDbUser();
		verify(this.properties, times(1)).getRecoveryDbPass();
		verify(this.delegateWrapper).wrapDataSource(this.dataSource);
	}

}
