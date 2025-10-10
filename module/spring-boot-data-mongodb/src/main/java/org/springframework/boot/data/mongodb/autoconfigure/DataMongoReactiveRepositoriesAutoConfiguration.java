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

package org.springframework.boot.data.mongodb.autoconfigure;

import com.mongodb.reactivestreams.client.MongoClient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.data.mongodb.repository.config.ReactiveMongoRepositoryConfigurationExtension;
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Mongo Reactive
 * Repositories.
 * <p>
 * Activates when there is no bean of type
 * {@link org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactoryBean}
 * configured in the context, the Spring Data Mongo {@link ReactiveMongoRepository} type
 * is on the classpath, the ReactiveStreams Mongo client driver API is on the classpath,
 * and there is no other configured {@link ReactiveMongoRepository}.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling Mongo repositories
 * using the {@link EnableReactiveMongoRepositories @EnableReactiveMongoRepositories}
 * annotation.
 *
 * @author Mark Paluch
 * @since 4.0.0
 * @see EnableReactiveMongoRepositories
 */
@AutoConfiguration(after = DataMongoReactiveAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, ReactiveMongoRepository.class })
@ConditionalOnMissingBean({ ReactiveMongoRepositoryFactoryBean.class,
		ReactiveMongoRepositoryConfigurationExtension.class })
@ConditionalOnRepositoryType(store = "mongodb", type = RepositoryType.REACTIVE)
@Import(DataMongoReactiveRepositoriesRegistrar.class)
public final class DataMongoReactiveRepositoriesAutoConfiguration {

}
