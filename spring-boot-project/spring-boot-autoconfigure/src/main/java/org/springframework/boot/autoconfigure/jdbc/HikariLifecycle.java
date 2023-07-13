/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.pool.HikariPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.Lifecycle;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link Lifecycle} management for a {@link HikariDataSource} taking care of
 * {@link Lifecycle#start() starting}/{@link Lifecycle#stop() stopping} the
 * {@link javax.sql.DataSource} by {@link HikariDataSource#isAllowPoolSuspension()
 * eventually} suspending/resuming the underlying {@link HikariPool connection pool} and
 * {@link HikariPoolMXBean#softEvictConnections() evicting} open & idle connections.
 *
 * @author Christoph Strobl
 */
class HikariLifecycle implements Lifecycle {

	private final HikariDataSource dataSource;

	private final LifecycleExecutor lifecycleExecutor;

	HikariLifecycle(HikariDataSource dataSource) {

		this.dataSource = dataSource;
		this.lifecycleExecutor = new LifecycleExecutor(dataSource);
	}

	HikariDataSource getManagedInstance() {
		return this.dataSource;
	}

	@Override
	public void start() {

		if (this.dataSource.isRunning()) {
			return;
		}

		if (this.dataSource.isClosed()) {
			throw new IllegalStateException("DataSource has been closed and cannot be restarted");
		}

		this.lifecycleExecutor.resume();
	}

	@Override
	public void stop() {
		if (this.dataSource.isRunning()) {
			this.lifecycleExecutor.pause();
		}
	}

	@Override
	public boolean isRunning() {
		return this.dataSource.isRunning();
	}

	/**
	 * Component to help suspend/resume a {@link HikariDataSource} by taking the pool
	 * suspension flag into account. Will perform best effort to make sure connections
	 * reported as closed buy the {@link HikariPoolMXBean} have actually been closed by
	 * the {@link java.util.concurrent.Executor} that is in charge of closing them.
	 *
	 * @author Christoph Strobl
	 */
	private static class LifecycleExecutor {

		private static final Log logger = LogFactory.getLog(LifecycleExecutor.class);

		private final HikariDataSource dataSource;

		private Supplier<Boolean> hasOpenConnections;

		LifecycleExecutor(HikariDataSource hikariDataSource) {

			this.dataSource = getUltimateTargetObject(hikariDataSource);

			if (hikariDataSource.getHikariPoolMXBean() instanceof HikariPool pool) {

				Field closeConnectionExecutor = ReflectionUtils.findField(HikariPool.class, "closeConnectionExecutor");
				if (closeConnectionExecutor != null) {

					ReflectionUtils.makeAccessible(closeConnectionExecutor);
					Object field = ReflectionUtils.getField(closeConnectionExecutor, pool);
					if (field instanceof ThreadPoolExecutor executor) {
						this.hasOpenConnections = () -> executor.getActiveCount() > 0;
					}
				}
			}
			if (this.hasOpenConnections == null) {
				this.hasOpenConnections = () -> hikariDataSource.getHikariPoolMXBean().getTotalConnections() > 0;
			}
		}

		/**
		 * Pause the {@link HikariDataSource} and try to suspend obtaining new connections
		 * from the pool if possible. Will wait for connection to be closed. Default
		 * timeout is set to {@link HikariDataSource#getConnectionTimeout()} + 250 ms.
		 */
		void pause() {
			pause(Duration.ofMillis(this.dataSource.getConnectionTimeout() + 250));
		}

		/**
		 * Pause the {@link HikariDataSource} and try to suspend obtaining new connections
		 * from the pool if possible. Wait at most the given {@literal shutdownTimeout}
		 * for connections to be closed.
		 * @param shutdownTimeout max timeout to wait for connections to be closed.
		 */
		void pause(Duration shutdownTimeout) {

			if (this.dataSource.isAllowPoolSuspension()) {
				logger.info("Suspending Hikari pool");
				this.dataSource.getHikariPoolMXBean().suspendPool();
			}
			closeConnections(shutdownTimeout);
		}

		/**
		 * Resume the {@link HikariDataSource} by lifting the pool suspension if set.
		 */
		void resume() {

			if (this.dataSource.isAllowPoolSuspension()) {
				logger.info("Resuming Hikari pool");
				this.dataSource.getHikariPoolMXBean().resumePool();
			}
		}

		void closeConnections(Duration shutdownTimeout) {

			logger.info("Evicting Hikari connections");
			this.dataSource.getHikariPoolMXBean().softEvictConnections();

			logger.debug("Waiting for Hikari connections to be closed");
			CompletableFuture<Void> allConnectionsClosed = CompletableFuture.runAsync(this::waitForConnectionsToClose);
			try {
				allConnectionsClosed.get(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS);
				logger.debug("Hikari connections closed");
			}
			catch (InterruptedException ex) {
				logger.error("Interrupted while waiting for connections to be closed", ex);
				Thread.currentThread().interrupt();
			}
			catch (TimeoutException ex) {
				logger.error("Hikari connections could not be closed within %s".formatted(shutdownTimeout), ex);
			}
			catch (ExecutionException ex) {
				throw new RuntimeException("Failed to close Hikari connections", ex);
			}
		}

		private void waitForConnectionsToClose() {
			while (this.hasOpenConnections.get()) {
				try {
					TimeUnit.MILLISECONDS.sleep(50);
				}
				catch (InterruptedException ex) {
					logger.error("Interrupted while waiting for datasource connections to be closed", ex);
					Thread.currentThread().interrupt();
				}
			}
		}

		@SuppressWarnings("unchecked")
		private static <T> T getUltimateTargetObject(Object candidate) {
			Assert.notNull(candidate, "Candidate must not be null");
			try {
				if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised advised) {
					TargetSource targetSource = advised.getTargetSource();
					if (targetSource.isStatic()) {
						Object target = targetSource.getTarget();
						if (target != null) {
							return getUltimateTargetObject(target);
						}
					}
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to unwrap proxied object", ex);
			}
			return (T) candidate;
		}

	}

}
