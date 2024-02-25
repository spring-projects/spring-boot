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

	/**
	 * Creates a new instance of MongoTemplate if no bean of type MongoOperations is
	 * present.
	 * @param factory the MongoDatabaseFactory to be used for creating the MongoTemplate
	 * @param converter the MongoConverter to be used for converting objects
	 * @return a new instance of MongoTemplate if no bean of type MongoOperations is
	 * present
	 */
	@Bean
	@ConditionalOnMissingBean(MongoOperations.class)
	MongoTemplate mongoTemplate(MongoDatabaseFactory factory, MongoConverter converter) {
		return new MongoTemplate(factory, converter);
	}

	/**
	 * Creates a {@link MappingMongoConverter} bean if no other bean of type
	 * {@link MongoConverter} is present.
	 * @param factory the {@link MongoDatabaseFactory} to be used for creating the
	 * {@link MappingMongoConverter}
	 * @param context the {@link MongoMappingContext} to be used for creating the
	 * {@link MappingMongoConverter}
	 * @param conversions the {@link MongoCustomConversions} to be used for creating the
	 * {@link MappingMongoConverter}
	 * @return the created {@link MappingMongoConverter} bean
	 */
	@Bean
	@ConditionalOnMissingBean(MongoConverter.class)
	MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory factory, MongoMappingContext context,
			MongoCustomConversions conversions) {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
		MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver, context);
		mappingConverter.setCustomConversions(conversions);
		return mappingConverter;
	}

	/**
	 * Creates a new {@link GridFsTemplate} bean if no other bean of type
	 * {@link GridFsOperations} is present.
	 * @param properties the {@link MongoProperties} containing the MongoDB connection
	 * details
	 * @param factory the {@link MongoDatabaseFactory} used to create the MongoDB database
	 * connection
	 * @param mongoTemplate the {@link MongoTemplate} used for MongoDB operations
	 * @param connectionDetails the {@link MongoConnectionDetails} containing additional
	 * MongoDB connection details
	 * @return a new {@link GridFsTemplate} instance
	 */
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

		/**
		 * Constructs a new GridFsMongoDatabaseFactory with the given MongoDatabaseFactory
		 * and MongoConnectionDetails.
		 * @param mongoDatabaseFactory the MongoDatabaseFactory to be used for creating
		 * the underlying MongoDB database
		 * @param connectionDetails the MongoConnectionDetails containing the connection
		 * information for the MongoDB server
		 * @throws IllegalArgumentException if either mongoDatabaseFactory or
		 * connectionDetails is null
		 */
		GridFsMongoDatabaseFactory(MongoDatabaseFactory mongoDatabaseFactory,
				MongoConnectionDetails connectionDetails) {
			Assert.notNull(mongoDatabaseFactory, "MongoDatabaseFactory must not be null");
			Assert.notNull(connectionDetails, "ConnectionDetails must not be null");
			this.mongoDatabaseFactory = mongoDatabaseFactory;
			this.connectionDetails = connectionDetails;
		}

		/**
		 * Retrieves the MongoDatabase instance for the GridFS database.
		 * @return the MongoDatabase instance for the GridFS database
		 * @throws DataAccessException if there is an error accessing the database
		 */
		@Override
		public MongoDatabase getMongoDatabase() throws DataAccessException {
			String gridFsDatabase = getGridFsDatabase(this.connectionDetails);
			if (StringUtils.hasText(gridFsDatabase)) {
				return this.mongoDatabaseFactory.getMongoDatabase(gridFsDatabase);
			}
			return this.mongoDatabaseFactory.getMongoDatabase();
		}

		/**
		 * Retrieves the MongoDatabase instance for the specified database name.
		 * @param dbName the name of the database
		 * @return the MongoDatabase instance
		 * @throws DataAccessException if there is an error accessing the database
		 */
		@Override
		public MongoDatabase getMongoDatabase(String dbName) throws DataAccessException {
			return this.mongoDatabaseFactory.getMongoDatabase(dbName);
		}

		/**
		 * Returns the PersistenceExceptionTranslator for this GridFsMongoDatabaseFactory.
		 * @return the PersistenceExceptionTranslator for this GridFsMongoDatabaseFactory
		 */
		@Override
		public PersistenceExceptionTranslator getExceptionTranslator() {
			return this.mongoDatabaseFactory.getExceptionTranslator();
		}

		/**
		 * Returns a client session for the given options.
		 * @param options the options for the client session
		 * @return a client session
		 */
		@Override
		public ClientSession getSession(ClientSessionOptions options) {
			return this.mongoDatabaseFactory.getSession(options);
		}

		/**
		 * Returns a new MongoDatabaseFactory instance with the specified ClientSession.
		 * @param session the ClientSession to be associated with the MongoDatabaseFactory
		 * @return a new MongoDatabaseFactory instance with the specified ClientSession
		 */
		@Override
		public MongoDatabaseFactory withSession(ClientSession session) {
			return this.mongoDatabaseFactory.withSession(session);
		}

		/**
		 * Returns the name of the GridFS database based on the provided
		 * MongoConnectionDetails.
		 * @param connectionDetails the MongoConnectionDetails object containing the
		 * GridFS information
		 * @return the name of the GridFS database, or null if not specified
		 */
		private String getGridFsDatabase(MongoConnectionDetails connectionDetails) {
			return (connectionDetails.getGridFs() != null) ? connectionDetails.getGridFs().getDatabase() : null;
		}

	}

}
