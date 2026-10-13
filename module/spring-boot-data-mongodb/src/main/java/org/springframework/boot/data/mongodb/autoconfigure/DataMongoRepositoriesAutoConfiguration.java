/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.autoconfigure;

import com.mongodb.client.MongoClient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
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
 * using the {@link EnableMongoRepositories @EnableMongoRepositories} annotation.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Josh Long
 * @since 4.0.0
 * @see EnableMongoRepositories
 */
@AutoConfiguration(after = DataMongoAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, MongoRepository.class })
@ConditionalOnMissingBean({ MongoRepositoryFactoryBean.class, MongoRepositoryConfigurationExtension.class })
@ConditionalOnRepositoryType(store = "mongodb", type = RepositoryType.IMPERATIVE)
@Import(DataMongoRepositoriesRegistrar.class)
public final class DataMongoRepositoriesAutoConfiguration {

}
