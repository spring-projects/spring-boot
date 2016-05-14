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

package org.springframework.boot.autoconfigure.session;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration;

/**
 * Redis backed session configuration.
 *
 * @author Andy Wilkinson
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnBean({ RedisTemplate.class, RedisConnectionFactory.class })
@Conditional(SessionCondition.class)
class RedisSessionConfiguration {

	private static final Logger logger = LoggerFactory
			.getLogger(RedisSessionConfiguration.class);

	@Configuration
	public static class SpringBootRedisHttpSessionConfiguration
			extends RedisHttpSessionConfiguration {

		private SessionProperties sessionProperties;

		@Autowired
		public void customize(SessionProperties sessionProperties) {
			this.sessionProperties = sessionProperties;
			Integer timeout = this.sessionProperties.getTimeout();
			if (timeout != null) {
				setMaxInactiveIntervalInSeconds(timeout);
			}
			SessionProperties.Redis redis = this.sessionProperties.getRedis();
			setRedisNamespace(redis.getNamespace());
			setRedisFlushMode(redis.getFlushMode());
		}

		@PostConstruct
		public void validate() {
			if (this.sessionProperties.getStoreType() == null) {
				logger.warn("Spring Session store type is mandatory: set "
						+ "'spring.session.store-type=redis' in your configuration");
			}
		}

	}

}
