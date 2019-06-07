/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.audit.listener.AbstractAuditListener;
import org.springframework.boot.actuate.audit.listener.AuditListener;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.boot.actuate.security.AbstractAuthorizationAuditListener;
import org.springframework.boot.actuate.security.AuthenticationAuditListener;
import org.springframework.boot.actuate.security.AuthorizationAuditListener;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link AuditEvent}s.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @since 2.0.0
 */
@Configuration
public class AuditAutoConfiguration {

	private final AuditEventRepository auditEventRepository;

	public AuditAutoConfiguration(ObjectProvider<AuditEventRepository> auditEventRepository) {
		this.auditEventRepository = auditEventRepository.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean(AbstractAuditListener.class)
	public AuditListener auditListener() throws Exception {
		return new AuditListener(this.auditEventRepository);
	}

	@Bean
	@ConditionalOnClass(name = "org.springframework.security.authentication.event.AbstractAuthenticationEvent")
	@ConditionalOnMissingBean(AbstractAuthenticationAuditListener.class)
	public AuthenticationAuditListener authenticationAuditListener() throws Exception {
		return new AuthenticationAuditListener();
	}

	@Bean
	@ConditionalOnClass(name = "org.springframework.security.access.event.AbstractAuthorizationEvent")
	@ConditionalOnMissingBean(AbstractAuthorizationAuditListener.class)
	public AuthorizationAuditListener authorizationAuditListener() throws Exception {
		return new AuthorizationAuditListener();
	}

	@Configuration
	@ConditionalOnMissingBean(AuditEventRepository.class)
	protected static class AuditEventRepositoryConfiguration {

		@Bean
		public InMemoryAuditEventRepository auditEventRepository() throws Exception {
			return new InMemoryAuditEventRepository();
		}

	}

}
