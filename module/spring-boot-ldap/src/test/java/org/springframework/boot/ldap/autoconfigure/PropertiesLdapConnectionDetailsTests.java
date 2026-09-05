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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesLdapConnectionDetails}.
 *
 * @author Moritz Halbritter
 */
class PropertiesLdapConnectionDetailsTests {

	private final LdapProperties properties = new LdapProperties();

	@Test
	void shouldUseDefaultLdapPortWhenSslIsNotEnabled() {
		assertThat(createConnectionDetails().getUrls()).containsExactly("ldap://localhost:389");
	}

	@Test
	void shouldUseDefaultLdapsPortWhenSslIsEnabled() {
		this.properties.getSsl().setEnabled(true);
		assertThat(createConnectionDetails().getUrls()).containsExactly("ldaps://localhost:636");
	}

	@Test
	void shouldUseDefaultLdapsPortWhenSslBundleIsConfigured() {
		this.properties.getSsl().setBundle("example");
		assertThat(createConnectionDetails().getUrls()).containsExactly("ldaps://localhost:636");
	}

	@Test
	void shouldUseConfiguredUrlsRegardlessOfSsl() {
		this.properties.setUrls(new String[] { "ldap://localhost:1234" });
		this.properties.getSsl().setEnabled(true);
		assertThat(createConnectionDetails().getUrls()).containsExactly("ldap://localhost:1234");
	}

	private PropertiesLdapConnectionDetails createConnectionDetails() {
		return new PropertiesLdapConnectionDetails(this.properties, null);
	}

}
