/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc;

import java.sql.SQLException;
import java.util.Arrays;

import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.JDBCConnectionPool;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.Lifecycle;
import org.springframework.util.Assert;

/**
 * A {@link Lifecycle} over the connection pool of a single
 * {@link oracle.ucp.jdbc.PoolDataSourceImpl}, which lets the pool be started and stopped
 * along with the application context without being destroyed in between.
 * <p>
 * {@link #start()} creates the pool when it does not exist yet, since a pool data source
 * has no {@code connectionPoolName} until then, and UCP registers a freshly created pool
 * in the stopped state. Both {@code start()} and {@link #stop()} guard on the current
 * life cycle state, as UCP rejects a transition that has already happened.
 *
 * @author Fabio Grassi
 * @since 4.1.0
 */
public final class OracleUcpCheckpointRestoreLifecycle implements Lifecycle {

	private static final Logger logger = LoggerFactory.getLogger(OracleUcpCheckpointRestoreLifecycle.class);

	private final PoolDataSourceImpl poolDataSource;

	public OracleUcpCheckpointRestoreLifecycle(final PoolDataSourceImpl poolDataSource) {
		Assert.notNull(poolDataSource, "Non null PoolDataSourceImpl instance expected");
		this.poolDataSource = poolDataSource;
	}

	@Override
	public void start() {
		JDBCConnectionPool pool = getPool(this.poolDataSource.getConnectionPoolName());
		if (pool == null) {
			pool = createPool();
			logger.info("Created new Oracle Universal Connection Pool named '{}'", pool.getName());
		}
		if (!pool.isLifecycleRunning() && !pool.isLifecycleStarting()) {
			doWithPool(pool::start);
			logger.info("Oracle Universal Connection Pool '{}' started", pool.getName());
		}
	}

	@Override
	public void stop() {
		final JDBCConnectionPool pool = getPool(this.poolDataSource.getConnectionPoolName());
		if (pool != null && !pool.isLifecycleStopped() && !pool.isLifecycleStopping()) {
			doWithPool(pool::stop);
			logger.info("Oracle Universal Connection Pool '{}' stopped", pool.getName());
		}
	}

	@Override
	public boolean isRunning() {
		final JDBCConnectionPool pool = getPool(this.poolDataSource.getConnectionPoolName());
		final boolean isRunning = pool != null && pool.isLifecycleRunning();
		logger.info("Oracle Universal Connection Pool '{}' is {}running", this.poolDataSource.getConnectionPoolName(),
				isRunning ? "" : "not ");
		return isRunning;
	}

	private JDBCConnectionPool createPool() {
		try {
			return (JDBCConnectionPool) this.poolDataSource.createUniversalConnectionPool();
		}
		catch (SQLException sqle) {
			throw new IllegalStateException("Failed to create new Oracle Universal Connection Pool", sqle);
		}
	}

	private static @Nullable JDBCConnectionPool getPool(final @Nullable String poolName) {
		try {
			final UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
				.getUniversalConnectionPoolManager();
			if (Arrays.asList(mgr.getConnectionPoolNames()).contains(poolName)) {
				return (JDBCConnectionPool) mgr.getConnectionPool(poolName);
			}
		}
		catch (UniversalConnectionPoolException ucpe) {
			throw new IllegalStateException("Failed to retrieve existing Oracle Universal Connection Pool", ucpe);
		}
		return null;
	}

	private static void doWithPool(final PoolCommand command) {
		try {
			command.execute();
		}
		catch (UniversalConnectionPoolException ucpe) {
			throw new IllegalStateException("Oracle Universal Connection Pool command failed", ucpe);
		}
	}

	@FunctionalInterface
	private interface PoolCommand {

		void execute() throws UniversalConnectionPoolException;

	}

}
