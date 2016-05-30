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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

/**
 * HashMap based session configuration, intended as a fallback.
 *
 * @author Tommy Ludwig
 * @author Stephane Nicoll
 */
@Configuration
@EnableSpringHttpSession
@Conditional(SessionCondition.class)
@ConditionalOnMissingBean(SessionRepository.class)
class HashMapSessionConfiguration {

	@Bean
	public SessionRepository<ExpiringSession> sessionRepository(
			SessionProperties properties) {
		MapSessionRepository repository = new MapSessionRepository();
		Integer timeout = properties.getTimeout();
		if (timeout != null) {
			repository.setDefaultMaxInactiveInterval(timeout);
		}
		return repository;
	}

}
