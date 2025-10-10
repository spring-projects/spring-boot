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
