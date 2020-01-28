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

import com.mongodb.client.MongoClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoDbFactoryConfiguration.AnyMongoClientAvailable;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoDatabaseFactorySupport;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * Configuration for a {@link MongoDatabaseFactory}.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(MongoDatabaseFactory.class)
@Conditional(AnyMongoClientAvailable.class)
class MongoDbFactoryConfiguration {

	@Bean
	MongoDatabaseFactorySupport<?> mongoDbFactory(ObjectProvider<MongoClient> mongoClient, MongoProperties properties) {

		com.mongodb.client.MongoClient fallbackClient = mongoClient.getIfAvailable();
		if (fallbackClient != null) {
			return new SimpleMongoClientDatabaseFactory(fallbackClient, properties.getMongoClientDatabase());
		}
		throw new IllegalStateException("Expected to find at least one MongoDB client.");
	}

	/**
	 * Check if either a {@link MongoClient com.mongodb.MongoClient} or
	 * {@link com.mongodb.client.MongoClient com.mongodb.client.MongoClient} bean is
	 * available.
	 */
	static class AnyMongoClientAvailable extends AnyNestedCondition {

		AnyMongoClientAvailable() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(MongoClient.class)
		static class PreferredClientAvailable {

		}

		@ConditionalOnBean(com.mongodb.client.MongoClient.class)
		static class FallbackClientAvailable {

		}

	}

}
