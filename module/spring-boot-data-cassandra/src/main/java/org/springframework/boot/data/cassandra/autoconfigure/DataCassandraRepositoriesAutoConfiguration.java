/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.cassandra.autoconfigure;

import com.datastax.oss.driver.api.core.CqlSession;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.cassandra.repository.support.CassandraRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Cassandra
 * Repositories.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 * @see EnableCassandraRepositories
 */
@AutoConfiguration
@ConditionalOnClass({ CqlSession.class, CassandraRepository.class })
@ConditionalOnRepositoryType(store = "cassandra", type = RepositoryType.IMPERATIVE)
@ConditionalOnMissingBean(CassandraRepositoryFactoryBean.class)
@Import(DataCassandraRepositoriesRegistrar.class)
public final class DataCassandraRepositoriesAutoConfiguration {

}
