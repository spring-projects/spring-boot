package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Initializer;

/**
 * Default Cassandra Cluster Factory Implementation
 *
 * @auther Steffen F. Qvistgaard
 * @since 2.2.0
 */
public class CassandraClusterFactoryImpl implements CassandraClusterFactory {

	@Override
	public Cluster build(final Initializer initializer) {
		return Cluster.buildFrom(initializer);
	}
}
