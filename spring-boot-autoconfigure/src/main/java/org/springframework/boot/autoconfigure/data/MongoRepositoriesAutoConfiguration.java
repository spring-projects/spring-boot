/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;

import com.mongodb.Mongo;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Mongo
 * Repositories.
 *
 * <p> Activates when there is no bean of type {@link org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean} configured in the context,
 * the Spring Data Mongo {@link org.springframework.data.mongodb.repository.MongoRepository} type is on the classpath,
 * the Mongo client driver API is on the classpath, and there is no other configured {@link org.springframework.data.mongodb.repository.MongoRepository}.
 *
 * <p> Once in effect, the auto-configuration is the equivalent of enabling Mongo repositories using
 * the {@link org.springframework.data.mongodb.repository.config.EnableMongoRepositories} annotation.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Josh Long
 * @see EnableMongoRepositories
 */
@Configuration
@ConditionalOnClass({ Mongo.class, MongoRepository.class })
@ConditionalOnMissingBean(MongoRepositoryFactoryBean.class)
@Import(MongoRepositoriesAutoConfigureRegistrar.class)
public class MongoRepositoriesAutoConfiguration {

}
