/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.session.data.mongodb.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.data.mongodb.autoconfigure.MongoDataAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.ReactiveSessionRepositoryCustomizer;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.mongo.ReactiveMongoSessionRepository;
import org.springframework.session.data.mongo.config.annotation.web.http.MongoHttpSessionConfiguration;
import org.springframework.session.data.mongo.config.annotation.web.reactive.ReactiveMongoWebSessionConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Session Data MongoDB.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 4.0.0
 */
@AutoConfiguration(before = SessionAutoConfiguration.class,
		beforeName = { "org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration",
				"org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration" },
		after = { MongoDataAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class },
		afterName = "org.springframework.boot.webflux.autoconfigure.WebSessionIdResolverAutoConfiguration")
@ConditionalOnClass(Session.class)
@EnableConfigurationProperties(MongoSessionProperties.class)
public class MongoSessionAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ MongoOperations.class, MongoIndexedSessionRepository.class })
	@ConditionalOnBean(MongoOperations.class)
	@ConditionalOnMissingBean(SessionRepository.class)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@Import(MongoHttpSessionConfiguration.class)
	class ServletMongoSessionConfiguration {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		SessionRepositoryCustomizer<MongoIndexedSessionRepository> springBootSessionRepositoryCustomizer(
				SessionProperties sessionProperties, MongoSessionProperties mongoSessionProperties,
				ServerProperties serverProperties) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			return (sessionRepository) -> {
				map.from(sessionProperties
					.determineTimeout(() -> serverProperties.getServlet().getSession().getTimeout()))
					.to(sessionRepository::setDefaultMaxInactiveInterval);
				map.from(mongoSessionProperties::getCollectionName).to(sessionRepository::setCollectionName);
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ ReactiveMongoOperations.class, ReactiveMongoSessionRepository.class })
	@ConditionalOnMissingBean(ReactiveSessionRepository.class)
	@ConditionalOnBean(ReactiveMongoOperations.class)
	@Import(ReactiveMongoWebSessionConfiguration.class)
	class ReactiveMongoSessionConfiguration {

		@Bean
		ReactiveSessionRepositoryCustomizer<ReactiveMongoSessionRepository> springBootSessionRepositoryCustomizer(
				SessionProperties sessionProperties, MongoSessionProperties mongoSessionProperties,
				ServerProperties serverProperties) {
			return (sessionRepository) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(sessionProperties
					.determineTimeout(() -> serverProperties.getReactive().getSession().getTimeout()))
					.to(sessionRepository::setDefaultMaxInactiveInterval);
				map.from(mongoSessionProperties::getCollectionName).to(sessionRepository::setCollectionName);
			};
		}

	}

}
