/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.mongo;

import java.util.Arrays;
import java.util.List;

import com.mongodb.ClientSessionOptions;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.AnySyncMongoClientAvailable;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's mongo support.
 * <p>
 * Registers a {@link MongoTemplate} and {@link GridFsTemplate} beans if no other beans of
 * the same type are configured.
 * <P>
 * Honors the {@literal spring.data.mongodb.database} property if set, otherwise connects
 * to the {@literal test} database.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Josh Long
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Christoph Strobl
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ MongoClient.class, MongoTemplate.class })
@Conditional(AnySyncMongoClientAvailable.class)
@EnableConfigurationProperties(MongoProperties.class)
@Import(MongoDataConfiguration.class)
@AutoConfigureAfter(MongoAutoConfiguration.class)
public class MongoDataAutoConfiguration {

	private final MongoProperties properties;

	private final MongoDbFactoryFactory dbFactoryFactory;

	public MongoDataAutoConfiguration(ObjectProvider<MongoClient> mongoClientProvider,
			ObjectProvider<com.mongodb.client.MongoClient> mongoClientClientProvider,
			MongoProperties properties) {

		this.properties = properties;
		this.dbFactoryFactory = new MongoDbFactoryFactory(mongoClientProvider,
				mongoClientClientProvider);
	}

	@Bean
	@Conditional(AnySyncMongoClientAvailable.class)
	@ConditionalOnMissingBean(MongoDbFactory.class)
	public MongoDbFactory mongoDbFactory() {
		return this.dbFactoryFactory.getFor(this.properties.getMongoClientDatabase());
	}

	@Bean
	@ConditionalOnMissingBean
	public MongoTemplate mongoTemplate(MongoDbFactory mongoDbFactory,
			MongoConverter converter) {
		return new MongoTemplate(mongoDbFactory, converter);
	}

	@Bean
	@ConditionalOnMissingBean(MongoConverter.class)
	public MappingMongoConverter mappingMongoConverter(MongoDbFactory factory,
			MongoMappingContext context, MongoCustomConversions conversions) {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
		MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver,
				context);
		mappingConverter.setCustomConversions(conversions);
		return mappingConverter;
	}

	@Bean
	@ConditionalOnMissingBean
	public GridFsTemplate gridFsTemplate(MongoDbFactory mongoDbFactory,
			MongoTemplate mongoTemplate) {
		return new GridFsTemplate(
				new GridFsMongoDbFactory(mongoDbFactory, this.properties),
				mongoTemplate.getConverter());
	}

	/**
	 * {@link MongoDbFactory} decorator to respect
	 * {@link MongoProperties#getGridFsDatabase()} if set.
	 */
	private static class GridFsMongoDbFactory implements MongoDbFactory {

		private final MongoDbFactory mongoDbFactory;

		private final MongoProperties properties;

		GridFsMongoDbFactory(MongoDbFactory mongoDbFactory, MongoProperties properties) {
			Assert.notNull(mongoDbFactory, "MongoDbFactory must not be null");
			Assert.notNull(properties, "Properties must not be null");
			this.mongoDbFactory = mongoDbFactory;
			this.properties = properties;
		}

		@Override
		public MongoDatabase getDb() throws DataAccessException {
			String gridFsDatabase = this.properties.getGridFsDatabase();
			if (StringUtils.hasText(gridFsDatabase)) {
				return this.mongoDbFactory.getDb(gridFsDatabase);
			}
			return this.mongoDbFactory.getDb();
		}

		@Override
		public MongoDatabase getDb(String dbName) throws DataAccessException {
			return this.mongoDbFactory.getDb(dbName);
		}

		@Override
		public PersistenceExceptionTranslator getExceptionTranslator() {
			return this.mongoDbFactory.getExceptionTranslator();
		}

		@Override
		public DB getLegacyDb() {
			return this.mongoDbFactory.getLegacyDb();
		}

		@Override
		public ClientSession getSession(ClientSessionOptions options) {
			return this.mongoDbFactory.getSession(options);
		}

		@Override
		public MongoDbFactory withSession(ClientSession session) {
			return this.mongoDbFactory.withSession(session);
		}

	}

	/**
	 * Check if either {@link com.mongodb.MongoClient} or
	 * {@link com.mongodb.client.MongoClient} is already defined in the
	 * {@link org.springframework.context.ApplicationContext}.
	 *
	 * @author Christoph Strobl
	 * @since 2.1
	 */
	static class AnySyncMongoClientAvailable extends AnyNestedCondition {

		AnySyncMongoClientAvailable() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(com.mongodb.MongoClient.class)
		static class MongoClientPreferred {

		}

		@ConditionalOnBean(com.mongodb.client.MongoClient.class)
		static class MongoClientClientPreferred {

		}

	}

	/**
	 * Encapsulation of {@link MongoDbFactory} creation depending on available beans
	 * {@link com.mongodb.MongoClient} or {@link com.mongodb.client.MongoClient} expressed
	 * via the given {@link ObjectProvider ObjectProviders}. Prefers the first available
	 * MongoDB client creating a suitable instance of {@link MongoDbFactory} for it.
	 *
	 * @author Christoph Strobl
	 * @since 2.1
	 */
	static class MongoDbFactoryFactory {

		private final List<ObjectProvider<?>> clientProviders;

		/**
		 * Create new instance of {@link MongoDbFactoryFactory}.
		 * @param clientProviders order matters here, as we choose the first available
		 * one.
		 */
		MongoDbFactoryFactory(ObjectProvider<?>... clientProviders) {
			this.clientProviders = Arrays.asList(clientProviders);
		}

		/**
		 * Get the {@link MongoDbFactory} suitable for the first available MongoDB client.
		 * @param database the name of the default database to return on
		 * {@link MongoDbFactory#getDb()}.
		 * @return new instance of {@link MongoDbFactory} suitable for the first available
		 * MongoDB client.
		 */
		MongoDbFactory getFor(String database) {

			Object client = findAvailableClientProvider();

			if (client instanceof MongoClient) {
				return new SimpleMongoDbFactory(MongoClient.class.cast(client), database);
			}

			if (client instanceof com.mongodb.client.MongoClient) {
				return new SimpleMongoClientDbFactory(
						com.mongodb.client.MongoClient.class.cast(client), database);
			}

			return null;
		}

		private Object findAvailableClientProvider() {

			for (ObjectProvider<?> provider : this.clientProviders) {
				Object client = provider.getIfAvailable();
				if (client != null) {
					return client;
				}
			}

			throw new IllegalStateException(
					"Expected to find at least one MongoDB client.");
		}

	}

}
