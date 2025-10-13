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

package org.springframework.boot.actuate.autoconfigure.audit;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.listener.AbstractAuditListener;
import org.springframework.boot.actuate.audit.listener.AuditListener;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.boot.actuate.security.AbstractAuthorizationAuditListener;
import org.springframework.boot.actuate.security.AuthenticationAuditListener;
import org.springframework.boot.actuate.security.AuthorizationAuditListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link AuditEvent}s.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnBean(AuditEventRepository.class)
@ConditionalOnProperty(prefix = "management.auditevents", name = "enabled", matchIfMissing = true)
public class AuditAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AbstractAuditListener.class)
	public AuditListener auditListener(AuditEventRepository auditEventRepository) {
		return new AuditListener(auditEventRepository);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.security.authentication.event.AbstractAuthenticationEvent")
	static class AuthenticationAuditConfiguration {

		@Bean
		@ConditionalOnMissingBean(AbstractAuthenticationAuditListener.class)
		AuthenticationAuditListener authenticationAuditListener() {
			return new AuthenticationAuditListener();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.security.access.event.AbstractAuthorizationEvent")
	static class AuthorizationAuditConfiguration {

		@Bean
		@ConditionalOnMissingBean(AbstractAuthorizationAuditListener.class)
		AuthorizationAuditListener authorizationAuditListener() {
			return new AuthorizationAuditListener();
		}

	}

}
