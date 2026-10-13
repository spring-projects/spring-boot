/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactoryBean;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data R2DBC Repositories.
 *
 * @author Mark Paluch
 * @since 4.0.0
 * @see EnableR2dbcRepositories
 */
@AutoConfiguration(after = DataR2dbcAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, R2dbcRepository.class })
@ConditionalOnBean(DatabaseClient.class)
@ConditionalOnBooleanProperty(name = "spring.data.r2dbc.repositories.enabled", matchIfMissing = true)
@ConditionalOnMissingBean(R2dbcRepositoryFactoryBean.class)
@Import(DataR2dbcRepositoriesAutoConfigureRegistrar.class)
public final class DataR2dbcRepositoriesAutoConfiguration {

}
