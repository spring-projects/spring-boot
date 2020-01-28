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

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configuration for Mongo-related beans that depend on a {@link MongoDatabaseFactory}.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(MongoDatabaseFactory.class)
class MongoDbFactoryDependentConfiguration {

	private final MongoProperties properties;

	MongoDbFactoryDependentConfiguration(MongoProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(MongoOperations.class)
	MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDbFactory, MongoConverter converter) {
		return new MongoTemplate(mongoDbFactory, converter);
	}

	@Bean
	@ConditionalOnMissingBean(MongoConverter.class)
	MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory factory, MongoMappingContext context,
			MongoCustomConversions conversions) {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
		MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver, context);
		mappingConverter.setCustomConversions(conversions);
		return mappingConverter;
	}

	@Bean
	@ConditionalOnMissingBean(GridFsOperations.class)
	GridFsTemplate gridFsTemplate(MongoDatabaseFactory mongoDbFactory, MongoTemplate mongoTemplate) {
		return new GridFsTemplate(new GridFsMongoDbFactory(mongoDbFactory, this.properties),
				mongoTemplate.getConverter());
	}

	/**
	 * {@link MongoDatabaseFactory} decorator to respect
	 * {@link MongoProperties#getGridFsDatabase()} if set.
	 */
	static class GridFsMongoDbFactory implements MongoDatabaseFactory {

		private final MongoDatabaseFactory mongoDbFactory;

		private final MongoProperties properties;

		GridFsMongoDbFactory(MongoDatabaseFactory mongoDbFactory, MongoProperties properties) {
			Assert.notNull(mongoDbFactory, "MongoDbFactory must not be null");
			Assert.notNull(properties, "Properties must not be null");
			this.mongoDbFactory = mongoDbFactory;
			this.properties = properties;
		}

		@Override
		public MongoDatabase getMongoDatabase() throws DataAccessException {
			String gridFsDatabase = this.properties.getGridFsDatabase();
			if (StringUtils.hasText(gridFsDatabase)) {
				return this.mongoDbFactory.getMongoDatabase(gridFsDatabase);
			}
			return this.mongoDbFactory.getMongoDatabase();
		}

		@Override
		public MongoDatabase getMongoDatabase(String dbName) throws DataAccessException {
			return this.mongoDbFactory.getMongoDatabase(dbName);
		}

		@Override
		public PersistenceExceptionTranslator getExceptionTranslator() {
			return this.mongoDbFactory.getExceptionTranslator();
		}

		@Override
		public ClientSession getSession(ClientSessionOptions options) {
			return this.mongoDbFactory.getSession(options);
		}

		@Override
		public MongoDatabaseFactory withSession(ClientSession session) {
			return this.mongoDbFactory.withSession(session);
		}

	}

}
