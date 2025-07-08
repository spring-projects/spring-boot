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

package org.springframework.boot.session.hazelcast.autoconfigure;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.hazelcast.autoconfigure.HazelcastAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.hazelcast.config.annotation.web.http.HazelcastHttpSessionConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Session Hazelcast.
 *
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 4.0.0
 */
@AutoConfiguration(before = SessionAutoConfiguration.class, after = HazelcastAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Session.class, HazelcastIndexedSessionRepository.class })
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnBean(HazelcastInstance.class)
@EnableConfigurationProperties({ HazelcastSessionProperties.class, ServerProperties.class, SessionProperties.class })
@Import(HazelcastHttpSessionConfiguration.class)
public class HazelcastSessionAutoConfiguration {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SessionRepositoryCustomizer<HazelcastIndexedSessionRepository> springBootSessionRepositoryCustomizer(
			SessionProperties sessionProperties, HazelcastSessionProperties hazelcastSessionProperties,
			ServerProperties serverProperties) {
		return (sessionRepository) -> {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(sessionProperties.determineTimeout(() -> serverProperties.getServlet().getSession().getTimeout()))
				.to(sessionRepository::setDefaultMaxInactiveInterval);
			map.from(hazelcastSessionProperties::getMapName).to(sessionRepository::setSessionMapName);
			map.from(hazelcastSessionProperties::getFlushMode).to(sessionRepository::setFlushMode);
			map.from(hazelcastSessionProperties::getSaveMode).to(sessionRepository::setSaveMode);
		};
	}

}
