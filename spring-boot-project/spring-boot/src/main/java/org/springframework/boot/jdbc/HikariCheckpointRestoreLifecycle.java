/*
 * Copyright 2012-2025 the original author or authors.
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

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.pool.HikariPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link Lifecycle} for a {@link HikariDataSource} allowing it to participate in
 * checkpoint-restore. When {@link #stop() stopped}, and the data source
 * {@link HikariDataSource#isAllowPoolSuspension() allows it}, its pool is suspended,
 * blocking any attempts to borrow connections. Open and idle connections are then
 * evicted. When subsequently {@link #start() started}, the pool is
 * {@link HikariPoolMXBean#resumePool() resumed} if necessary.
 *
 * @author Christoph Strobl
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public class HikariCheckpointRestoreLifecycle implements Lifecycle {

	private static final Log logger = LogFactory.getLog(HikariCheckpointRestoreLifecycle.class);

	private static final Field CLOSE_CONNECTION_EXECUTOR;

	static {
		Field closeConnectionExecutor = ReflectionUtils.findField(HikariPool.class, "closeConnectionExecutor");
		Assert.state(closeConnectionExecutor != null, "Unable to locate closeConnectionExecutor for HikariPool");
		Assert.state(ThreadPoolExecutor.class.isAssignableFrom(closeConnectionExecutor.getType()),
				() -> "Expected ThreadPoolExecutor for closeConnectionExecutor but found %s"
					.formatted(closeConnectionExecutor.getType()));
		ReflectionUtils.makeAccessible(closeConnectionExecutor);
		CLOSE_CONNECTION_EXECUTOR = closeConnectionExecutor;
	}

	private final Function<HikariPool, Boolean> hasOpenConnections;

	private final HikariDataSource dataSource;

	private final ConfigurableApplicationContext applicationContext;

	/**
	 * Creates a new {@code HikariCheckpointRestoreLifecycle} that will allow the given
	 * {@code dataSource} to participate in checkpoint-restore. The {@code dataSource} is
	 * {@link DataSourceUnwrapper#unwrap unwrapped} to a {@link HikariDataSource}. If such
	 * unwrapping is not possible, the lifecycle will have no effect.
	 * @param dataSource the checkpoint-restore participant
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #HikariCheckpointRestoreLifecycle(DataSource, ConfigurableApplicationContext)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public HikariCheckpointRestoreLifecycle(DataSource dataSource) {
		this(dataSource, null);
	}

	/**
	 * Creates a new {@code HikariCheckpointRestoreLifecycle} that will allow the given
	 * {@code dataSource} to participate in checkpoint-restore. The {@code dataSource} is
	 * {@link DataSourceUnwrapper#unwrap unwrapped} to a {@link HikariDataSource}. If such
	 * unwrapping is not possible, the lifecycle will have no effect.
	 * @param dataSource the checkpoint-restore participant
	 * @param applicationContext the application context
	 * @since 3.4.0
	 */
	public HikariCheckpointRestoreLifecycle(DataSource dataSource, ConfigurableApplicationContext applicationContext) {
		this.dataSource = DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class, HikariDataSource.class);
		this.applicationContext = applicationContext;
		this.hasOpenConnections = (pool) -> {
			ThreadPoolExecutor closeConnectionExecutor = (ThreadPoolExecutor) ReflectionUtils
				.getField(CLOSE_CONNECTION_EXECUTOR, pool);
			Assert.state(closeConnectionExecutor != null, "'closeConnectionExecutor' was null");
			return closeConnectionExecutor.getActiveCount() > 0;
		};
	}

	@Override
	public void start() {
		if (this.dataSource == null || this.dataSource.isRunning()) {
			return;
		}
		Assert.state(!this.dataSource.isClosed(), "DataSource has been closed and cannot be restarted");
		if (this.dataSource.isAllowPoolSuspension()) {
			logger.info("Resuming Hikari pool");
			this.dataSource.getHikariPoolMXBean().resumePool();
		}
	}

	@Override
	public void stop() {
		if (this.dataSource == null || !this.dataSource.isRunning()) {
			return;
		}
		if (this.dataSource.isAllowPoolSuspension()) {
			logger.info("Suspending Hikari pool");
			this.dataSource.getHikariPoolMXBean().suspendPool();
		}
		else {
			if (this.applicationContext != null && !this.applicationContext.isClosed()) {
				logger.warn(this.dataSource + " is not configured to allow pool suspension. "
						+ "This will cause problems when the application is checkpointed. "
						+ "Please configure allow-pool-suspension to fix this!");
			}
		}
		closeConnections(Duration.ofMillis(this.dataSource.getConnectionTimeout() + 250));
	}

	private void closeConnections(Duration shutdownTimeout) {
		logger.info("Evicting Hikari connections");
		this.dataSource.getHikariPoolMXBean().softEvictConnections();
		logger.debug(LogMessage.format("Waiting %d seconds for Hikari connections to be closed",
				shutdownTimeout.toSeconds()));
		CompletableFuture<Void> allConnectionsClosed = CompletableFuture.runAsync(this::waitForConnectionsToClose);
		try {
			allConnectionsClosed.get(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS);
			logger.debug("Hikari connections closed");
		}
		catch (InterruptedException ex) {
			logger.warn("Interrupted while waiting for connections to be closed", ex);
			Thread.currentThread().interrupt();
		}
		catch (TimeoutException ex) {
			logger.warn(LogMessage.format("Hikari connections could not be closed within %s", shutdownTimeout), ex);
		}
		catch (ExecutionException ex) {
			throw new RuntimeException("Failed to close Hikari connections", ex);
		}
	}

	private void waitForConnectionsToClose() {
		while (this.hasOpenConnections.apply((HikariPool) this.dataSource.getHikariPoolMXBean())) {
			try {
				TimeUnit.MILLISECONDS.sleep(50);
			}
			catch (InterruptedException ex) {
				logger.error("Interrupted while waiting for datasource connections to be closed", ex);
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.dataSource != null && this.dataSource.isRunning();
	}

}
