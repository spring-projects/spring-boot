/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Session.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(Session.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class SessionAutoConfiguration {

	@EnableConfigurationProperties
	@ConditionalOnClass(RedisConnectionFactory.class)
	@ConditionalOnWebApplication
	@ConditionalOnMissingBean(RedisHttpSessionConfiguration.class)
	@EnableRedisHttpSession
	@Configuration
	public static class SessionRedisHttpConfiguration {

		@Autowired
		private ServerProperties serverProperties;

		@Autowired
		private RedisOperationsSessionRepository sessionRepository;

		@PostConstruct
		public void applyConfigurationProperties() {
			Integer timeout = this.serverProperties.getSession().getTimeout();
			if (timeout != null) {
				this.sessionRepository.setDefaultMaxInactiveInterval(timeout);
			}
		}

		@Configuration
		@ConditionalOnMissingBean(value = ServerProperties.class, search = SearchStrategy.CURRENT)
		// Just in case user switches off ServerPropertiesAutoConfiguration
		public static class ServerPropertiesConfiguration {

			@Bean
			// Use the same bean name as the default one for any old webapp
			public ServerProperties serverProperties() {
				return new ServerProperties();
			}

		}

	}

}
