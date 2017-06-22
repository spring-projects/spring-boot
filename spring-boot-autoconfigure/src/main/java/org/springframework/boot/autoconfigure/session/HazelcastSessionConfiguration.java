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

package org.springframework.boot.autoconfigure.session;

import javax.annotation.PostConstruct;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.SessionRepository;
import org.springframework.session.hazelcast.HazelcastSessionRepository;
import org.springframework.session.hazelcast.config.annotation.web.http.HazelcastHttpSessionConfiguration;

/**
 * Hazelcast backed session configuration.
 *
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
@Configuration
@ConditionalOnClass(HazelcastSessionRepository.class)
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnBean(HazelcastInstance.class)
@Conditional(SessionCondition.class)
@EnableConfigurationProperties({ ServerProperties.class,
		HazelcastSessionProperties.class })
class HazelcastSessionConfiguration {

	@Configuration
	public static class SpringBootHazelcastHttpSessionConfiguration
			extends HazelcastHttpSessionConfiguration {

		private final HazelcastSessionProperties sessionProperties;

		private final ServerProperties serverProperties;

		SpringBootHazelcastHttpSessionConfiguration(
				HazelcastSessionProperties sessionProperties,
				ObjectProvider<ServerProperties> serverProperties) {
			this.sessionProperties = sessionProperties;
			this.serverProperties = serverProperties.getIfUnique();
		}

		@PostConstruct
		public void init() {
			if (this.serverProperties != null) {
				Integer timeout = this.serverProperties.getSession().getTimeout();
				if (timeout != null) {
					setMaxInactiveIntervalInSeconds(timeout);
				}
			}
			setSessionMapName(this.sessionProperties.getMapName());
			setHazelcastFlushMode(this.sessionProperties.getFlushMode());
		}

	}

}
