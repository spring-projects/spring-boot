/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.health;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for configured LDAP server(s).
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class LdapHealthIndicator extends AbstractHealthIndicator {

	private static final ContextExecutor<@Nullable String> versionContextExecutor = (
			dirContext) -> (String) dirContext.getEnvironment().get("java.naming.ldap.version");

	private final LdapOperations ldapOperations;

	public LdapHealthIndicator(LdapOperations ldapOperations) {
		super("LDAP health check failed");
		Assert.notNull(ldapOperations, "'ldapOperations' must not be null");
		this.ldapOperations = ldapOperations;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		builder.up();
		String version = this.ldapOperations.executeReadOnly(versionContextExecutor);
		if (version != null) {
			builder.withDetail("version", version);
		}
	}

}
