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

package org.springframework.boot.jdbc.metrics;

import java.io.Closeable;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.ToDoubleFunction;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import oracle.ucp.jdbc.JDBCConnectionPoolStatistics;
import oracle.ucp.jdbc.PoolDataSourceImpl;

import org.springframework.dao.InvalidDataAccessApiUsageException;

import static io.micrometer.core.instrument.binder.BaseUnits.CONNECTIONS;
import static java.util.Objects.requireNonNull;

/**
 * A {@link MeterBinder} for a {@link oracle.ucp.jdbc.PoolDataSourceImpl}.
 *
 * @author Fabio Grassi
 * @since 4.1.0
 */
public final class OracleUcpStatisticsMeterBinder implements MeterBinder, Closeable {

	private static final String SECONDS = ChronoUnit.SECONDS.toString();

	static final String METRIC_PREFIX = "oracleucp.connections.";

	static final MeterInfo[] METER_INFOS = {
			// Start - Current state
			new MeterInfo("curr.count", Tags.of("status", "available"),
					"Total number of available connections in the pool", CONNECTIONS,
					JDBCConnectionPoolStatistics::getAvailableConnectionsCount, Meter.Type.GAUGE),
			new MeterInfo("curr.count", Tags.of("status", "borrowed"),
					"Total number of borrowed connections in the pool", CONNECTIONS,
					JDBCConnectionPoolStatistics::getBorrowedConnectionsCount, Meter.Type.GAUGE),
			new MeterInfo("curr.count.open", Tags.empty(), "Total number of connections in the pool", CONNECTIONS,
					JDBCConnectionPoolStatistics::getTotalConnectionsCount, Meter.Type.GAUGE),
			new MeterInfo("curr.count.labeled", Tags.empty(), "Total number of labeled connections in the pool",
					CONNECTIONS, JDBCConnectionPoolStatistics::getLabeledConnectionsCount, Meter.Type.GAUGE),
			new MeterInfo("curr.count.pending", Tags.empty(), "Total number of pending requests count in the pool",
					CONNECTIONS, JDBCConnectionPoolStatistics::getPendingRequestsCount, Meter.Type.GAUGE),
			new MeterInfo("curr.count.remaining", Tags.empty(), "Remaining pool capacity count for the pool",
					CONNECTIONS, JDBCConnectionPoolStatistics::getRemainingPoolCapacityCount, Meter.Type.GAUGE),
			// End - Current state
			// Start - Aggregates since last reset of the pool
			new MeterInfo("aggr.count.abandoned", Tags.empty(), "Total number of abandoned connections in the pool",
					CONNECTIONS, JDBCConnectionPoolStatistics::getAbandonedConnectionsCount, Meter.Type.COUNTER),
			new MeterInfo("aggr.count.closed", Tags.empty(), "Total number of closed connections in the pool",
					CONNECTIONS, JDBCConnectionPoolStatistics::getConnectionsClosedCount, Meter.Type.COUNTER),
			new MeterInfo("aggr.count.created", Tags.empty(), "Total number of connections created in the pool",
					CONNECTIONS, JDBCConnectionPoolStatistics::getConnectionsCreatedCount, Meter.Type.COUNTER),
			new MeterInfo("aggr.average.borrowed", Tags.empty(), "Average count for borrowed connections in the pool",
					CONNECTIONS, JDBCConnectionPoolStatistics::getAverageBorrowedConnectionsCount, Meter.Type.GAUGE),
			new MeterInfo("aggr.average.wait.time", Tags.empty(), "Average connection wait time in the pool", SECONDS,
					JDBCConnectionPoolStatistics::getAverageConnectionWaitTime, Meter.Type.GAUGE),
			new MeterInfo("aggr.max.open", Tags.empty(), "Peak connections count in the pool", CONNECTIONS,
					JDBCConnectionPoolStatistics::getPeakConnectionsCount, Meter.Type.GAUGE),
			new MeterInfo("aggr.max.wait.time", Tags.empty(), "Peak connection wait time in the pool", SECONDS,
					JDBCConnectionPoolStatistics::getPeakConnectionWaitTime, Meter.Type.GAUGE),
			// End - Aggregates since last reset of the pool
			// Start - History since last restart of the application
			new MeterInfo("cum.count.borrowed", Tags.empty(), "Cumulative connection borrowed count for the pool",
					CONNECTIONS, JDBCConnectionPoolStatistics::getCumulativeConnectionBorrowedCount,
					Meter.Type.COUNTER),
			new MeterInfo("cum.count.returned", Tags.empty(), "Cumulative connection returned count in the pool",
					CONNECTIONS, JDBCConnectionPoolStatistics::getCumulativeConnectionReturnedCount,
					Meter.Type.COUNTER),
			new MeterInfo("cum.count.wait", Tags.of("status", "failure"),
					"Cumulative failed connection wait count for the pool", CONNECTIONS,
					JDBCConnectionPoolStatistics::getCumulativeFailedConnectionWaitCount, Meter.Type.COUNTER),
			new MeterInfo("cum.count.wait", Tags.of("status", "success"),
					"Cumulative successful connection wait count for the pool", CONNECTIONS,
					JDBCConnectionPoolStatistics::getCumulativeSuccessfulConnectionWaitCount, Meter.Type.COUNTER),
			new MeterInfo("cum.sum.use.time", Tags.empty(), "Cumulative connection use time for the pool", SECONDS,
					JDBCConnectionPoolStatistics::getCumulativeConnectionUseTime, Meter.Type.COUNTER),
			new MeterInfo("cum.sum.wait.time", Tags.of("status", "failure"),
					"Cumulative failed connection wait time for the pool", SECONDS,
					JDBCConnectionPoolStatistics::getCumulativeFailedConnectionWaitTime, Meter.Type.COUNTER),
			new MeterInfo("cum.sum.wait.time", Tags.of("status", "success"),
					"Cumulative successful connection wait time for the pool", SECONDS,
					JDBCConnectionPoolStatistics::getCumulativeSuccessfulConnectionWaitTime, Meter.Type.COUNTER)
			// End - History since last restart of the application
	};

