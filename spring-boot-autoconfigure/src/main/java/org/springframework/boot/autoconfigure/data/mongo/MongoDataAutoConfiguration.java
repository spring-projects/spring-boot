/*
 * Copyright 2012-2016 the original author or authors.
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

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.Document;
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
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ Mongo.class, MongoTemplate.class })
@EnableConfigurationProperties(MongoProperties.class)
@AutoConfigureAfter(MongoAutoConfiguration.class)
public class MongoDataAutoConfiguration {

	private final ApplicationContext applicationContext;

	private final MongoProperties properties;

	public MongoDataAutoConfiguration(ApplicationContext applicationContext,
			MongoProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(MongoDbFactory.class)
	public SimpleMongoDbFactory mongoDbFactory(MongoClient mongo) throws Exception {
		String database = this.properties.getMongoClientDatabase();
		return new SimpleMongoDbFactory(mongo, database);
	}

	@Bean
	@ConditionalOnMissingBean
	public MongoTemplate mongoTemplate(MongoDbFactory mongoDbFactory,
			MongoConverter converter) throws UnknownHostException {
		return new MongoTemplate(mongoDbFactory, converter);
	}

	@Bean
	@ConditionalOnMissingBean(MongoConverter.class)
	public MappingMongoConverter mappingMongoConverter(MongoDbFactory factory,
			MongoMappingContext context, BeanFactory beanFactory) {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
		MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver,
				context);
		try {
			mappingConverter
					.setCustomConversions(beanFactory.getBean(CustomConversions.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ignore
		}
		return mappingConverter;
	}

	@Bean
	@ConditionalOnMissingBean
	public MongoMappingContext mongoMappingContext(BeanFactory beanFactory)
			throws ClassNotFoundException {
		MongoMappingContext context = new MongoMappingContext();
		context.setInitialEntitySet(new EntityScanner(this.applicationContext)
				.scan(Document.class, Persistent.class));
		Class<?> strategyClass = this.properties.getFieldNamingStrategy();
		if (strategyClass != null) {
			context.setFieldNamingStrategy(
					(FieldNamingStrategy) BeanUtils.instantiate(strategyClass));
		}
		return context;
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
		public DB getDb() throws DataAccessException {
			String gridFsDatabase = this.properties.getGridFsDatabase();
			if (StringUtils.hasText(gridFsDatabase)) {
				return this.mongoDbFactory.getDb(gridFsDatabase);
			}
			return this.mongoDbFactory.getDb();
		}

		@Override
		public DB getDb(String dbName) throws DataAccessException {
			return this.mongoDbFactory.getDb(dbName);
		}

		@Override
		public PersistenceExceptionTranslator getExceptionTranslator() {
			return this.mongoDbFactory.getExceptionTranslator();
		}

	}

}
