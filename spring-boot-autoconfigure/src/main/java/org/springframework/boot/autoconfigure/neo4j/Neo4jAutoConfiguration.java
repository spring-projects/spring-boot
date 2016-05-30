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

package org.springframework.boot.autoconfigure.neo4j;

import java.util.List;

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.neo4j.SessionFactoryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j support.
 * <p>
 * Registers a {@link Neo4jTemplate} bean if no other bean of the same type is configured.
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
public class Neo4jAutoConfiguration {

	@Configuration
	@Import(SessionFactoryProviderConfiguration.class)
	public static class SpringBootNeo4jConfiguration extends Neo4jConfiguration {

		private final ObjectProvider<SessionFactoryProvider> sessionFactoryProvider;

		public SpringBootNeo4jConfiguration(
				ObjectProvider<SessionFactoryProvider> sessionFactoryProvider) {
			this.sessionFactoryProvider = sessionFactoryProvider;
		}

		@Override
		public SessionFactory getSessionFactory() {
			return this.sessionFactoryProvider.getObject().getSessionFactory();
		}

		@Bean
		@Scope(scopeName = "${spring.data.neo4j.session.scope:singleton}", proxyMode = ScopedProxyMode.TARGET_CLASS)
		@Override
		public Session getSession() throws Exception {
			return getSessionFactory().openSession();
		}

	}

	@Configuration
	@Import(Neo4jConfigurationConfiguration.class)
	static class SessionFactoryProviderConfiguration implements BeanFactoryAware {

		private final org.neo4j.ogm.config.Configuration configuration;

		private ConfigurableListableBeanFactory beanFactory;

		SessionFactoryProviderConfiguration(
				org.neo4j.ogm.config.Configuration configuration) {
			this.configuration = configuration;
		}

		@Bean
		@ConditionalOnMissingBean
		public SessionFactoryProvider sessionFactoryProvider() {
			SessionFactoryProvider provider = new SessionFactoryProvider();
			provider.setConfiguration(this.configuration);
			provider.setPackagesToScan(getPackagesToScan());
			return provider;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}

		protected String[] getPackagesToScan() {
			if (AutoConfigurationPackages.has(this.beanFactory)) {
				List<String> basePackages = AutoConfigurationPackages
						.get(this.beanFactory);
				return basePackages.toArray(new String[basePackages.size()]);
			}
			return new String[0];
		}

	}

	@Configuration
	static class Neo4jConfigurationConfiguration {

		private final Neo4jProperties properties;

		Neo4jConfigurationConfiguration(Neo4jProperties properties) {
			this.properties = properties;
		}

		@Bean
		@ConditionalOnMissingBean
		public org.neo4j.ogm.config.Configuration configuration() {
			return this.properties.createConfiguration();
		}

	}

}
