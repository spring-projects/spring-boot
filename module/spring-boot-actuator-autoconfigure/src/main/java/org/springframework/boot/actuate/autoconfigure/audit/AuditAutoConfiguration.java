/*
 * Copyright 2012-present the original author or authors.
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link AuditEvent}s.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnBean(AuditEventRepository.class)
@ConditionalOnBooleanProperty(name = "management.auditevents.enabled", matchIfMissing = true)
public final class AuditAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AbstractAuditListener.class)
	AuditListener auditListener(AuditEventRepository auditEventRepository) {
		return new AuditListener(auditEventRepository);
	}

	@Bean
	@ConditionalOnMissingBean(AbstractAuthenticationAuditListener.class)
	AuthenticationAuditListener authenticationAuditListener() {
		return new AuthenticationAuditListener();
	}

	@Bean
	@ConditionalOnMissingBean(AbstractAuthorizationAuditListener.class)
	AuthorizationAuditListener authorizationAuditListener() {
		return new AuthorizationAuditListener();
	}

}
