/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.web.client;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.data.solr.SolrRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.jest.JestAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RestTemplateMetricsConfigurationTests.ClientApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RestTemplateMetricsConfigurationTests {
	@Autowired
	private RestTemplate client;

	@Autowired
	private MeterRegistry registry;

	@Test
	public void restTemplatesCreatedWithBuilderAreInstrumented() {
		try {
			this.client.getForObject("http://google.com", String.class);
		}
		catch (Throwable ignored) {
			// doesn't matter whether the request succeeded or not
		}
		assertThat(this.registry.find("http.client.requests").meter()).isPresent();
	}

	@SpringBootApplication(exclude = {
			FlywayAutoConfiguration.class, LiquibaseAutoConfiguration.class,
			CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class,
			Neo4jDataAutoConfiguration.class, Neo4jRepositoriesAutoConfiguration.class,
			MongoAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class,
			HazelcastAutoConfiguration.class, MongoDataAutoConfiguration.class,
			ElasticsearchAutoConfiguration.class, ElasticsearchDataAutoConfiguration.class,
			JestAutoConfiguration.class, SolrRepositoriesAutoConfiguration.class,
			SolrAutoConfiguration.class, RedisAutoConfiguration.class,
			RedisRepositoriesAutoConfiguration.class
	})
	static class ClientApp {
		@Bean
		public MeterRegistry registry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		public RestTemplate restTemplate(RestTemplateBuilder builder) {
			return builder.build();
		}
	}

}
