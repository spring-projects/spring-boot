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

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConnectionManager}.
 *
 * @author Gytis Trikleris
 */
public class ConnectionManagerTests {

	@Mock
	private XADataSource xaDataSource;

	@Mock
	private XAConnection xaConnection;

	@Mock
	private XAResource xaResource;

	private String user = "testUser";

	private String pass = "testPass";

	private ConnectionManager connectionManager;

	@Before
	public void before() throws SQLException {
		MockitoAnnotations.initMocks(this);

		given(this.xaDataSource.getXAConnection()).willReturn(this.xaConnection);
		given(this.xaDataSource.getXAConnection(anyString(), anyString())).willReturn(this.xaConnection);
		given(this.xaConnection.getXAResource()).willReturn(this.xaResource);

		this.connectionManager = new ConnectionManager(this.xaDataSource, this.user, this.pass);
	}

	@Test
	public void shouldConnectWithoutCredentials() throws XAException, SQLException {
		this.connectionManager = new ConnectionManager(this.xaDataSource, null, null);
		this.connectionManager.connect();
		verify(this.xaDataSource, times(1)).getXAConnection();
		verify(this.xaDataSource, times(0)).getXAConnection(anyString(), anyString());
		assertThat(this.connectionManager.isConnected()).isTrue();
	}

	@Test
	public void shouldConnectWithCredentials() throws XAException, SQLException {
		this.connectionManager.connect();
		verify(this.xaDataSource, times(0)).getXAConnection();
		verify(this.xaDataSource, times(1)).getXAConnection(anyString(), anyString());
		assertThat(this.connectionManager.isConnected()).isTrue();
	}

	@Test
	public void shouldNotConnectWithExistingConnection() throws XAException, SQLException {
		this.connectionManager.connect();
		this.connectionManager.connect();
		verify(this.xaDataSource, times(1)).getXAConnection(anyString(), anyString());
		assertThat(this.connectionManager.isConnected()).isTrue();
	}

	@Test
	public void shouldFailToConnect() throws XAException, SQLException {
		given(this.xaDataSource.getXAConnection(anyString(), anyString())).willThrow(new SQLException("test"));
		try {
			this.connectionManager.connect();
			fail("XAException was expected");
		}
		catch (XAException e) {
			assertThat(e.errorCode).isEqualTo(XAException.XAER_RMFAIL);
		}

	}

	@Test
	public void shouldDisconnect() throws XAException, SQLException {
		this.connectionManager.connect();
		this.connectionManager.disconnect();
		verify(this.xaConnection, times(1)).close();
		assertThat(this.connectionManager.isConnected()).isFalse();
	}

	@Test
	public void shouldFailToDisconnect() throws XAException, SQLException {
		willThrow(new SQLException("test")).given(this.xaConnection).close();
		this.connectionManager.connect();
		this.connectionManager.disconnect();
		verify(this.xaConnection, times(1)).close();
		assertThat(this.connectionManager.isConnected()).isFalse();
	}

	@Test
	public void shouldNotDisconnectWithoutConnection() throws XAException, SQLException {
		this.connectionManager.disconnect();
		verify(this.xaConnection, times(0)).close();
		assertThat(this.connectionManager.isConnected()).isFalse();
	}

	@Test
	public void shouldAcceptWithoutConnecting() throws XAException, SQLException {
		this.connectionManager.connect();
		this.connectionManager.connectAndAccept(XAResource::getTransactionTimeout);
		verify(this.xaDataSource, times(1)).getXAConnection(anyString(), anyString());
		verify(this.xaResource, times(1)).getTransactionTimeout();
		assertThat(this.connectionManager.isConnected()).isTrue();
	}

	@Test
	public void shouldConnectAndAccept() throws XAException, SQLException {
		this.connectionManager.connectAndAccept(XAResource::getTransactionTimeout);
		verify(this.xaDataSource, times(1)).getXAConnection(anyString(), anyString());
		verify(this.xaConnection, times(1)).close();
		verify(this.xaResource, times(1)).getTransactionTimeout();
		assertThat(this.connectionManager.isConnected()).isFalse();
	}

	@Test
	public void shouldFailToConnectAndNotAccept() throws XAException, SQLException {
		given(this.xaDataSource.getXAConnection(anyString(), anyString())).willThrow(new SQLException("test"));
		try {
			this.connectionManager.connectAndAccept(XAResource::getTransactionTimeout);
			fail("Exception expected");
		}
		catch (XAException ignored) {
		}
		verify(this.xaDataSource, times(1)).getXAConnection(anyString(), anyString());
		verify(this.xaConnection, times(0)).close();
		verify(this.xaConnection, times(0)).getXAResource();
		verify(this.xaResource, times(0)).getTransactionTimeout();
		assertThat(this.connectionManager.isConnected()).isFalse();
	}

	@Test
	public void shouldApplyWithoutConnecting() throws XAException, SQLException {
		this.connectionManager.connect();
		this.connectionManager.connectAndApply(XAResource::getTransactionTimeout);
		verify(this.xaDataSource, times(1)).getXAConnection(anyString(), anyString());
		verify(this.xaConnection, times(1)).getXAResource();
		verify(this.xaResource, times(1)).getTransactionTimeout();
		assertThat(this.connectionManager.isConnected()).isTrue();
	}

	@Test
	public void shouldConnectAndApply() throws XAException, SQLException {
		this.connectionManager.connectAndApply(XAResource::getTransactionTimeout);
		verify(this.xaDataSource, times(1)).getXAConnection(anyString(), anyString());
		verify(this.xaConnection, times(1)).close();
		verify(this.xaConnection, times(1)).getXAResource();
		verify(this.xaResource, times(1)).getTransactionTimeout();
		assertThat(this.connectionManager.isConnected()).isFalse();
	}

	@Test
	public void shouldFailToConnectAndNotApply() throws XAException, SQLException {
		given(this.xaDataSource.getXAConnection(anyString(), anyString())).willThrow(new SQLException("test"));
		try {
			this.connectionManager.connectAndApply(XAResource::getTransactionTimeout);
			fail("Exception expected");
		}
		catch (XAException ignored) {
		}
		verify(this.xaDataSource, times(1)).getXAConnection(anyString(), anyString());
		verify(this.xaConnection, times(0)).close();
		verify(this.xaConnection, times(0)).getXAResource();
		verify(this.xaResource, times(0)).getTransactionTimeout();
		assertThat(this.connectionManager.isConnected()).isFalse();
	}

}
