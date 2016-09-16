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

package org.springframework.boot.autoconfigure.data.neo4j;

import java.util.List;

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.template.Neo4jOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Neo4j.
 *
 * @author Michael Hunger
 * @author Josh Long
 * @author Vince Bickers
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass({ Neo4jSession.class, Neo4jOperations.class })
@ConditionalOnMissingBean(Neo4jOperations.class)
@EnableConfigurationProperties(Neo4jProperties.class)
public class Neo4jDataAutoConfiguration {

	private final Neo4jProperties properties;

	public Neo4jDataAutoConfiguration(Neo4jProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public org.neo4j.ogm.config.Configuration configuration() {
		return this.properties.createConfiguration();
	}

	@Configuration
	static class SpringBootNeo4jConfiguration extends Neo4jConfiguration {

		private final ApplicationContext applicationContext;

		private final org.neo4j.ogm.config.Configuration configuration;

		SpringBootNeo4jConfiguration(ApplicationContext applicationContext,
				org.neo4j.ogm.config.Configuration configuration) {
			this.applicationContext = applicationContext;
			this.configuration = configuration;
		}

		@Override
		public SessionFactory getSessionFactory() {
			return new SessionFactory(this.configuration, getPackagesToScan());
		}

		private String[] getPackagesToScan() {
			List<String> packages = EntityScanPackages.get(this.applicationContext)
					.getPackageNames();
			if (packages.isEmpty()
					&& AutoConfigurationPackages.has(this.applicationContext)) {
				packages = AutoConfigurationPackages.get(this.applicationContext);
			}
			return packages.toArray(new String[packages.size()]);
		}

		@Override
		@Bean
		@Scope(scopeName = "${spring.data.neo4j.session.scope:singleton}", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public Session getSession() throws Exception {
			return super.getSession();
		}

	}

}
