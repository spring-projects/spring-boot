/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.couchbase;

import com.couchbase.client.java.Cluster;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Import;
import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;
import org.springframework.data.couchbase.repository.config.ReactiveRepositoryOperationsMapping;
import org.springframework.data.couchbase.repository.support.ReactiveCouchbaseRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Couchbase Reactive
 * Repositories.
 *
 * @author Alex Derkach
 * @since 2.0.0
 */
@AutoConfiguration(after = CouchbaseReactiveDataAutoConfiguration.class)
@ConditionalOnClass({ Cluster.class, ReactiveCouchbaseRepository.class, Flux.class })
@ConditionalOnRepositoryType(store = "couchbase", type = RepositoryType.REACTIVE)
@ConditionalOnBean(ReactiveRepositoryOperationsMapping.class)
@ConditionalOnMissingBean(ReactiveCouchbaseRepositoryFactoryBean.class)
@Import(CouchbaseReactiveRepositoriesRegistrar.class)
public class CouchbaseReactiveRepositoriesAutoConfiguration {

}
