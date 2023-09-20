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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the auto-configuration of web endpoints.
 *
 * @author Andy Wilkinson
 */
class WebEndpointsAutoConfigurationIntegrationTests {

	@Test
	void healthEndpointWebExtensionIsAutoConfigured() {
		servletWebRunner().run((context) -> context.getBean(WebEndpointTestApplication.class));
		servletWebRunner().run((context) -> assertThat(context).hasSingleBean(HealthEndpointWebExtension.class));
	}

	@Test
	@ClassPathExclusions({ "spring-security-oauth2-client-*.jar", "spring-security-oauth2-resource-server-*.jar" })
	void healthEndpointReactiveWebExtensionIsAutoConfigured() {
		reactiveWebRunner()
			.run((context) -> assertThat(context).hasSingleBean(ReactiveHealthEndpointWebExtension.class));
	}

	private WebApplicationContextRunner servletWebRunner() {
		return new WebApplicationContextRunner()
			.withConfiguration(UserConfigurations.of(WebEndpointTestApplication.class))
			.withPropertyValues("management.tracing.enabled=false", "management.defaults.metrics.export.enabled=false");
	}

	private ReactiveWebApplicationContextRunner reactiveWebRunner() {
		return new ReactiveWebApplicationContextRunner()
			.withConfiguration(UserConfigurations.of(WebEndpointTestApplication.class))
			.withPropertyValues("management.tracing.enabled=false", "management.defaults.metrics.export.enabled=false");
	}

	@EnableAutoConfiguration(exclude = { FlywayAutoConfiguration.class, LiquibaseAutoConfiguration.class,
			CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class, Neo4jDataAutoConfiguration.class,
			Neo4jRepositoriesAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
			MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class,
			RepositoryRestMvcAutoConfiguration.class, HazelcastAutoConfiguration.class,
			ElasticsearchDataAutoConfiguration.class, RedisAutoConfiguration.class,
			RedisRepositoriesAutoConfiguration.class, BraveAutoConfiguration.class,
			OpenTelemetryAutoConfiguration.class })
	@SpringBootConfiguration
	static class WebEndpointTestApplication {

	}

}
