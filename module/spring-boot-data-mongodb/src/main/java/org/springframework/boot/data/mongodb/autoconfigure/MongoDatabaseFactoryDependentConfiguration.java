/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.autoconfigure;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoProperties.Gridfs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
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
	@ConditionalOnMissingBean(GridFsOperations.class)
	GridFsTemplate gridFsTemplate(DataMongoProperties properties, MongoDatabaseFactory factory,
			MongoTemplate mongoTemplate) {
		return new GridFsTemplate(new GridFsMongoDatabaseFactory(factory, properties), mongoTemplate.getConverter(),
				properties.getGridfs().getBucket());
	}

	/**
	 * {@link MongoDatabaseFactory} decorator to respect {@link Gridfs#getDatabase()} if
	 * set.
	 */
	static class GridFsMongoDatabaseFactory implements MongoDatabaseFactory {

		private final MongoDatabaseFactory mongoDatabaseFactory;

		private final DataMongoProperties properties;

		GridFsMongoDatabaseFactory(MongoDatabaseFactory mongoDatabaseFactory, DataMongoProperties properties) {
			Assert.notNull(mongoDatabaseFactory, "'mongoDatabaseFactory' must not be null");
			Assert.notNull(properties, "'properties' must not be null");
			this.mongoDatabaseFactory = mongoDatabaseFactory;
			this.properties = properties;
		}

		@Override
		public MongoDatabase getMongoDatabase() throws DataAccessException {
			String gridFsDatabase = getGridFsDatabase();
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

		private @Nullable String getGridFsDatabase() {
			return this.properties.getGridfs().getDatabase();
		}

	}

}
