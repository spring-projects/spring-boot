/*
 * Copyright 2012-2016 the original author or authors.
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

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DataSourceXAResourceRecoveryHelper}.
 *
 * @author Gytis Trikleris
 */
public class DataSourceXAResourceRecoveryHelperTests {

	private XADataSource xaDataSource;

	private XAConnection xaConnection;

	private XAResource xaResource;

	private DataSourceXAResourceRecoveryHelper recoveryHelper;

	@Before
	public void before() throws SQLException {
		this.xaDataSource = mock(XADataSource.class);
		this.xaConnection = mock(XAConnection.class);
		this.xaResource = mock(XAResource.class);
		this.recoveryHelper = new DataSourceXAResourceRecoveryHelper(this.xaDataSource);

		given(this.xaDataSource.getXAConnection()).willReturn(this.xaConnection);
		given(this.xaConnection.getXAResource()).willReturn(this.xaResource);
	}

	@Test
	public void shouldCreateConnectionAndGetXAResource() throws SQLException {
		XAResource[] xaResources = this.recoveryHelper.getXAResources();
		assertThat(xaResources.length).isEqualTo(1);
		assertThat(xaResources[0]).isSameAs(this.recoveryHelper);
		verify(this.xaDataSource, times(1)).getXAConnection();
		verify(this.xaConnection, times(1)).getXAResource();
	}

	@Test
	public void shouldCreateConnectionWithCredentialsAndGetXAResource()
			throws SQLException {
		given(this.xaDataSource.getXAConnection(anyString(), anyString()))
				.willReturn(this.xaConnection);
		this.recoveryHelper = new DataSourceXAResourceRecoveryHelper(this.xaDataSource,
				"username", "password");
		XAResource[] xaResources = this.recoveryHelper.getXAResources();
		assertThat(xaResources.length).isEqualTo(1);
		assertThat(xaResources[0]).isSameAs(this.recoveryHelper);
		verify(this.xaDataSource, times(1)).getXAConnection("username", "password");
		verify(this.xaConnection, times(1)).getXAResource();
	}

	@Test
	public void shouldFailToCreateConnectionAndNotGetXAResource() throws SQLException {
		given(this.xaDataSource.getXAConnection())
				.willThrow(new SQLException("Test exception"));
		XAResource[] xaResources = this.recoveryHelper.getXAResources();
		assertThat(xaResources.length).isEqualTo(0);
		verify(this.xaDataSource, times(1)).getXAConnection();
		verify(this.xaConnection, times(0)).getXAResource();
	}

	@Test
	public void shouldDelegateRecoverCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.recover(XAResource.TMSTARTRSCAN);
		verify(this.xaResource, times(1)).recover(XAResource.TMSTARTRSCAN);
	}

	@Test
	public void shouldDelegateRecoverCallAndCloseConnection()
			throws XAException, SQLException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.recover(XAResource.TMENDRSCAN);
		verify(this.xaResource, times(1)).recover(XAResource.TMENDRSCAN);
		verify(this.xaConnection, times(1)).close();
	}

	@Test
	public void shouldDelegateStartCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.start(null, 0);
		verify(this.xaResource, times(1)).start(null, 0);
	}

	@Test
	public void shouldDelegateEndCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.end(null, 0);
		verify(this.xaResource, times(1)).end(null, 0);
	}

	@Test
	public void shouldDelegatePrepareCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.prepare(null);
		verify(this.xaResource, times(1)).prepare(null);
	}

	@Test
	public void shouldDelegateCommitCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.commit(null, true);
		verify(this.xaResource, times(1)).commit(null, true);
	}

	@Test
	public void shouldDelegateRollbackCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.rollback(null);
		verify(this.xaResource, times(1)).rollback(null);
	}

	@Test
	public void shouldDelegateIsSameRMCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.isSameRM(null);
		verify(this.xaResource, times(1)).isSameRM(null);
	}

	@Test
	public void shouldDelegateForgetCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.forget(null);
		verify(this.xaResource, times(1)).forget(null);
	}

	@Test
	public void shouldDelegateGetTransactionTimeoutCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.getTransactionTimeout();
		verify(this.xaResource, times(1)).getTransactionTimeout();
	}

	@Test
	public void shouldDelegateSetTransactionTimeoutCall() throws XAException {
		this.recoveryHelper.getXAResources();
		this.recoveryHelper.setTransactionTimeout(0);
		verify(this.xaResource, times(1)).setTransactionTimeout(0);
	}

}
