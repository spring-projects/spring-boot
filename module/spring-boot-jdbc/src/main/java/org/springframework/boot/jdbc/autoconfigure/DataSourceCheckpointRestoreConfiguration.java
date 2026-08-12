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

package org.springframework.boot.jdbc.autoconfigure;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.OracleConnection;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCheckpointRestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.DataSourceUnwrapper;
import org.springframework.boot.jdbc.HikariCheckpointRestoreLifecycle;
import org.springframework.boot.jdbc.OracleUcpCheckpointRestoreLifecycle;
import org.springframework.boot.jdbc.autoconfigure.DataSourceCheckpointRestoreConfiguration.CheckpointRestorePoolsAvailableCondition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Checkpoint-restore specific configuration.
 *
 * @author Olga Maciaszek-Sharma
 * @author Fabio Grassi
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCheckpointRestore
@Conditional(CheckpointRestorePoolsAvailableCondition.class)
class DataSourceCheckpointRestoreConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	static class Hikari {

		@Bean
		@ConditionalOnMissingBean
		HikariCheckpointRestoreLifecycleRegistry hikariCheckpointRestoreLifecycle(
				final ObjectProvider<DataSource> dataSources, final ConfigurableApplicationContext applicationContext) {
			return new HikariCheckpointRestoreLifecycleRegistry(dataSources, applicationContext);
		}

		static final class HikariCheckpointRestoreLifecycleRegistry
				extends DataSourceCheckpointRestoreLifecycleRegistry<HikariConfigMXBean, HikariDataSource> {

			HikariCheckpointRestoreLifecycleRegistry(final ObjectProvider<DataSource> dataSources,
					final ConfigurableApplicationContext applicationContext) {
				super(dataSources, HikariConfigMXBean.class, HikariDataSource.class,
						hds -> new HikariCheckpointRestoreLifecycle(hds, applicationContext));
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ PoolDataSourceImpl.class, OracleConnection.class })
	static class OracleUcp {

		@Bean
		@ConditionalOnMissingBean
		OracleUcpCheckpointRestoreLifecycleRegistry oracleUcpCheckpointRestoreLifecycle(
				final ObjectProvider<DataSource> dataSources) {
			return new OracleUcpCheckpointRestoreLifecycleRegistry(dataSources);
		}

		static final class OracleUcpCheckpointRestoreLifecycleRegistry
				extends DataSourceCheckpointRestoreLifecycleRegistry<PoolDataSource, PoolDataSourceImpl> {

			OracleUcpCheckpointRestoreLifecycleRegistry(final ObjectProvider<DataSource> dataSources) {
				super(dataSources, PoolDataSource.class, PoolDataSourceImpl.class,
						OracleUcpCheckpointRestoreLifecycle::new);
			}

		}

	}

	static class CheckpointRestorePoolsAvailableCondition extends AnyNestedCondition {

		CheckpointRestorePoolsAvailableCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnClass(HikariDataSource.class)
		static class HickariAvailable {

		}

		@ConditionalOnClass({ PoolDataSourceImpl.class, OracleConnection.class })
		static class OracleUcpAvailable {

		}

	}

	/**
	 * A {@link Lifecycle} container that propagates {@code start()} and {@code stop()}
	 * signals to all its elements and {@code isRunning()} if and only if all its elements
	 * are running or there are no elements.
	 * <p>
	 * This class implements also {@link SmartInitializingSingleton} to hook into the bean
	 * factory lifecyle after all singleton beans registration and iterate over all
	 * {@code DataSource}s, including the ones that are neither default nor autowire
	 * candidates, unwrap each of them to reach the underlying data source, supply it to
	 * the given factory to create a {@code Lifecycle} and add it its elements.
	 *
	 * @author Fabio Grassi
	 * @since 4.1.0
	 */
	static sealed class DataSourceCheckpointRestoreLifecycleRegistry<I, T extends I>
			implements SmartInitializingSingleton, Lifecycle {

		private final ObjectProvider<DataSource> dataSources;

		private final Class<I> wrappingInterface;

		private final Class<T> targetClass;

		private final Function<T, Lifecycle> lifecycleFactory;

		private final Collection<Lifecycle> lifecycles;

		DataSourceCheckpointRestoreLifecycleRegistry(final ObjectProvider<DataSource> dataSources,
				final Class<I> wrappingInterface, final Class<T> targetClass,
				final Function<T, Lifecycle> lifecycleFactory) {
			this.dataSources = dataSources;
			this.wrappingInterface = wrappingInterface;
			this.targetClass = targetClass;
			this.lifecycleFactory = lifecycleFactory;
			this.lifecycles = new LinkedList<>();
		}

		@Override
		public void afterSingletonsInstantiated() {
			this.dataSources.stream(ObjectProvider.UNFILTERED, false).forEach(ds -> {
				final T unwrapped = DataSourceUnwrapper.unwrap(ds, this.wrappingInterface, this.targetClass);
				if (unwrapped != null) {
					this.lifecycles.add(this.lifecycleFactory.apply(unwrapped));
				}
			});
		}

		@Override
		public void start() {
			this.lifecycles.forEach(Lifecycle::start);
		}

		@Override
		public void stop() {
			this.lifecycles.forEach(Lifecycle::stop);
		}

		@Override
		public boolean isRunning() {
			return this.lifecycles.stream().allMatch(Lifecycle::isRunning);
		}

	}

}
