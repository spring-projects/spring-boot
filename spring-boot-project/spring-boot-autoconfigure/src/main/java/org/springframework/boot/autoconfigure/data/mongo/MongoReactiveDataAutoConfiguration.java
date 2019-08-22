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

import com.mongodb.reactivestreams.client.MongoClient;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's reactive mongo
 * support.
 * <p>
 * Registers a {@link ReactiveMongoTemplate} bean if no other bean of the same type is
 * configured.
 * <P>
 * Honors the {@literal spring.data.mongodb.database} property if set, otherwise connects
 * to the {@literal test} database.
 *
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ MongoClient.class, ReactiveMongoTemplate.class })
@ConditionalOnBean(MongoClient.class)
@EnableConfigurationProperties(MongoProperties.class)
@Import(MongoDataConfiguration.class)
@AutoConfigureAfter(MongoReactiveAutoConfiguration.class)
public class MongoReactiveDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReactiveMongoDatabaseFactory.class)
	public SimpleReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory(MongoProperties properties,
			MongoClient mongo) {
		String database = properties.getMongoClientDatabase();
		return new SimpleReactiveMongoDatabaseFactory(mongo, database);
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			MongoConverter converter) {
		return new ReactiveMongoTemplate(reactiveMongoDatabaseFactory, converter);
	}

	@Bean
	@ConditionalOnMissingBean(MongoConverter.class)
	public MappingMongoConverter mappingMongoConverter(MongoMappingContext context,
			MongoCustomConversions conversions) {
		MappingMongoConverter mappingConverter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
		mappingConverter.setCustomConversions(conversions);
		return mappingConverter;
	}

	@Bean
	@ConditionalOnMissingBean
	public DefaultDataBufferFactory dataBufferFactory() {
		return new DefaultDataBufferFactory();
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveGridFsTemplate reactiveGridFsTemplate(ReactiveMongoDatabaseFactory reactiveMongoDbFactory,
			MappingMongoConverter mappingMongoConverter, DataBufferFactory dataBufferFactory) {
		return new ReactiveGridFsTemplate(dataBufferFactory, reactiveMongoDbFactory, mappingMongoConverter, null);
	}

}
