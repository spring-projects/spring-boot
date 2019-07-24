/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.cassandra;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Configuration;
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
 * @since 2.0.0
 * @see EnableReactiveCassandraRepositories
 */
@Configuration
@ConditionalOnClass({ ReactiveSession.class, ReactiveCassandraRepository.class })
@ConditionalOnRepositoryType(store = "cassandra", type = RepositoryType.REACTIVE)
@ConditionalOnMissingBean(ReactiveCassandraRepositoryFactoryBean.class)
@Import(CassandraReactiveRepositoriesAutoConfigureRegistrar.class)
@AutoConfigureAfter(CassandraReactiveDataAutoConfiguration.class)
public class CassandraReactiveRepositoriesAutoConfiguration {

}
