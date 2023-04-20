/*
 * Copyright 2012-2023 the original author or authors.
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
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails.GridFs;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.MongoProperties.Gridfs;
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
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(MongoDatabaseFactory.class)
class MongoDatabaseFactoryDependentConfiguration {

	@Bean
	@ConditionalOnMissingBean(MongoOperations.class)
	MongoTemplate mongoTemplate(MongoDatabaseFactory factory, MongoConverter converter) {
		return new MongoTemplate(factory, converter);
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
	GridFsTemplate gridFsTemplate(MongoProperties properties, MongoDatabaseFactory factory, MongoTemplate mongoTemplate,
			MongoConnectionDetails connectionDetails) {
		return new GridFsTemplate(new GridFsMongoDatabaseFactory(factory, connectionDetails),
				mongoTemplate.getConverter(),
				(connectionDetails.getGridFs() != null) ? connectionDetails.getGridFs().getBucket() : null);
	}

	/**
	 * {@link MongoDatabaseFactory} decorator to respect {@link Gridfs#getDatabase()} or
	 * {@link GridFs#getGridFs()} from the {@link MongoConnectionDetails} if set.
	 */
	static class GridFsMongoDatabaseFactory implements MongoDatabaseFactory {

		private final MongoDatabaseFactory mongoDatabaseFactory;

		private final MongoConnectionDetails connectionDetails;

		GridFsMongoDatabaseFactory(MongoDatabaseFactory mongoDatabaseFactory,
				MongoConnectionDetails connectionDetails) {
			Assert.notNull(mongoDatabaseFactory, "MongoDatabaseFactory must not be null");
			Assert.notNull(connectionDetails, "ConnectionDetails must not be null");
			this.mongoDatabaseFactory = mongoDatabaseFactory;
			this.connectionDetails = connectionDetails;
		}

		@Override
		public MongoDatabase getMongoDatabase() throws DataAccessException {
			String gridFsDatabase = getGridFsDatabase(this.connectionDetails);
			if (StringUtils.hasText(gridFsDatabase)) {
				return this.mongoDatabaseFactory.getMongoDatabase(gridFsDatabase);
			}
			return this.mongoDatabaseFactory.getMongoDatabase();
		}

		@Override
		public MongoDatabase getMongoDatabase(String dbName) throws DataAccessException {
			return this.mongoDatabaseFactory.getMongoDatabase(dbName);
		}

		@Override
		public PersistenceExceptionTranslator getExceptionTranslator() {
			return this.mongoDatabaseFactory.getExceptionTranslator();
		}

		@Override
		public ClientSession getSession(ClientSessionOptions options) {
			return this.mongoDatabaseFactory.getSession(options);
		}

		@Override
		public MongoDatabaseFactory withSession(ClientSession session) {
			return this.mongoDatabaseFactory.withSession(session);
		}

		private String getGridFsDatabase(MongoConnectionDetails connectionDetails) {
			return (connectionDetails.getGridFs() != null) ? connectionDetails.getGridFs().getDatabase() : null;
		}

	}

}
