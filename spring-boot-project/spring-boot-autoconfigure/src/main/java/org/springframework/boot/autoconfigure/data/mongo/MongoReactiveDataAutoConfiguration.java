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

import java.util.Optional;

import com.mongodb.ClientSessionOptions;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails.GridFs;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsOperations;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's reactive mongo
 * support.
 * <p>
 * Registers a {@link ReactiveMongoTemplate} bean if no other bean of the same type is
 * configured.
 *
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.0.0
 */
@AutoConfiguration(after = MongoReactiveAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, ReactiveMongoTemplate.class })
@ConditionalOnBean(MongoClient.class)
@EnableConfigurationProperties(MongoProperties.class)
@Import(MongoDataConfiguration.class)
public class MongoReactiveDataAutoConfiguration {

	private final MongoConnectionDetails connectionDetails;

	/**
	 * Constructs a new MongoReactiveDataAutoConfiguration with the specified connection
	 * details.
	 * @param connectionDetails the connection details for the MongoDB database
	 */
	MongoReactiveDataAutoConfiguration(MongoConnectionDetails connectionDetails) {
		this.connectionDetails = connectionDetails;
	}

	/**
	 * Creates a {@link SimpleReactiveMongoDatabaseFactory} bean if no other bean of type
	 * {@link ReactiveMongoDatabaseFactory} is present.
	 * @param mongo the {@link MongoClient} instance to use for creating the database
	 * factory
	 * @param properties the {@link MongoProperties} instance containing the MongoDB
	 * connection details
	 * @return the created {@link SimpleReactiveMongoDatabaseFactory} bean
	 */
	@Bean
	@ConditionalOnMissingBean(ReactiveMongoDatabaseFactory.class)
	public SimpleReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory(MongoClient mongo,
			MongoProperties properties) {
		String database = properties.getDatabase();
		if (database == null) {
			database = this.connectionDetails.getConnectionString().getDatabase();
		}
		return new SimpleReactiveMongoDatabaseFactory(mongo, database);
	}

