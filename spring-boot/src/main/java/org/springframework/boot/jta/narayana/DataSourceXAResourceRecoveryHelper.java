/*
 * Copyright 2012-2015 the original author or authors.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

/**
 * XAResourceRecoveryHelper implementation which gets Xids, which needs to be recovered, from the database.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class DataSourceXAResourceRecoveryHelper implements XAResourceRecoveryHelper, XAResource {

	private static final Logger LOGGER = Logger.getLogger(DataSourceXAResourceRecoveryHelper.class.getName());

	private final XADataSource xaDataSource;

	private final String user;

	private final String pass;

	private XAConnection xaConnection;

	private XAResource delegate;

	public DataSourceXAResourceRecoveryHelper(XADataSource xaDataSource) {
		this(xaDataSource, null, null);
	}

	public DataSourceXAResourceRecoveryHelper(XADataSource xaDataSource, String user, String pass) {
		this.xaDataSource = xaDataSource;
		this.user = user;
		this.pass = pass;
	}

	@Override
	public boolean initialise(String properties) {
		return true;
	}

	@Override
	public XAResource[] getXAResources() {
		if (connect()) {
			return new XAResource[] { this };
		}

		return new XAResource[0];
	}

	@Override
	public Xid[] recover(int i) throws XAException {
		try {
			return this.delegate.recover(i);
		}
		finally {
			if (i == XAResource.TMENDRSCAN) {
				disconnect();
			}
		}
	}

	@Override
	public void start(Xid xid, int i) throws XAException {
		this.delegate.start(xid, i);
	}

	@Override
	public void end(Xid xid, int i) throws XAException {
		this.delegate.end(xid, i);
	}

	@Override
	public int prepare(Xid xid) throws XAException {
		return this.delegate.prepare(xid);
	}

	@Override
	public void commit(Xid xid, boolean b) throws XAException {
		this.delegate.commit(xid, b);
	}

	@Override
	public void rollback(Xid xid) throws XAException {
		this.delegate.rollback(xid);
	}

	@Override
	public boolean isSameRM(XAResource xaResource) throws XAException {
		return this.delegate.isSameRM(xaResource);
	}

	@Override
	public void forget(Xid xid) throws XAException {
		this.delegate.forget(xid);
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return this.delegate.getTransactionTimeout();
	}

	@Override
	public boolean setTransactionTimeout(int i) throws XAException {
		return this.delegate.setTransactionTimeout(i);
	}

	private boolean connect() {
		if (this.delegate != null) {
			return true;
		}

		try {
			this.xaConnection = getXaConnection();
			this.delegate = this.xaConnection.getXAResource();
		}
		catch (SQLException e) {
			LOGGER.log(Level.WARNING, "Failed to create connection", e);
			return false;
		}

		return true;
	}

	private void disconnect() throws XAException {
		try {
			this.xaConnection.close();
		}
		catch (SQLException e) {
			LOGGER.log(Level.WARNING, "Failed to close connection", e);
		}
		finally {
			this.xaConnection = null;
			this.delegate = null;
		}
	}

	private XAConnection getXaConnection() throws SQLException {
		if (this.user == null && this.pass == null) {
			return this.xaDataSource.getXAConnection();
		}

		return this.xaDataSource.getXAConnection(this.user, this.pass);
	}

}
