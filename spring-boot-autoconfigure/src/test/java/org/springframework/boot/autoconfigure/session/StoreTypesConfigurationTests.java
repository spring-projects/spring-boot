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

package org.springframework.boot.autoconfigure.session;

import java.net.UnknownHostException;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import com.mongodb.MongoClient;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.data.mongo.MongoOperationsSessionRepository;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

public class StoreTypesConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void hashMapSessionStore() {
		load("spring.session.store.type=hash-map");
		MapSessionRepository sessionRepository = this.context.getBean(MapSessionRepository.class);
		assertThat(sessionRepository).isNotNull();
	}

	@Test
	public void jdbcSessionStore() {
		load(new String[] {"spring.session.store.type=jdbc"}, EmbeddedDataSourceConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class);
		assertThat(this.context.getBean(JdbcOperationsSessionRepository.class)).isNotNull();
	}

	@Test
	public void hazelcastSessionStore() {
		load(new String[] {"spring.session.store.type=hazelcast"}, MockHazelcastInstanceConfiguration.class);
		assertThat(this.context.getBean(MapSessionRepository.class)).isNotNull();
	}

	@Test
	public void mongoSessionStore() {
		load(new String[] {"spring.session.store.type=mongo", "spring.data.mongodb.port=0"}, MockMongoConfiguration.class, MongoDataAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class);
		assertThat(this.context.getBean(MongoOperationsSessionRepository.class)).isNotNull();
	}

	private void load(String storeType) {
		load(new String[] {storeType}, null);
	}

	private void load(String[] storeType, Class<?>... config) {
		this.context = new AnnotationConfigWebApplicationContext();
		for (String property : storeType) {
			EnvironmentTestUtils.addEnvironment(this.context, storeType);
		}
		if (config != null) {
			this.context.register(config);
		}
		this.context.register(ServerPropertiesAutoConfiguration.class,
				SessionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	static class MockHazelcastInstanceConfiguration {

		@Bean
		public HazelcastInstance hazelcastInstance() {
			return Hazelcast.newHazelcastInstance();
		}

	}

	@Configuration
	static class MockMongoConfiguration {

		@Bean
		public MongoClient mongoClient(@Value("${local.mongo.port}") int port)
				throws UnknownHostException {
			return new MongoClient("localhost", port);
		}

	}

}
