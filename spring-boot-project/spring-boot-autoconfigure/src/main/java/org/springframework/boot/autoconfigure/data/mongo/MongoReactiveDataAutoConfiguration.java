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

import com.mongodb.reactivestreams.client.MongoClient;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;

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
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ MongoClient.class, ReactiveMongoTemplate.class })
@EnableConfigurationProperties(MongoProperties.class)
@AutoConfigureAfter({ MongoReactiveAutoConfiguration.class,
		MongoDataAutoConfiguration.class })
public class MongoReactiveDataAutoConfiguration {

	private final MongoProperties properties;

	public MongoReactiveDataAutoConfiguration(MongoProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(ReactiveMongoDatabaseFactory.class)
	public SimpleReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory(
			MongoClient mongo) {
		String database = this.properties.getMongoClientDatabase();
		return new SimpleReactiveMongoDatabaseFactory(mongo, database);
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveMongoTemplate reactiveMongoTemplate(
			ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			MongoConverter converter) {
		return new ReactiveMongoTemplate(reactiveMongoDatabaseFactory, converter);
	}

}
