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

import java.util.Hashtable;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.ldap.ssl.SslBundleSocketFactoryRegistry;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.DirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SSL configuration in {@link LdapAutoConfiguration}.
 *
 * @author Massimo Deiana
 */
class LdapAutoConfigurationSslTests {

	private static final String TEST_KEYSTORE_LOCATION = "classpath:org/springframework/boot/ldap/autoconfigure/embedded/test.jks";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LdapAutoConfiguration.class, SslAutoConfiguration.class));

	@Test
	void whenSslBundleConfiguredWithLdapsUrl_thenSocketFactoryPropertyIsSet() {
		this.contextRunner
			.withPropertyValues("spring.ssl.bundle.jks.test.keystore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.keystore.password=secret",
					"spring.ssl.bundle.jks.test.truststore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.truststore.password=secret", "spring.ldap.urls=ldaps://localhost:636",
					"spring.ldap.ssl.bundle=test")
			.run((context) -> {
				assertThat(context).hasSingleBean(LdapContextSource.class);
				LdapContextSource contextSource = context.getBean(LdapContextSource.class);
				@SuppressWarnings("unchecked")
				Hashtable<String, Object> baseEnv = (Hashtable<String, Object>) Objects
					.requireNonNull(ReflectionTestUtils.getField(contextSource, "baseEnv"));
				assertThat(baseEnv).containsKey("java.naming.ldap.factory.socket");
				assertThat(baseEnv.get("java.naming.ldap.factory.socket"))
					.isEqualTo(SslBundleSocketFactoryRegistry.class.getName());
			});
	}

	@Test
	void whenSslBundleConfiguredWithLdapUrl_thenStartTlsStrategyIsCreated() {
		this.contextRunner
			.withPropertyValues("spring.ssl.bundle.jks.test.keystore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.keystore.password=secret",
					"spring.ssl.bundle.jks.test.truststore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.truststore.password=secret", "spring.ldap.urls=ldap://localhost:389",
					"spring.ldap.ssl.bundle=test")
			.run((context) -> {
				assertThat(context).hasSingleBean(DirContextAuthenticationStrategy.class);
				assertThat(context.getBean(DirContextAuthenticationStrategy.class))
					.isInstanceOf(DefaultTlsDirContextAuthenticationStrategy.class);
			});
	}

	@Test
	void whenSslBundleConfiguredWithExplicitStartTls_thenStartTlsStrategyIsCreated() {
		this.contextRunner
			.withPropertyValues("spring.ssl.bundle.jks.test.keystore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.keystore.password=secret",
					"spring.ssl.bundle.jks.test.truststore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.truststore.password=secret", "spring.ldap.urls=ldap://localhost:389",
					"spring.ldap.ssl.bundle=test", "spring.ldap.ssl.start-tls=true")
			.run((context) -> assertThat(context).hasSingleBean(DefaultTlsDirContextAuthenticationStrategy.class));
	}

	@Test
	void whenSslBundleConfiguredWithExplicitLdapsMode_thenSocketFactoryIsUsed() {
		this.contextRunner
			.withPropertyValues("spring.ssl.bundle.jks.test.keystore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.keystore.password=secret",
					"spring.ssl.bundle.jks.test.truststore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.truststore.password=secret", "spring.ldap.urls=ldap://localhost:389",
					"spring.ldap.ssl.bundle=test", "spring.ldap.ssl.start-tls=false")
			.run((context) -> {
				LdapContextSource contextSource = context.getBean(LdapContextSource.class);
				@SuppressWarnings("unchecked")
				Hashtable<String, Object> baseEnv = (Hashtable<String, Object>) Objects
					.requireNonNull(ReflectionTestUtils.getField(contextSource, "baseEnv"));
				assertThat(baseEnv).containsKey("java.naming.ldap.factory.socket");
				assertThat(context).doesNotHaveBean(DefaultTlsDirContextAuthenticationStrategy.class);
			});
	}

	@Test
	void whenSslDisabled_thenNoSslConfiguration() {
		this.contextRunner
			.withPropertyValues("spring.ssl.bundle.jks.test.keystore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.keystore.password=secret", "spring.ldap.urls=ldap://localhost:389",
					"spring.ldap.ssl.enabled=false", "spring.ldap.ssl.bundle=test")
			.run((context) -> {
				LdapContextSource contextSource = context.getBean(LdapContextSource.class);
				@SuppressWarnings("unchecked")
				Hashtable<String, Object> baseEnv = (Hashtable<String, Object>) Objects
					.requireNonNull(ReflectionTestUtils.getField(contextSource, "baseEnv"));
				assertThat(baseEnv).doesNotContainKey("java.naming.ldap.factory.socket");
				assertThat(context).doesNotHaveBean(DefaultTlsDirContextAuthenticationStrategy.class);
			});
	}

	@Test
	void whenNoSslBundle_thenNoSslConfiguration() {
		this.contextRunner.withPropertyValues("spring.ldap.urls=ldap://localhost:389").run((context) -> {
			LdapContextSource contextSource = context.getBean(LdapContextSource.class);
			@SuppressWarnings("unchecked")
			Hashtable<String, Object> baseEnv = (Hashtable<String, Object>) Objects
				.requireNonNull(ReflectionTestUtils.getField(contextSource, "baseEnv"));
			assertThat(baseEnv).isEmpty();
			assertThat(context).doesNotHaveBean(DefaultTlsDirContextAuthenticationStrategy.class);
		});
	}

	@Test
	void whenHostnameVerificationDisabled_thenStrategyHasNoOpVerifier() {
		this.contextRunner
			.withPropertyValues("spring.ssl.bundle.jks.test.keystore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.keystore.password=secret",
					"spring.ssl.bundle.jks.test.truststore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.truststore.password=secret", "spring.ldap.urls=ldap://localhost:389",
					"spring.ldap.ssl.bundle=test", "spring.ldap.ssl.verify-hostname=false")
			.run((context) -> {
				DefaultTlsDirContextAuthenticationStrategy strategy = context
					.getBean(DefaultTlsDirContextAuthenticationStrategy.class);
				Object hostnameVerifier = ReflectionTestUtils.getField(strategy, "hostnameVerifier");
				assertThat(hostnameVerifier).isNotNull();
			});
	}

	@Test
	void whenHostnameVerificationEnabled_thenStrategyUsesDefaultVerifier() {
		this.contextRunner
			.withPropertyValues("spring.ssl.bundle.jks.test.keystore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.keystore.password=secret",
					"spring.ssl.bundle.jks.test.truststore.location=" + TEST_KEYSTORE_LOCATION,
					"spring.ssl.bundle.jks.test.truststore.password=secret", "spring.ldap.urls=ldap://localhost:389",
					"spring.ldap.ssl.bundle=test", "spring.ldap.ssl.verify-hostname=true")
			.run((context) -> {
				DefaultTlsDirContextAuthenticationStrategy strategy = context
					.getBean(DefaultTlsDirContextAuthenticationStrategy.class);
				assertThat(strategy).hasFieldOrPropertyWithValue("hostnameVerifier", null);
			});
	}

	@Test
	void whenInvalidSslBundle_thenFailsWithClearError() {
		this.contextRunner
			.withPropertyValues("spring.ldap.urls=ldaps://localhost:636", "spring.ldap.ssl.bundle=nonexistent")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context).getFailure().hasMessageContaining("nonexistent");
			});
	}

	@Test
	void sslRegistryCleanupBeanIsCreated() {
		this.contextRunner.withPropertyValues("spring.ldap.urls=ldap://localhost:389")
			.run((context) -> assertThat(context).hasBean("ldapSslRegistryCleanup"));
	}

}
