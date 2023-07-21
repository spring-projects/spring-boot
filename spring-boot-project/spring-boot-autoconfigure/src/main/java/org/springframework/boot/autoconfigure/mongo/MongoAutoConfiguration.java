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

package org.springframework.boot.autoconfigure.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mongo.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Phillip Webb
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(MongoClient.class)
@EnableConfigurationProperties(MongoProperties.class)
@ConditionalOnMissingBean(type = "org.springframework.data.mongodb.MongoDatabaseFactory")
public class MongoAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MongoConnectionDetails.class)
	PropertiesMongoConnectionDetails mongoConnectionDetails(MongoProperties properties) {
		return new PropertiesMongoConnectionDetails(properties);
	}

	@Bean
	@ConditionalOnMissingBean(MongoClient.class)
	public MongoClient mongo(ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
			MongoClientSettings settings) {
		return new MongoClientFactory(builderCustomizers.orderedStream().toList()).createMongoClient(settings);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(MongoClientSettings.class)
	static class MongoClientSettingsConfiguration {

		@Bean
		MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().build();
		}

		@Bean
		StandardMongoClientSettingsBuilderCustomizer standardMongoSettingsCustomizer(MongoProperties properties,
				MongoConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
			return new StandardMongoClientSettingsBuilderCustomizer(connectionDetails.getConnectionString(),
					properties.getUuidRepresentation(), properties.getSsl(), sslBundles.getIfAvailable());
		}

	}

}
