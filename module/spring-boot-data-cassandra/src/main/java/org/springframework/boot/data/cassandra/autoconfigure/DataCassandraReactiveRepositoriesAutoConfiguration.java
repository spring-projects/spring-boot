/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.cassandra.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.ReactiveSession;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;
import org.springframework.data.cassandra.repository.support.ReactiveCassandraRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Cassandra Reactive
 * Repositories.
 *
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @since 4.0.0
 * @see EnableReactiveCassandraRepositories
 */
@AutoConfiguration(after = DataCassandraReactiveAutoConfiguration.class)
@ConditionalOnClass({ ReactiveSession.class, ReactiveCassandraRepository.class })
@ConditionalOnRepositoryType(store = "cassandra", type = RepositoryType.REACTIVE)
@ConditionalOnMissingBean(ReactiveCassandraRepositoryFactoryBean.class)
@Import(DataCassandraReactiveRepositoriesRegistrar.class)
public final class DataCassandraReactiveRepositoriesAutoConfiguration {

}
