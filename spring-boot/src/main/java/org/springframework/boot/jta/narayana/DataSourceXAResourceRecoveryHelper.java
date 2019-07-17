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

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * XAResourceRecoveryHelper implementation which gets XIDs, which needs to be recovered,
 * from the database.
 *
 * @author Gytis Trikleris
 * @since 1.4.0
 */
public class DataSourceXAResourceRecoveryHelper implements XAResourceRecoveryHelper, XAResource {

	private static final XAResource[] NO_XA_RESOURCES = {};

	private static final Log logger = LogFactory.getLog(DataSourceXAResourceRecoveryHelper.class);

	private final XADataSource xaDataSource;

	private final String user;

	private final String password;

	private XAConnection xaConnection;

	private XAResource delegate;

	/**
	 * Create a new {@link DataSourceXAResourceRecoveryHelper} instance.
	 * @param xaDataSource the XA data source
	 */
	public DataSourceXAResourceRecoveryHelper(XADataSource xaDataSource) {
		this(xaDataSource, null, null);
	}

	/**
	 * Create a new {@link DataSourceXAResourceRecoveryHelper} instance.
	 * @param xaDataSource the XA data source
	 * @param user the database user or {@code null}
	 * @param password the database password or {@code null}
	 */
	public DataSourceXAResourceRecoveryHelper(XADataSource xaDataSource, String user, String password) {
		Assert.notNull(xaDataSource, "XADataSource must not be null");
		this.xaDataSource = xaDataSource;
		this.user = user;
		this.password = password;
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
		return NO_XA_RESOURCES;
	}

	private boolean connect() {
		if (this.delegate == null) {
			try {
				this.xaConnection = getXaConnection();
				this.delegate = this.xaConnection.getXAResource();
			}
			catch (SQLException ex) {
				logger.warn("Failed to create connection", ex);
				return false;
			}
		}
		return true;
	}

	private XAConnection getXaConnection() throws SQLException {
		if (this.user == null && this.password == null) {
			return this.xaDataSource.getXAConnection();
		}
		return this.xaDataSource.getXAConnection(this.user, this.password);
	}

	@Override
	public Xid[] recover(int flag) throws XAException {
		try {
			return getDelegate(true).recover(flag);
		}
		finally {
			if (flag == XAResource.TMENDRSCAN) {
				disconnect();
			}
		}
	}

	private void disconnect() throws XAException {
		try {
			this.xaConnection.close();
		}
		catch (SQLException ex) {
			logger.warn("Failed to close connection", ex);
		}
		finally {
			this.xaConnection = null;
			this.delegate = null;
		}
	}

	@Override
	public void start(Xid xid, int flags) throws XAException {
		getDelegate(true).start(xid, flags);
	}

	@Override
	public void end(Xid xid, int flags) throws XAException {
		getDelegate(true).end(xid, flags);
	}

	@Override
	public int prepare(Xid xid) throws XAException {
		return getDelegate(true).prepare(xid);
	}

	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		getDelegate(true).commit(xid, onePhase);
	}

	@Override
	public void rollback(Xid xid) throws XAException {
		getDelegate(true).rollback(xid);
	}

	@Override
	public boolean isSameRM(XAResource xaResource) throws XAException {
		return getDelegate(true).isSameRM(xaResource);
	}

	@Override
	public void forget(Xid xid) throws XAException {
		getDelegate(true).forget(xid);
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return getDelegate(true).getTransactionTimeout();
	}

	@Override
	public boolean setTransactionTimeout(int seconds) throws XAException {
		return getDelegate(true).setTransactionTimeout(seconds);
	}

	private XAResource getDelegate(boolean required) {
		Assert.state(this.delegate != null || !required, "Connection has not been opened");
		return this.delegate;
	}

}
