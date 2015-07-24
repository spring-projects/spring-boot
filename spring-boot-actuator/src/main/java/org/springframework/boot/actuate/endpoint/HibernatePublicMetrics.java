/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;

/**
 * A {@link PublicMetrics} implementation that provides Hibernate metrics. It exposes the same stats as would be
 * exposed when calling {@code Statistics#logSummary}.
 *
 * @author Marten Deinum
 * @since 2.0.0
 */
public class HibernatePublicMetrics implements PublicMetrics {

	@Autowired
	private ApplicationContext applicationContext;

	private final Map<String, EntityManagerFactory> metadataByPrefix = new HashMap<>();

	@PostConstruct
	public void initialize() {
		EntityManagerFactory primaryEntityManagerFactory = getPrimaryEntityManagerFactory();
		for (Map.Entry<String, EntityManagerFactory> entry : this.applicationContext.getBeansOfType(EntityManagerFactory.class).entrySet()) {
			String beanName = entry.getKey();
			EntityManagerFactory bean = entry.getValue();
			if (hasStatisticsEnabled(bean)) {
				String prefix = createPrefix(beanName, bean, bean.equals(primaryEntityManagerFactory));
				this.metadataByPrefix.put(prefix, bean);
			}
		}
	}

	private String createPrefix(String name, EntityManagerFactory bean, boolean primary) {
		return primary ? "hibernate.primary" : "hibernate." + name;
	}

	private boolean hasStatisticsEnabled(EntityManagerFactory emf) {
		Statistics stats = getStatistics(emf);
		return (stats != null && stats.isStatisticsEnabled());
	}

	/**
	 * Get the {@code Statistics} object from the underlying {@code SessionFactory}. If it isn't hibernate that is
	 * used return {@code null}.
	 *
	 * @param emf a {@code EntityManagerFactory}
	 * @return the {@code Statistics} from the underlying {@code SessionFactory} or {@code null}.
	 */
	private Statistics getStatistics(EntityManagerFactory emf) {
		try {
			SessionFactory sf = emf.unwrap(SessionFactory.class);
			return sf.getStatistics();
		}
		catch (PersistenceException pe) {
			return null;
		}
	}


	private <T extends Number> void addMetric(Set<Metric<?>> metrics, String name, T value) {
		if (value != null) {
			metrics.add(new Metric<T>(name, value));
		}
	}

	@Override
	public Collection<Metric<?>> metrics() {

		Set<Metric<?>> metrics = new LinkedHashSet<Metric<?>>();

		for (Map.Entry<String, EntityManagerFactory> entry : metadataByPrefix.entrySet()) {
			Statistics stats = getStatistics(entry.getValue());
			String prefix = entry.getKey();
			prefix = (prefix.endsWith(".") ? prefix : prefix + ".");
			addMetric(metrics, prefix + "start-time", stats.getStartTime());

			// Session stats
			addMetric(metrics, prefix + "sessions.open", stats.getSessionOpenCount());
			addMetric(metrics, prefix + "sessions.close", stats.getSessionCloseCount());

			// Transaction stats
			addMetric(metrics, prefix + "transactions", stats.getTransactionCount());
			addMetric(metrics, prefix + "transactions.success", stats.getSuccessfulTransactionCount());

			addMetric(metrics, prefix + "optimistic_failure_count", stats.getOptimisticFailureCount());
			addMetric(metrics, prefix + "flushes", stats.getFlushCount());
			addMetric(metrics, prefix + "connections.obtained", stats.getConnectCount());

			// Statements
			addMetric(metrics, prefix + "statements.prepared", stats.getPrepareStatementCount());
			addMetric(metrics, prefix + "statements.closed", stats.getCloseStatementCount());

			// Second Level Caching
			addMetric(metrics, prefix + "cache.second_level.hits", stats.getSecondLevelCacheHitCount());
			addMetric(metrics, prefix + "cache.second_level.misses", stats.getSecondLevelCacheMissCount());
			addMetric(metrics, prefix + "cache.second_level.puts", stats.getSecondLevelCachePutCount());

			// Entity information
			addMetric(metrics, prefix + "entities.deleted", stats.getEntityDeleteCount());
			addMetric(metrics, prefix + "entities.fetched", stats.getEntityFetchCount());
			addMetric(metrics, prefix + "entities.inserted", stats.getEntityInsertCount());
			addMetric(metrics, prefix + "entities.loaded", stats.getEntityLoadCount());
			addMetric(metrics, prefix + "entities.updated", stats.getOptimisticFailureCount());

			// Collections
			addMetric(metrics, prefix + "collections.removed", stats.getCollectionRemoveCount());
			addMetric(metrics, prefix + "collections.fetched", stats.getCollectionFetchCount());
			addMetric(metrics, prefix + "collections.loaded", stats.getCollectionLoadCount());
			addMetric(metrics, prefix + "collections.recreated", stats.getCollectionRecreateCount());
			addMetric(metrics, prefix + "collections.updated", stats.getCollectionUpdateCount());

			// Natural Id cache
			addMetric(metrics, prefix + "cache.natural_id.hits", stats.getNaturalIdCacheHitCount());
			addMetric(metrics, prefix + "cache.natural_id.misses", stats.getNaturalIdCacheMissCount());
			addMetric(metrics, prefix + "cache.natural_id.puts", stats.getNaturalIdCachePutCount());
			addMetric(metrics, prefix + "query.natural_id.execution.count", stats.getNaturalIdQueryExecutionCount());
			addMetric(metrics, prefix + "query.natural_id.execution.max_time", stats.getNaturalIdQueryExecutionMaxTime());

			// Query stats
			addMetric(metrics, prefix + "query.execution.count", stats.getQueryExecutionCount());
			addMetric(metrics, prefix + "query.execution.max_time", stats.getQueryExecutionMaxTime());

			// Update timestamp cache
			addMetric(metrics, prefix + "cache.update_timestamps.hits", stats.getUpdateTimestampsCacheHitCount());
			addMetric(metrics, prefix + "cache.update_timestamps.misses", stats.getUpdateTimestampsCacheMissCount());
			addMetric(metrics, prefix + "cache.update_timestamps.puts", stats.getUpdateTimestampsCachePutCount());

			// Query Caching
			addMetric(metrics, prefix + "cache.query.hits", stats.getQueryCacheHitCount());
			addMetric(metrics, prefix + "cache.query.misses", stats.getQueryCacheMissCount());
			addMetric(metrics, prefix + "cache.query.puts", stats.getQueryCachePutCount());
		}
		return metrics;
	}

	/**
	 * Attempt to locate the primary {@link EntityManagerFactory} (i.e. either the only one
	 * available or the one amongst the candidates marked as {@link Primary}. Return
	 * {@code null} if no primary could be found.
	 *
	 * @return The detected primary {@code EntityManagerFactory} or {@code null}
	 */
	private EntityManagerFactory getPrimaryEntityManagerFactory() {
		try {
			return this.applicationContext.getBean(EntityManagerFactory.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}
}

