package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Initializer;

/**
 * Cassandra Cluster Factory Interface
 *
 * This interface allows the default cassandra cluster builder to be overwritten
 *
 * @auther Steffen F. Qvistgaard
 * @since 2.2.0
 */
public interface CassandraClusterFactory {
	Cluster build(Initializer initializer);
}
