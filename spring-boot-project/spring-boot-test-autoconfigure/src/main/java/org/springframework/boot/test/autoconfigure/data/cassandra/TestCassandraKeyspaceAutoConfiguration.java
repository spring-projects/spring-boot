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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.data.cassandra.core.cql.generator.CreateKeyspaceCqlGenerator;
import org.springframework.data.cassandra.core.cql.generator.DropKeyspaceCqlGenerator;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.DropKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceActionSpecification;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for cassandra keyspace.
 *
 * @author Dmytro Nosan
 * @see AutoConfigureCassandraKeyspace
 */
@Configuration
@AutoConfigureAfter(CassandraAutoConfiguration.class)
@AutoConfigureBefore(CassandraDataAutoConfiguration.class)
@ConditionalOnClass({Cluster.class, Session.class, CqlTemplate.class})
@ConditionalOnBean(Cluster.class)
public class TestCassandraKeyspaceAutoConfiguration {

	private final Cluster cluster;
	private final Environment environment;

	public TestCassandraKeyspaceAutoConfiguration(Cluster cluster, Environment environment) {
		this.cluster = cluster;
		this.environment = environment;
	}


	@Bean(name = "testKeyspaceActionExecutor")
	public KeyspaceActionExecutor keyspaceActionExecutor() {
		KeyspaceAction action = this.environment.getProperty("spring.data.cassandra.test.keyspace-action",
				KeyspaceAction.class);
		String keyspaceName = this.environment.getProperty("spring.data.cassandra.keyspace-name");
		KeyspaceActionFactory factory = new KeyspaceActionFactory(action, keyspaceName);
		return new KeyspaceActionExecutor(this.cluster, factory);
	}


	private static final class KeyspaceActionExecutor implements InitializingBean {

		private final KeyspaceActionFactory factory;
		private final Cluster cluster;

		private KeyspaceActionExecutor(Cluster cluster, KeyspaceActionFactory factory) {
			this.factory = factory;
			this.cluster = cluster;
		}

		@Override
		public void afterPropertiesSet() {
			List<KeyspaceActionSpecification> actions = this.factory.getActions();
			if (!CollectionUtils.isEmpty(actions)) {
				try (Session session = this.cluster.connect()) {
					CqlTemplate cqlTemplate = new CqlTemplate(session);
					for (KeyspaceActionSpecification action : actions) {
						cqlTemplate.execute(toCql(action));
					}
				}
			}
		}

		private String toCql(KeyspaceActionSpecification specification) {

			if (specification instanceof CreateKeyspaceSpecification) {
				return new CreateKeyspaceCqlGenerator((CreateKeyspaceSpecification) specification).toCql();
			}

			if (specification instanceof DropKeyspaceSpecification) {
				return new DropKeyspaceCqlGenerator((DropKeyspaceSpecification) specification).toCql();
			}

			throw new IllegalArgumentException("Unsupported specification type: "
					+ ClassUtils.getQualifiedName(specification.getClass()));
		}
	}


	private static final class KeyspaceActionFactory {

		private final KeyspaceAction action;
		private final String keyspaceName;

		private KeyspaceActionFactory(KeyspaceAction action, String keyspaceName) {
			this.action = action;
			this.keyspaceName = keyspaceName;
		}

		private List<KeyspaceActionSpecification> getActions() {
			if (this.action == null || this.action == KeyspaceAction.NONE) {
				return Collections.emptyList();
			}
			if (!StringUtils.hasText(this.keyspaceName)) {
				return Collections.emptyList();
			}
			switch (this.action) {
				case CREATE:
					return Collections.singletonList(create(false));
				case CREATE_IF_NOT_EXISTS:
					return Collections.singletonList(create(true));
				case RECREATE:
					return Arrays.asList(drop(), create(false));
			}

			return Collections.emptyList();
		}

		private CreateKeyspaceSpecification create(boolean ifNotExists) {
			return CreateKeyspaceSpecification.createKeyspace(this.keyspaceName)
					.ifNotExists(ifNotExists).withSimpleReplication();
		}

		private DropKeyspaceSpecification drop() {
			return DropKeyspaceSpecification.dropKeyspace(this.keyspaceName)
					.ifExists(true);
		}
	}

	/**
	 * Additional configuration to ensure that {@link Session} bean depends on the
	 * {@code keyspaceActionScriptsExecutor} bean.
	 */
	@Configuration
	@ConditionalOnClass({Session.class, CassandraSessionFactoryBean.class})
	protected static class KeyspaceActionExecutorCassandraSessionDependencyConfiguration
			extends AbstractDependsOnBeanFactoryPostProcessor {

		public KeyspaceActionExecutorCassandraSessionDependencyConfiguration() {
			super(Session.class, CassandraSessionFactoryBean.class, "testKeyspaceActionExecutor");
		}

	}

}
