/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.health;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for configured LDAP server(s).
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @version 1.5.0
 */
public class LdapHealthIndicator extends AbstractHealthIndicator {

	private static final ContextExecutor<String> versionContextExecutor = new VersionContextExecutor();

	private final LdapOperations ldapOperations;

	public LdapHealthIndicator(LdapOperations ldapOperations) {
		Assert.notNull(ldapOperations, "LdapOperations must not be null");
		this.ldapOperations = ldapOperations;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		String version = this.ldapOperations.executeReadOnly(versionContextExecutor);
		builder.up().withDetail("version", version);
	}

	private static class VersionContextExecutor implements ContextExecutor<String> {

		@Override
		public String executeWithContext(DirContext ctx) throws NamingException {
			Object version = ctx.getEnvironment().get("java.naming.ldap.version");
			if (version != null) {
				return (String) version;
			}
			return null;
		}

	}

}
