/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.ldap;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.DirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.core.support.SimpleDirContextAuthenticationStrategy;
import org.springframework.ldap.pool2.factory.PoolConfig;
import org.springframework.ldap.pool2.factory.PooledContextSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LdapAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
class LdapAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LdapAutoConfiguration.class));

	@Test
	void contextSourceWithDefaultUrl() {
		this.contextRunner.run((context) -> {
			LdapContextSource contextSource = context.getBean(LdapContextSource.class);
			assertThat(contextSource.getUrls()).containsExactly("ldap://localhost:389");
			assertThat(contextSource.isAnonymousReadOnly()).isTrue();
		});
	}

	@Test
	void contextSourceWithSingleUrl() {
		this.contextRunner.withPropertyValues("spring.ldap.urls:ldap://localhost:123").run((context) -> {
			LdapContextSource contextSource = context.getBean(LdapContextSource.class);
			assertThat(contextSource.getUrls()).containsExactly("ldap://localhost:123");
		});
	}

	@Test
	void contextSourceWithSeveralUrls() {
		this.contextRunner.withPropertyValues("spring.ldap.urls:ldap://localhost:123,ldap://mycompany:123")
				.run((context) -> {
					LdapContextSource contextSource = context.getBean(LdapContextSource.class);
					LdapProperties ldapProperties = context.getBean(LdapProperties.class);
					assertThat(contextSource.getUrls()).containsExactly("ldap://localhost:123", "ldap://mycompany:123");
					assertThat(ldapProperties.getUrls()).hasSize(2);
				});
	}

	@Test
	void contextSourceWithUserDoesNotEnableAnonymousReadOnly() {
		this.contextRunner.withPropertyValues("spring.ldap.username:root").run((context) -> {
			LdapContextSource contextSource = context.getBean(LdapContextSource.class);
			assertThat(contextSource.getUserDn()).isEqualTo("root");
			assertThat(contextSource.isAnonymousReadOnly()).isFalse();
		});
	}

	@Test
	void contextSourceWithExtraCustomization() {
		this.contextRunner.withPropertyValues("spring.ldap.urls:ldap://localhost:123", "spring.ldap.username:root",
				"spring.ldap.password:secret", "spring.ldap.anonymous-read-only:true",
				"spring.ldap.base:cn=SpringDevelopers",
				"spring.ldap.baseEnvironment.java.naming.security.authentication:DIGEST-MD5").run((context) -> {
					LdapContextSource contextSource = context.getBean(LdapContextSource.class);
					assertThat(contextSource.getUserDn()).isEqualTo("root");
					assertThat(contextSource.getPassword()).isEqualTo("secret");
					assertThat(contextSource.isAnonymousReadOnly()).isTrue();
					assertThat(contextSource.getBaseLdapPathAsString()).isEqualTo("cn=SpringDevelopers");
					LdapProperties ldapProperties = context.getBean(LdapProperties.class);
					assertThat(ldapProperties.getBaseEnvironment()).containsEntry("java.naming.security.authentication",
							"DIGEST-MD5");
				});
	}

	@Test
	void contextSourceWithNoCustomization() {
		this.contextRunner.run((context) -> {
			LdapContextSource contextSource = context.getBean(LdapContextSource.class);
			assertThat(contextSource.getUserDn()).isEqualTo("");
			assertThat(contextSource.getPassword()).isEqualTo("");
			assertThat(contextSource.isAnonymousReadOnly()).isTrue();
			assertThat(contextSource.getBaseLdapPathAsString()).isEqualTo("");
		});
	}

	@Test
	void templateExists() {
		this.contextRunner.withPropertyValues("spring.ldap.urls:ldap://localhost:389")
				.run((context) -> assertThat(context).hasSingleBean(LdapTemplate.class));
	}

	@Test
	void contextSourceWithUserProvidedPooledContextSource() {
		this.contextRunner.withUserConfiguration(PooledContextSourceConfig.class).run((context) -> {
			LdapContextSource contextSource = context.getBean(LdapContextSource.class);
			assertThat(contextSource.getUrls()).containsExactly("ldap://localhost:389");
			assertThat(contextSource.isAnonymousReadOnly()).isTrue();
		});
	}

	@Test
	void contextSourceWithCustomUniqueDirContextAuthenticationStrategy() {
		this.contextRunner.withUserConfiguration(CustomDirContextAuthenticationStrategy.class).run((context) -> {
			assertThat(context).hasSingleBean(DirContextAuthenticationStrategy.class);
			LdapContextSource contextSource = context.getBean(LdapContextSource.class);
			assertThat(contextSource).extracting("authenticationStrategy")
					.isSameAs(context.getBean("customDirContextAuthenticationStrategy"));
		});
	}

	@Test
	void contextSourceWithCustomNonUniqueDirContextAuthenticationStrategy() {
		this.contextRunner.withUserConfiguration(CustomDirContextAuthenticationStrategy.class,
				AnotherCustomDirContextAuthenticationStrategy.class).run((context) -> {
					assertThat(context).hasBean("customDirContextAuthenticationStrategy")
							.hasBean("anotherCustomDirContextAuthenticationStrategy");
					LdapContextSource contextSource = context.getBean(LdapContextSource.class);
					assertThat(contextSource).extracting("authenticationStrategy")
							.isNotSameAs(context.getBean("customDirContextAuthenticationStrategy"))
							.isNotSameAs(context.getBean("anotherCustomDirContextAuthenticationStrategy"))
							.isInstanceOf(SimpleDirContextAuthenticationStrategy.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class PooledContextSourceConfig {

		@Bean
		@Primary
		PooledContextSource pooledContextSource(LdapContextSource ldapContextSource) {
			PooledContextSource pooledContextSource = new PooledContextSource(new PoolConfig());
			pooledContextSource.setContextSource(ldapContextSource);
			return pooledContextSource;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDirContextAuthenticationStrategy {

		@Bean
		DirContextAuthenticationStrategy customDirContextAuthenticationStrategy() {
			return mock(DirContextAuthenticationStrategy.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AnotherCustomDirContextAuthenticationStrategy {

		@Bean
		DirContextAuthenticationStrategy anotherCustomDirContextAuthenticationStrategy() {
			return mock(DirContextAuthenticationStrategy.class);
		}

	}

}
