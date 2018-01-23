/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.ldap;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LdapAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
public class LdapAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LdapAutoConfiguration.class));

	@Test
	public void testDefaultUrl() {
		this.contextRunner.run(context -> {
			ContextSource contextSource = context.getBean(ContextSource.class);
			String[] urls = (String[]) ReflectionTestUtils
					.getField(contextSource, "urls");
			assertThat(urls).containsExactly("ldap://localhost:389");
		});
	}

	@Test
	public void testContextSourceSetOneUrl() {
		this.contextRunner.withPropertyValues("spring.ldap.urls:ldap://localhost:123")
				.run(context -> {
					ContextSource contextSource = context.getBean(ContextSource.class);
					String[] urls = (String[]) ReflectionTestUtils.getField(
							contextSource, "urls");
					assertThat(urls).containsExactly("ldap://localhost:123");
				});
	}

	@Test
	public void testContextSourceSetTwoUrls() {
		this.contextRunner
				.withPropertyValues(
						"spring.ldap.urls:ldap://localhost:123,ldap://mycompany:123")
				.run(context -> {
					ContextSource contextSource = context.getBean(ContextSource.class);
					LdapProperties ldapProperties = context.getBean(LdapProperties.class);
					String[] urls = (String[]) ReflectionTestUtils.getField(
							contextSource, "urls");
					assertThat(urls).containsExactly("ldap://localhost:123",
							"ldap://mycompany:123");
					assertThat(ldapProperties.getUrls()).hasSize(2);
				});
	}

	@Test
	public void testContextSourceWithMoreProperties() {
		this.contextRunner
				.withPropertyValues(
						"spring.ldap.urls:ldap://localhost:123",
						"spring.ldap.username:root",
						"spring.ldap.password:root",
						"spring.ldap.base:cn=SpringDevelopers",
						"spring.ldap.baseEnvironment.java.naming.security.authentication:DIGEST-MD5")
				.run(context -> {
					LdapProperties ldapProperties = context.getBean(LdapProperties.class);
					assertThat(ldapProperties.getBaseEnvironment()).containsEntry(
							"java.naming.security.authentication", "DIGEST-MD5");
				});
	}

}
