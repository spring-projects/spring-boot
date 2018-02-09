/*
 * Copyright 2012-2018 the original author or authors.
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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.dbcp2.managed.ManagedDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;

/**
 * @author Gytis Trikleris
 */
@RunWith(MockitoJUnitRunner.class)
public class DbcpXaDataSourceWrapperTest {

	@Mock
	private TransactionManager mockTransactionManager;

	@Mock
	private NarayanaProperties.PoolProperties mockNarayanaPoolProperties;

	@Mock
	private XADataSource mockXaDataSource;

	@Mock
	private XAConnection mockXaConnection;

	@Mock
	private Connection mockConnection;

	@Mock
	private XAResource mockXaResource;

	private DbcpXaDataSourceWrapper wrapper;

	@Before
	public void before() throws SQLException {
		when(this.mockXaDataSource.getXAConnection()).thenReturn(this.mockXaConnection);
		when(this.mockXaConnection.getConnection()).thenReturn(this.mockConnection);
		when(this.mockXaConnection.getXAResource()).thenReturn(this.mockXaResource);
		this.wrapper = new DbcpXaDataSourceWrapper(this.mockTransactionManager, this.mockNarayanaPoolProperties);
	}

	@Test
	public void shouldWrapToCorrectDataSource() {
		DataSource dataSource = this.wrapper.wrapDataSource(this.mockXaDataSource);
		assertThat(dataSource).isInstanceOf(ManagedDataSource.class);
	}

	@Test
	public void shouldDelegateConnectionCreation() throws SQLException {
		DataSource dataSource = this.wrapper.wrapDataSource(this.mockXaDataSource);
		dataSource.getConnection();
		verify(this.mockXaDataSource).getXAConnection();
	}

}
