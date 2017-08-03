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

package org.springframework.boot.jta.narayana;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DataSourceXAResourceRecoveryHelper}.
 *
 * @author Gytis Trikleris
 */
public class DataSourceXAResourceRecoveryHelperTests {

	@Mock
	private ConnectionManager connectionManager;

	private DataSourceXAResourceRecoveryHelper recoveryHelper;

	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);

		this.recoveryHelper = new DataSourceXAResourceRecoveryHelper(this.connectionManager);
	}

	@Test
	public void shouldCreateConnectionAndGetXAResource() throws XAException {
		given(this.connectionManager.isConnected()).willReturn(false);

		XAResource[] xaResources = this.recoveryHelper.getXAResources();

		assertThat(xaResources.length).isEqualTo(1);
		assertThat(xaResources[0]).isSameAs(this.recoveryHelper);
		verify(this.connectionManager, times(1)).isConnected();
		verify(this.connectionManager, times(1)).connect();
	}

	@Test
	public void shouldGetXAResourceWithoutConnecting() throws XAException {
		given(this.connectionManager.isConnected()).willReturn(true);

		XAResource[] xaResources = this.recoveryHelper.getXAResources();

		assertThat(xaResources.length).isEqualTo(1);
		assertThat(xaResources[0]).isSameAs(this.recoveryHelper);
		verify(this.connectionManager, times(1)).isConnected();
		verify(this.connectionManager, times(0)).connect();
	}

	@Test
	public void shouldFailToCreateConnectionAndNotGetXAResource() throws XAException {
		given(this.connectionManager.isConnected()).willReturn(false);
		willThrow(new XAException("test")).given(this.connectionManager).connect();

		XAResource[] xaResources = this.recoveryHelper.getXAResources();

		assertThat(xaResources.length).isEqualTo(0);
		verify(this.connectionManager, times(1)).isConnected();
		verify(this.connectionManager, times(1)).connect();
	}

	@Test
	public void shouldDelegateRecoverCall() throws XAException {
		this.recoveryHelper.recover(XAResource.TMSTARTRSCAN);
		verify(this.connectionManager, times(1)).connectAndApply(any());
		verify(this.connectionManager, times(0)).disconnect();
	}

	@Test
	public void shouldDelegateRecoverCallAndCloseConnection() throws XAException {
		this.recoveryHelper.recover(XAResource.TMENDRSCAN);
		verify(this.connectionManager, times(1)).connectAndApply(any());
		verify(this.connectionManager, times(1)).disconnect();
	}

	@Test
	public void shouldDelegateStartCall() throws XAException {
		this.recoveryHelper.start(null, 0);
		verify(this.connectionManager, times(1)).connectAndAccept(any());
	}

	@Test
	public void shouldDelegateEndCall() throws XAException {
		this.recoveryHelper.end(null, 0);
		verify(this.connectionManager, times(1)).connectAndAccept(any());
	}

	@Test
	public void shouldDelegatePrepareCall() throws XAException {
		given(this.connectionManager.connectAndApply(any())).willReturn(10);
		assertThat(this.recoveryHelper.prepare(null)).isEqualTo(10);
		verify(this.connectionManager, times(1)).connectAndApply(any());
	}

	@Test
	public void shouldDelegateCommitCall() throws XAException {
		this.recoveryHelper.commit(null, true);
		verify(this.connectionManager, times(1)).connectAndAccept(any());
	}

	@Test
	public void shouldDelegateRollbackCall() throws XAException {
		this.recoveryHelper.rollback(null);
		verify(this.connectionManager, times(1)).connectAndAccept(any());
	}

	@Test
	public void shouldDelegateIsSameRMCall() throws XAException {
		given(this.connectionManager.connectAndApply(any())).willReturn(true);
		assertThat(this.recoveryHelper.isSameRM(null)).isTrue();
		verify(this.connectionManager, times(1)).connectAndApply(any());
	}

	@Test
	public void shouldDelegateForgetCall() throws XAException {
		this.recoveryHelper.forget(null);
		verify(this.connectionManager, times(1)).connectAndAccept(any());
	}

	@Test
	public void shouldDelegateGetTransactionTimeoutCall() throws XAException {
		given(this.connectionManager.connectAndApply(any())).willReturn(10);
		assertThat(this.recoveryHelper.getTransactionTimeout()).isEqualTo(10);
		verify(this.connectionManager, times(1)).connectAndApply(any());
	}

	@Test
	public void shouldDelegateSetTransactionTimeoutCall() throws XAException {
		given(this.connectionManager.connectAndApply(any())).willReturn(true);
		assertThat(this.recoveryHelper.setTransactionTimeout(0)).isTrue();
		verify(this.connectionManager, times(1)).connectAndApply(any());
	}

}
