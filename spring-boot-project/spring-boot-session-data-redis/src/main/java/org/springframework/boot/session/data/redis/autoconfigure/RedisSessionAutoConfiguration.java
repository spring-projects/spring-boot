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

package org.springframework.boot.session.data.redis.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.RedisReactiveAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.ReactiveSessionRepositoryCustomizer;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.ReactiveRedisIndexedSessionRepository;
import org.springframework.session.data.redis.ReactiveRedisSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.data.redis.config.ConfigureNotifyKeyspaceEventsAction;
import org.springframework.session.data.redis.config.ConfigureReactiveRedisAction;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.ConfigureNotifyKeyspaceEventsReactiveAction;
import org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration;
import org.springframework.session.data.redis.config.annotation.web.http.RedisIndexedHttpSessionConfiguration;
import org.springframework.session.data.redis.config.annotation.web.server.RedisIndexedWebSessionConfiguration;
import org.springframework.session.data.redis.config.annotation.web.server.RedisWebSessionConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Session Data Redis.
 *
 * @author Andy Wilkinson
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 4.0.0
 */
@AutoConfiguration(before = SessionAutoConfiguration.class,
		beforeName = { "org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration",
				"org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration" },
		after = { RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class },
		afterName = "org.springframework.boot.webflux.autoconfigure.WebSessionIdResolverAutoConfiguration")
@ConditionalOnClass(Session.class)
@EnableConfigurationProperties({ RedisSessionProperties.class, ServerProperties.class, SessionProperties.class })
public class RedisSessionAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ RedisTemplate.class, RedisIndexedSessionRepository.class })
	@ConditionalOnMissingBean(SessionRepository.class)
	@ConditionalOnBean(RedisConnectionFactory.class)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class ServletRedisSessionConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(name = "spring.session.redis.repository-type", havingValue = "default",
				matchIfMissing = true)
		@Import(RedisHttpSessionConfiguration.class)
		static class DefaultRedisSessionConfiguration {

			@Bean
			@Order(Ordered.HIGHEST_PRECEDENCE)
			SessionRepositoryCustomizer<RedisSessionRepository> springBootSessionRepositoryCustomizer(
					SessionProperties sessionProperties, RedisSessionProperties redisSessionProperties,
					ServerProperties serverProperties) {
				String cleanupCron = redisSessionProperties.getCleanupCron();
				if (cleanupCron != null) {
					throw new InvalidConfigurationPropertyValueException("spring.session.redis.cleanup-cron",
							cleanupCron, "Cron-based cleanup is only supported when "
									+ "spring.session.redis.repository-type is set to indexed.");
				}
				return (sessionRepository) -> {
					PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
					map.from(sessionProperties
						.determineTimeout(() -> serverProperties.getServlet().getSession().getTimeout()))
						.to(sessionRepository::setDefaultMaxInactiveInterval);
					map.from(redisSessionProperties::getNamespace).to(sessionRepository::setRedisKeyNamespace);
					map.from(redisSessionProperties::getFlushMode).to(sessionRepository::setFlushMode);
					map.from(redisSessionProperties::getSaveMode).to(sessionRepository::setSaveMode);
				};
			}

		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(name = "spring.session.redis.repository-type", havingValue = "indexed")
		@Import(RedisIndexedHttpSessionConfiguration.class)
		static class IndexedRedisSessionConfiguration {

			@Bean
			@ConditionalOnMissingBean
			ConfigureRedisAction configureRedisAction(RedisSessionProperties redisSessionProperties) {
				return switch (redisSessionProperties.getConfigureAction()) {
					case NOTIFY_KEYSPACE_EVENTS -> new ConfigureNotifyKeyspaceEventsAction();
					case NONE -> ConfigureRedisAction.NO_OP;
				};
			}

			@Bean
			@Order(Ordered.HIGHEST_PRECEDENCE)
			SessionRepositoryCustomizer<RedisIndexedSessionRepository> springBootSessionRepositoryCustomizer(
					SessionProperties sessionProperties, RedisSessionProperties redisSessionProperties,
					ServerProperties serverProperties) {
				return (sessionRepository) -> {
					PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
					map.from(sessionProperties
						.determineTimeout(() -> serverProperties.getServlet().getSession().getTimeout()))
						.to(sessionRepository::setDefaultMaxInactiveInterval);
					map.from(redisSessionProperties::getNamespace).to(sessionRepository::setRedisKeyNamespace);
					map.from(redisSessionProperties::getFlushMode).to(sessionRepository::setFlushMode);
					map.from(redisSessionProperties::getSaveMode).to(sessionRepository::setSaveMode);
					map.from(redisSessionProperties::getCleanupCron).to(sessionRepository::setCleanupCron);
				};
			}

		}

	}

	@ConditionalOnWebApplication(type = Type.REACTIVE)
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ ReactiveRedisConnectionFactory.class, ReactiveRedisSessionRepository.class })
	@ConditionalOnMissingBean(ReactiveSessionRepository.class)
	@ConditionalOnBean(ReactiveRedisConnectionFactory.class)
	class ReactiveRedisSessionConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(name = "spring.session.redis.repository-type", havingValue = "default",
				matchIfMissing = true)
		@Import(RedisWebSessionConfiguration.class)
		static class DefaultRedisSessionConfiguration {

			@Bean
			ReactiveSessionRepositoryCustomizer<ReactiveRedisSessionRepository> springBootSessionRepositoryCustomizer(
					SessionProperties sessionProperties, RedisSessionProperties redisSessionProperties,
					ServerProperties serverProperties) {
				return (sessionRepository) -> {
					PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
					map.from(sessionProperties
						.determineTimeout(() -> serverProperties.getReactive().getSession().getTimeout()))
						.to(sessionRepository::setDefaultMaxInactiveInterval);
					map.from(redisSessionProperties::getNamespace).to(sessionRepository::setRedisKeyNamespace);
					map.from(redisSessionProperties::getSaveMode).to(sessionRepository::setSaveMode);
				};
			}

		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(name = "spring.session.redis.repository-type", havingValue = "indexed")
		@Import(RedisIndexedWebSessionConfiguration.class)
		static class IndexedRedisSessionConfiguration {

			@Bean
			@ConditionalOnMissingBean
			ConfigureReactiveRedisAction configureReactiveRedisAction(RedisSessionProperties redisSessionProperties) {
				return switch (redisSessionProperties.getConfigureAction()) {
					case NOTIFY_KEYSPACE_EVENTS -> new ConfigureNotifyKeyspaceEventsReactiveAction();
					case NONE -> ConfigureReactiveRedisAction.NO_OP;
				};
			}

			@Bean
			ReactiveSessionRepositoryCustomizer<ReactiveRedisIndexedSessionRepository> springBootSessionRepositoryCustomizer(
					SessionProperties sessionProperties, RedisSessionProperties redisSessionProperties,
					ServerProperties serverProperties) {
				return (sessionRepository) -> {
					PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
					map.from(sessionProperties
						.determineTimeout(() -> serverProperties.getReactive().getSession().getTimeout()))
						.to(sessionRepository::setDefaultMaxInactiveInterval);
					map.from(redisSessionProperties::getNamespace).to(sessionRepository::setRedisKeyNamespace);
					map.from(redisSessionProperties::getSaveMode).to(sessionRepository::setSaveMode);
				};
			}

		}

	}

}