	/**
	 * Creates a new instance of ReactiveMongoTemplate if no other bean of type
	 * ReactiveMongoOperations is present.
	 * @param reactiveMongoDatabaseFactory the ReactiveMongoDatabaseFactory used to create
	 * the ReactiveMongoTemplate
	 * @param converter the MongoConverter used to convert between Java objects and
	 * MongoDB documents
	 * @return a new instance of ReactiveMongoTemplate if no other bean of type
	 * ReactiveMongoOperations is present
	 */
	@Bean
	@ConditionalOnMissingBean(ReactiveMongoOperations.class)
	public ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			MongoConverter converter) {
		return new ReactiveMongoTemplate(reactiveMongoDatabaseFactory, converter);
	}

	/**
	 * Creates a {@link MappingMongoConverter} bean if no other bean of type
	 * {@link MongoConverter} is present.
	 * @param context the {@link MongoMappingContext} to use for the converter
	 * @param conversions the {@link MongoCustomConversions} to use for the converter
	 * @return the created {@link MappingMongoConverter} bean
	 */
	@Bean
	@ConditionalOnMissingBean(MongoConverter.class)
	public MappingMongoConverter mappingMongoConverter(MongoMappingContext context,
			MongoCustomConversions conversions) {
		MappingMongoConverter mappingConverter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
		mappingConverter.setCustomConversions(conversions);
		return mappingConverter;
	}

	/**
	 * Creates a new instance of {@link DefaultDataBufferFactory} if no other bean of type
	 * {@link DataBufferFactory} is present.
	 * @return the {@link DefaultDataBufferFactory} instance
	 */
	@Bean
	@ConditionalOnMissingBean(DataBufferFactory.class)
	public DefaultDataBufferFactory dataBufferFactory() {
		return new DefaultDataBufferFactory();
	}

	/**
	 * Creates a new {@link ReactiveGridFsTemplate} bean if there is no existing bean of
	 * type {@link ReactiveGridFsOperations}.
	 * @param reactiveMongoDatabaseFactory the {@link ReactiveMongoDatabaseFactory} used
	 * to create the {@link ReactiveGridFsTemplate}
	 * @param mappingMongoConverter the {@link MappingMongoConverter} used to convert data
	 * @param dataBufferFactory the {@link DataBufferFactory} used to create data buffers
	 * @return the created {@link ReactiveGridFsTemplate} bean
	 */
	@Bean
	@ConditionalOnMissingBean(ReactiveGridFsOperations.class)
	public ReactiveGridFsTemplate reactiveGridFsTemplate(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			MappingMongoConverter mappingMongoConverter, DataBufferFactory dataBufferFactory) {
		return new ReactiveGridFsTemplate(dataBufferFactory,
				new GridFsReactiveMongoDatabaseFactory(reactiveMongoDatabaseFactory, this.connectionDetails),
				mappingMongoConverter,
				(this.connectionDetails.getGridFs() != null) ? this.connectionDetails.getGridFs().getBucket() : null);
	}

	/**
	 * {@link ReactiveMongoDatabaseFactory} decorator to use {@link GridFs#getGridFs()}
	 * from the {@link MongoConnectionDetails} when set.
	 */
	static class GridFsReactiveMongoDatabaseFactory implements ReactiveMongoDatabaseFactory {

		private final ReactiveMongoDatabaseFactory delegate;

		private final MongoConnectionDetails connectionDetails;

		/**
		 * Constructs a new GridFsReactiveMongoDatabaseFactory with the specified delegate
		 * and connection details.
		 * @param delegate the ReactiveMongoDatabaseFactory delegate to be used
		 * @param connectionDetails the MongoConnectionDetails containing the connection
		 * details
		 */
		GridFsReactiveMongoDatabaseFactory(ReactiveMongoDatabaseFactory delegate,
				MongoConnectionDetails connectionDetails) {
			this.delegate = delegate;
			this.connectionDetails = connectionDetails;
		}

		/**
		 * Checks if the factory has a codec for the specified type.
		 * @param type the type to check for a codec
		 * @return {@code true} if the factory has a codec for the specified type,
		 * {@code false} otherwise
		 */
		@Override
		public boolean hasCodecFor(Class<?> type) {
			return this.delegate.hasCodecFor(type);
		}

		/**
		 * Retrieves the MongoDatabase instance for the GridFS database.
		 * @return a Mono containing the MongoDatabase instance
		 * @throws DataAccessException if an error occurs while retrieving the
		 * MongoDatabase
		 */
		@Override
		public Mono<MongoDatabase> getMongoDatabase() throws DataAccessException {
			String gridFsDatabase = getGridFsDatabase(this.connectionDetails);
			if (StringUtils.hasText(gridFsDatabase)) {
				return this.delegate.getMongoDatabase(gridFsDatabase);
			}
			return this.delegate.getMongoDatabase();
		}

		/**
		 * Returns the database name for GridFS based on the provided
		 * MongoConnectionDetails.
		 * @param connectionDetails the MongoConnectionDetails object containing the
		 * GridFS details
		 * @return the database name for GridFS, or null if GridFS is not configured
		 */
		private String getGridFsDatabase(MongoConnectionDetails connectionDetails) {
			return (connectionDetails.getGridFs() != null) ? connectionDetails.getGridFs().getDatabase() : null;
		}

		/**
		 * Retrieves the specified MongoDB database.
		 * @param dbName the name of the database to retrieve
		 * @return a Mono emitting the requested MongoDatabase
		 * @throws DataAccessException if an error occurs while retrieving the database
		 */
		@Override
		public Mono<MongoDatabase> getMongoDatabase(String dbName) throws DataAccessException {
			return this.delegate.getMongoDatabase(dbName);
		}

		/**
		 * Retrieves the codec for the specified type from the delegate GridFS database
		 * factory.
		 * @param type the class representing the type for which the codec is requested
		 * @param <T> the type of the codec
		 * @return an Optional containing the codec for the specified type, or an empty
		 * Optional if no codec is found
		 */
		@Override
		public <T> Optional<Codec<T>> getCodecFor(Class<T> type) {
			return this.delegate.getCodecFor(type);
		}

		/**
		 * Returns the PersistenceExceptionTranslator used by this
		 * GridFsReactiveMongoDatabaseFactory.
		 * @return the PersistenceExceptionTranslator used by this
		 * GridFsReactiveMongoDatabaseFactory
		 */
		@Override
		public PersistenceExceptionTranslator getExceptionTranslator() {
			return this.delegate.getExceptionTranslator();
		}

		/**
		 * Returns the codec registry used by this GridFsReactiveMongoDatabaseFactory.
		 * @return the codec registry used by this GridFsReactiveMongoDatabaseFactory
		 */
		@Override
		public CodecRegistry getCodecRegistry() {
			return this.delegate.getCodecRegistry();
		}

		/**
		 * Retrieves a reactive MongoDB session with the given options.
		 * @param options the options for the session
		 * @return a Mono emitting the reactive MongoDB session
		 */
		@Override
		public Mono<ClientSession> getSession(ClientSessionOptions options) {
			return this.delegate.getSession(options);
		}

		/**
		 * Returns a new ReactiveMongoDatabaseFactory instance with the specified
		 * ClientSession.
		 * @param session the ClientSession to be associated with the
		 * ReactiveMongoDatabaseFactory
		 * @return a new ReactiveMongoDatabaseFactory instance with the specified
		 * ClientSession
		 */
		@Override
		public ReactiveMongoDatabaseFactory withSession(ClientSession session) {
			return this.delegate.withSession(session);
		}

		/**
		 * Returns a boolean value indicating whether a transaction is currently active.
		 * @return {@code true} if a transaction is active, {@code false} otherwise
		 */
		@Override
		public boolean isTransactionActive() {
			return this.delegate.isTransactionActive();
		}

	}

}
