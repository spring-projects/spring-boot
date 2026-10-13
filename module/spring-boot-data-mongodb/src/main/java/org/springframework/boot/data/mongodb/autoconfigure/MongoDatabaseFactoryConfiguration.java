/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.autoconfigure;

import com.mongodb.client.MongoClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.mongodb.autoconfigure.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoDatabaseFactorySupport;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.util.Assert;

/**
 * Configuration for a {@link MongoDatabaseFactory}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author Scott Frederick
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(MongoDatabaseFactory.class)
@ConditionalOnSingleCandidate(MongoClient.class)
class MongoDatabaseFactoryConfiguration {

	@Bean
	MongoDatabaseFactorySupport<?> mongoDatabaseFactory(MongoClient mongoClient, MongoProperties properties,
			MongoConnectionDetails connectionDetails) {
		String database = properties.getDatabase();
		if (database == null) {
			database = connectionDetails.getConnectionString().getDatabase();
		}
		Assert.hasText(database, "Database name must not be empty");
		return new SimpleMongoClientDatabaseFactory(mongoClient, database);
	}

}
