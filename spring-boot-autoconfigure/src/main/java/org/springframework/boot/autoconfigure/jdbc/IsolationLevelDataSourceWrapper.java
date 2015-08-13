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
package org.springframework.boot.autoconfigure.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.UsesJava7;

/**
 * {@link DataSource} wrapper to allow define default isolation level (on returned {@link Connection} instance).
 * 
 * @author Arnost Havelka
 *
 */
public class IsolationLevelDataSourceWrapper implements DataSource {
	
	private static Log logger = LogFactory.getLog(IsolationLevelDataSourceWrapper.class);
	
	private DataSource datasource;
	
	private int isolationLevel;
	
	public IsolationLevelDataSourceWrapper(DataSource datasource, int isolationLevel) {
		super();
		this.datasource = datasource;
		this.isolationLevel = isolationLevel;
	}

	/**
	 * Apply defined isolation level.
	 * 
	 * @param con instance of {@link Connection}
	 * @return same instance of {@link Connection} (with defined isolation level)
	 */
	private Connection updateConnection(Connection con) {
		try {
			con.setTransactionIsolation(isolationLevel);
		}
		catch (SQLException e) {
			logger.error("Setting isolation level to datasource failed!", e);
		}
		return con;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return updateConnection(datasource.getConnection());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return updateConnection(datasource.getConnection(username, password));
	}

	
	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return datasource.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		datasource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		datasource.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return datasource.getLoginTimeout();
	}

	@UsesJava7
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return datasource.getParentLogger();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return datasource.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return datasource.isWrapperFor(iface);
	}

}
