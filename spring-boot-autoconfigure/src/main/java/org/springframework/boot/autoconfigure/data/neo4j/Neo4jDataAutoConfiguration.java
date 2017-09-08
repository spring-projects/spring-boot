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

package org.springframework.boot.autoconfigure.data.neo4j;

import java.util.List;

import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.event.EventListener;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.web.support.OpenSessionInViewInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Neo4j.
 *
 * @author Michael Hunger
 * @author Josh Long
 * @author Vince Bickers
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass({ SessionFactory.class, Neo4jTransactionManager.class,
		PlatformTransactionManager.class })
@ConditionalOnMissingBean(SessionFactory.class)
@EnableConfigurationProperties(Neo4jProperties.class)
public class Neo4jDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public org.neo4j.ogm.config.Configuration configuration(Neo4jProperties properties) {
		org.neo4j.ogm.config.Configuration configuration = properties
				.createConfiguration();
		return configuration;
	}

	@Bean
	public SessionFactory sessionFactory(org.neo4j.ogm.config.Configuration configuration,
			ApplicationContext applicationContext,
			ObjectProvider<List<EventListener>> eventListeners) {
		SessionFactory sessionFactory = new SessionFactory(configuration,
				getPackagesToScan(applicationContext));
		List<EventListener> providedEventListeners = eventListeners.getIfAvailable();
		if (providedEventListeners != null) {
			for (EventListener eventListener : providedEventListeners) {
				sessionFactory.register(eventListener);
			}
		}
		return sessionFactory;
	}

	@Bean
	@ConditionalOnMissingBean(PlatformTransactionManager.class)
	public Neo4jTransactionManager transactionManager(SessionFactory sessionFactory,
			Neo4jProperties properties,
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		return customize(new Neo4jTransactionManager(sessionFactory),
				transactionManagerCustomizers.getIfAvailable());
	}

	private Neo4jTransactionManager customize(Neo4jTransactionManager transactionManager,
			TransactionManagerCustomizers customizers) {
		if (customizers != null) {
			customizers.customize(transactionManager);
		}
		return transactionManager;
	}

	private String[] getPackagesToScan(ApplicationContext applicationContext) {
		List<String> packages = EntityScanPackages.get(applicationContext)
				.getPackageNames();
		if (packages.isEmpty() && AutoConfigurationPackages.has(applicationContext)) {
			packages = AutoConfigurationPackages.get(applicationContext);
		}
		return packages.toArray(new String[packages.size()]);
	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnClass({ WebMvcConfigurer.class, OpenSessionInViewInterceptor.class })
	@ConditionalOnMissingBean(OpenSessionInViewInterceptor.class)
	@ConditionalOnProperty(prefix = "spring.data.neo4j", name = "open-in-view", havingValue = "true", matchIfMissing = true)
	protected static class Neo4jWebConfiguration {

		@Configuration
		protected static class Neo4jWebMvcConfiguration implements WebMvcConfigurer {

			@Bean
			public OpenSessionInViewInterceptor neo4jOpenSessionInViewInterceptor() {
				return new OpenSessionInViewInterceptor();
			}

			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addWebRequestInterceptor(neo4jOpenSessionInViewInterceptor());
			}

		}

	}

}
