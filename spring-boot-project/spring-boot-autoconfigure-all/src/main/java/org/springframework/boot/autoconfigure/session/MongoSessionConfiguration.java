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

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.mongo.config.annotation.web.http.MongoHttpSessionConfiguration;

/**
 * Mongo-backed session configuration.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ MongoOperations.class, MongoIndexedSessionRepository.class })
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnBean(MongoOperations.class)
@EnableConfigurationProperties(MongoSessionProperties.class)
@Import(MongoHttpSessionConfiguration.class)
class MongoSessionConfiguration {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SessionRepositoryCustomizer<MongoIndexedSessionRepository> springBootSessionRepositoryCustomizer(
			SessionProperties sessionProperties, MongoSessionProperties mongoSessionProperties,
			ServerProperties serverProperties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		return (sessionRepository) -> {
			map.from(sessionProperties.determineTimeout(() -> serverProperties.getServlet().getSession().getTimeout()))
				.to(sessionRepository::setDefaultMaxInactiveInterval);
			map.from(mongoSessionProperties::getCollectionName).to(sessionRepository::setCollectionName);
		};
	}

}