	private final Tags tags;

	// We need to hold a strong reference to the state object
	private final JDBCConnectionPoolStatistics poolStatistics;

	private final Collection<Meter> meters;

	private MeterRegistry registry;

	public OracleUcpStatisticsMeterBinder(final PoolDataSourceImpl poolDataSource) {
		ensurePoolIsCreated(requireNonNull(poolDataSource, "'poolDataSource' must not be null"));
		this.tags = Tags.of("pool", requireNonNull(poolDataSource.getConnectionPoolName(),
				"'getConnectionPoolName()' must not return null"));
		this.poolStatistics = requireNonNull(poolDataSource.getStatistics(), "'getStatistics()' must not return null");
		this.meters = new LinkedList<>();
	}

	public static OracleUcpStatisticsMeterBinder of(final PoolDataSourceImpl poolDataSource) {
		return new OracleUcpStatisticsMeterBinder(poolDataSource);
	}

	@Override
	public void bindTo(final MeterRegistry registry) {
		this.registry = registry;
		for (MeterInfo meterInfo : METER_INFOS) {
			this.meters.add(registerMeter(meterInfo));
		}
	}

	private Meter registerMeter(final MeterInfo meterInfo) {
		return switch (meterInfo.type) {
			case GAUGE -> Gauge //
				.builder(METRIC_PREFIX + meterInfo.name, this.poolStatistics, meterInfo.function) //
				.tags(this.tags.and(meterInfo.specificTags)) //
				.description(meterInfo.description) //
				.baseUnit(meterInfo.baseUnit) //
				.register(this.registry); //
			case COUNTER -> FunctionCounter //
				.builder(METRIC_PREFIX + meterInfo.name, this.poolStatistics, meterInfo.function) //
				.tags(this.tags.and(meterInfo.specificTags)) //
				.description(meterInfo.description) //
				.baseUnit(meterInfo.baseUnit) //
				.register(this.registry); //
			default -> throw new IllegalArgumentException("Unexpected meter type: " + meterInfo.type);
		};
	}

	@Override
	public void close() {
		if (this.registry != null) {
			final Iterator<Meter> iterator = this.meters.iterator();
			while (iterator.hasNext()) {
				this.registry.remove(iterator.next());
				iterator.remove();
			}
		}
		else if (!this.meters.isEmpty()) {
			this.meters.clear();
		}
		this.registry = null;
	}

	private static void ensurePoolIsCreated(final PoolDataSourceImpl poolDataSource) {
		try {
			poolDataSource.createUniversalConnectionPool();
		}
		catch (SQLException e) {
			throw new InvalidDataAccessApiUsageException("Oracle connection pool creation failed", e);
		}
	}

	static record MeterInfo(String name, Tags specificTags, String description, String baseUnit,
			ToDoubleFunction<JDBCConnectionPoolStatistics> function, Meter.Type type) {

		@Override
		public String toString() {
			return "MeterId{" + "name='" + name() + "', specificTags=" + specificTags() + "}";
		}

	}

}
