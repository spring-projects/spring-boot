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

package org.springframework.boot.ldap.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ldap.autoconfigure.LdapProperties.Template;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LdapProperties}
 *
 * @author Filip Hrisafov
 */
class LdapPropertiesTests {

	@Test
	void ldapTemplatePropertiesUseConsistentLdapTemplateDefaultValues() {
		Template templateProperties = new LdapProperties().getTemplate();
		LdapTemplate ldapTemplate = new LdapTemplate();
		assertThat(ldapTemplate).hasFieldOrPropertyWithValue("ignorePartialResultException",
				templateProperties.isIgnorePartialResultException());
		assertThat(ldapTemplate).hasFieldOrPropertyWithValue("ignoreNameNotFoundException",
				templateProperties.isIgnoreNameNotFoundException());
		assertThat(ldapTemplate).hasFieldOrPropertyWithValue("ignoreSizeLimitExceededException",
				templateProperties.isIgnoreSizeLimitExceededException());
	}

	@Test
	void determineUrlsShouldDefaultToPlainLdapWhenSslNotEnabled() {
		LdapProperties properties = new LdapProperties();
		assertThat(properties.determineUrls(new MockEnvironment())).containsExactly("ldap://localhost:389");
	}

	@Test
	void determineUrlsShouldDefaultToLdapsWhenSslEnabled() {
		LdapProperties properties = new LdapProperties();
		properties.getSsl().setEnabled(true);
		assertThat(properties.determineUrls(new MockEnvironment())).containsExactly("ldaps://localhost:636");
	}

	@Test
	void determineUrlsShouldDefaultToLdapsWhenSslBundleConfigured() {
		LdapProperties properties = new LdapProperties();
		properties.getSsl().setBundle("example");
		assertThat(properties.determineUrls(new MockEnvironment())).containsExactly("ldaps://localhost:636");
	}

	@Test
	void determineUrlsShouldPreferLocalPortOverDefaultSslPort() {
		LdapProperties properties = new LdapProperties();
		properties.getSsl().setEnabled(true);
		MockEnvironment environment = new MockEnvironment().withProperty("local.ldap.port", "1234");
		assertThat(properties.determineUrls(environment)).containsExactly("ldaps://localhost:1234");
	}

	@Test
	void determineUrlsShouldUseConfiguredUrlsRegardlessOfSsl() {
		LdapProperties properties = new LdapProperties();
		properties.setUrls(new String[] { "ldap://localhost:1234" });
		properties.getSsl().setEnabled(true);
		assertThat(properties.determineUrls(new MockEnvironment())).containsExactly("ldap://localhost:1234");
	}

}
