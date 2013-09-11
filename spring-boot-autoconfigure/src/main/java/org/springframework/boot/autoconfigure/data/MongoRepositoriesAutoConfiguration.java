/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.data;

import java.net.UnknownHostException;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.DBPort;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Mongo
 * Repositories.
 * 
 * @author Dave Syer
 * @see EnableMongoRepositories
 */
@Configuration
@ConditionalOnClass({ Mongo.class, MongoRepository.class })
public class MongoRepositoriesAutoConfiguration {

	@Import(MongoRepositoriesAutoConfigureRegistrar.class)
	@Configuration
	@EnableConfigurationProperties(MongoProperties.class)
	protected static class MongoRepositoriesConfiguration {

		@Autowired
		private MongoProperties config;

		@PreDestroy
		public void close() throws UnknownHostException {
			mongo().close();
		}

		@Bean
		@ConditionalOnMissingBean(Mongo.class)
		Mongo mongo() throws UnknownHostException {
			return this.config.mongo();
		}

		@Bean
		@ConditionalOnMissingBean(MongoTemplate.class)
		MongoTemplate mongoTemplate(Mongo mongo) throws UnknownHostException {
			return new MongoTemplate(mongo, this.config.database());
		}

	}

	@ConfigurationProperties(name = "spring.data.mongo")
	public static class MongoProperties {

		private String host;

		private int port = DBPort.PORT;

		private String uri = "mongodb://localhost/test";

		private String database;

		public String getHost() {
			return this.host;
		}

		public String database() {
			return this.database == null ? new MongoURI(this.uri).getDatabase()
					: this.database;
		}

		public Mongo mongo() throws UnknownHostException {
			return this.host != null ? new Mongo(this.host, this.port) : new Mongo(
					new MongoURI(this.uri));
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getDatabase() {
			return this.database;
		}

		public void setDatabase(String database) {
			this.database = database;
		}

		public String getUri() {
			return this.uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}

	}

}
