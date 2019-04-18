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

package org.springframework.boot.autoconfigure.data.mongo;

import com.mongodb.MongoClient;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.repository.config.MongoRepositoryConfigurationExtension;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Mongo
 * Repositories.
 * <p>
 * Activates when there is no bean of type
 * {@link org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean}
 * configured in the context, the Spring Data Mongo
 * {@link org.springframework.data.mongodb.repository.MongoRepository} type is on the
 * classpath, the Mongo client driver API is on the classpath, and there is no other
 * configured {@link org.springframework.data.mongodb.repository.MongoRepository}.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling Mongo repositories
 * using the
 * {@link org.springframework.data.mongodb.repository.config.EnableMongoRepositories}
 * annotation.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Josh Long
 * @see EnableMongoRepositories
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ MongoClient.class, MongoRepository.class })
@ConditionalOnMissingBean({ MongoRepositoryFactoryBean.class,
		MongoRepositoryConfigurationExtension.class })
@ConditionalOnRepositoryType(store = "mongodb", type = RepositoryType.IMPERATIVE)
@Import(MongoRepositoriesAutoConfigureRegistrar.class)
@AutoConfigureAfter(MongoDataAutoConfiguration.class)
public class MongoRepositoriesAutoConfiguration {

}
