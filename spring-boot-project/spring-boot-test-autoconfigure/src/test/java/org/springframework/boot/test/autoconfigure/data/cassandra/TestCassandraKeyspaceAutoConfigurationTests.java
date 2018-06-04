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

package org.springframework.boot.test.autoconfigure.data.cassandra;

import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TestCassandraKeyspaceAutoConfiguration}.
 *
 * @author Dmytro Nosan
 */
public class TestCassandraKeyspaceAutoConfigurationTests {


	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestCassandraKeyspaceAutoConfiguration.class));


	@Test
	public void createIfNotExists() {
		this.contextRunner
				.withPropertyValues("spring.data.cassandra.test.keyspace-action=CREATE_IF_NOT_EXISTS",
						"spring.data.cassandra.keyspace-name=test")
				.withUserConfiguration(MockClusterConfiguration.class)
				.run(context -> {
					Cluster cluster = context.getBean(Cluster.class);
					Session session = cluster.connect();
					ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
					verify(session).execute(captor.capture());
					assertThat(captor.getValue().getQueryString()).startsWith("CREATE KEYSPACE IF NOT EXISTS test");
				});
	}


	@Test
	public void create() {
		this.contextRunner
				.withPropertyValues("spring.data.cassandra.test.keyspace-action=CREATE",
						"spring.data.cassandra.keyspace-name=test")
				.withUserConfiguration(MockClusterConfiguration.class)
				.run(context -> {
					Cluster cluster = context.getBean(Cluster.class);
					Session session = cluster.connect();
					ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
					verify(session).execute(captor.capture());
					assertThat(captor.getValue().getQueryString()).startsWith("CREATE KEYSPACE test");
				});
	}

	@Test
	public void none() {
		this.contextRunner
				.withPropertyValues("spring.data.cassandra.test.keyspace-action=NONE",
						"spring.data.cassandra.keyspace-name=test")
				.withUserConfiguration(MockClusterConfiguration.class)
				.run(context -> {
					Cluster cluster = context.getBean(Cluster.class);
					Session session = cluster.connect();
					verify(session, times(0)).execute(any(SimpleStatement.class));
				});
	}

	@Test
	public void recreate() {
		this.contextRunner
				.withPropertyValues("spring.data.cassandra.test.keyspace-action=RECREATE",
						"spring.data.cassandra.keyspace-name=test")
				.withUserConfiguration(MockClusterConfiguration.class)
				.run(context -> {
					Cluster cluster = context.getBean(Cluster.class);
					Session session = cluster.connect();
					ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
					verify(session, times(2)).execute(captor.capture());
					List<SimpleStatement> values = captor.getAllValues();
					assertThat(values.get(0).getQueryString()).startsWith("DROP KEYSPACE IF EXISTS test");
					assertThat(values.get(1).getQueryString()).startsWith("CREATE KEYSPACE test");
				});
	}


	@Configuration
	static class MockClusterConfiguration {

		@Bean
		public Cluster cluster() {
			Cluster cluster = Mockito.mock(Cluster.class);
			Session session = Mockito.mock(Session.class);
			ResultSet resultSet = mock(ResultSet.class);
			doReturn(true).when(resultSet).wasApplied();
			doReturn(resultSet).when(session).execute(any(SimpleStatement.class));
			doReturn(session).when(cluster).connect();
			return cluster;
		}

	}
}
