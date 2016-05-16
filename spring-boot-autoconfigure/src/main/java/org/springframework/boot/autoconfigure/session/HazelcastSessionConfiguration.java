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

import com.hazelcast.core.HazelcastInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.SessionRepository;
import org.springframework.session.hazelcast.config.annotation.web.http.HazelcastHttpSessionConfiguration;

/**
 * Hazelcast backed session configuration.
 *
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnBean(HazelcastInstance.class)
@Conditional(SessionCondition.class)
class HazelcastSessionConfiguration {

	@Configuration
	public static class SpringBootHazelcastHttpSessionConfiguration
			extends HazelcastHttpSessionConfiguration {

		@Autowired
		public void customize(SessionProperties sessionProperties) {
			Integer timeout = sessionProperties.getTimeout();
			if (timeout != null) {
				setMaxInactiveIntervalInSeconds(timeout);
			}
			setSessionMapName(sessionProperties.getHazelcast().getMapName());
		}

	}
}
