/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.event.EventListener;

import org.springframework.beans.factory.BeanFactory;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.web.support.OpenSessionInViewInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
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
 * @author Michael Simons
 * @since 1.4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SessionFactory.class, Neo4jTransactionManager.class, PlatformTransactionManager.class })
@EnableConfigurationProperties(Neo4jProperties.class)
@Import(Neo4jBookmarkManagementConfiguration.class)
public class Neo4jDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(PlatformTransactionManager.class)
	public Neo4jTransactionManager transactionManager(SessionFactory sessionFactory,
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		Neo4jTransactionManager transactionManager = new Neo4jTransactionManager(sessionFactory);
		transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
		return transactionManager;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(SessionFactory.class)
	static class Neo4jOgmSessionFactoryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		org.neo4j.ogm.config.Configuration configuration(Neo4jProperties properties) {
			return properties.createConfiguration();
		}

		@Bean
		SessionFactory sessionFactory(org.neo4j.ogm.config.Configuration configuration, BeanFactory beanFactory,
				ObjectProvider<EventListener> eventListeners) {
			SessionFactory sessionFactory = new SessionFactory(configuration, getPackagesToScan(beanFactory));
			eventListeners.orderedStream().forEach(sessionFactory::register);
			return sessionFactory;
		}

		private String[] getPackagesToScan(BeanFactory beanFactory) {
			List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
			if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
				packages = AutoConfigurationPackages.get(beanFactory);
			}
			return StringUtils.toStringArray(packages);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnClass({ WebMvcConfigurer.class, OpenSessionInViewInterceptor.class })
	@ConditionalOnMissingBean(OpenSessionInViewInterceptor.class)
	@ConditionalOnProperty(prefix = "spring.data.neo4j", name = "open-in-view", havingValue = "true",
			matchIfMissing = true)
	static class Neo4jWebConfiguration {

		private static final Log logger = LogFactory.getLog(Neo4jWebConfiguration.class);

		@Bean
		OpenSessionInViewInterceptor neo4jOpenSessionInViewInterceptor(Neo4jProperties properties) {
			if (properties.getOpenInView() == null) {
				logger.warn("spring.data.neo4j.open-in-view is enabled by default."
						+ "Therefore, database queries may be performed during view "
						+ "rendering. Explicitly configure "
						+ "spring.data.neo4j.open-in-view to disable this warning");
			}
			return new OpenSessionInViewInterceptor();
		}

		@Bean
		WebMvcConfigurer neo4jOpenSessionInViewInterceptorConfigurer(OpenSessionInViewInterceptor interceptor) {
			return new WebMvcConfigurer() {

				@Override
				public void addInterceptors(InterceptorRegistry registry) {
					registry.addWebRequestInterceptor(interceptor);
				}

			};
		}

	}

}
