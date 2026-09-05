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

package org.springframework.boot.ldap.autoconfigure.embedded;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ldap.autoconfigure.LdapProperties;
import org.springframework.boot.ldap.autoconfigure.LdapSslSocketFactory;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EmbeddedLdapConnectionDetails}.
 *
 * @author Moritz Halbritter
 */
class EmbeddedLdapConnectionDetailsTests {

	private final MockEnvironment environment = new MockEnvironment().withProperty("local.ldap.port", "12345");

	private final LdapProperties properties = new LdapProperties();

	private final EmbeddedLdapProperties embeddedProperties = new EmbeddedLdapProperties();

	@AfterEach
	void clearSslBundle() {
		ReflectionTestUtils.setField(LdapSslSocketFactory.class, "sslBundle", null);
	}

	@Test
	void shouldUseLdapUrlWhenEmbeddedSslIsNotEnabled() {
		assertThat(createConnectionDetails(null).getUrls()).containsExactly("ldap://localhost:12345");
	}

	@Test
	void shouldUseLdapsUrlWhenEmbeddedSslIsEnabled() {
		this.embeddedProperties.getSsl().setBundle("server");
		assertThat(createConnectionDetails(null).getUrls()).containsExactly("ldaps://localhost:12345");
	}

	@Test
	void shouldIgnoreConfiguredUrls() {
		this.properties.setUrls(new String[] { "ldaps://ldap.example.com" });
		assertThat(createConnectionDetails(null).getUrls()).containsExactly("ldap://localhost:12345");
	}

	@Test
	void shouldUseEmbeddedCredentials() {
		this.embeddedProperties.getCredential().setUsername("uid=root");
		this.embeddedProperties.getCredential().setPassword("boot");
		this.properties.setUsername("uid=other");
		this.properties.setPassword("other");
		EmbeddedLdapConnectionDetails connectionDetails = createConnectionDetails(null);
		assertThat(connectionDetails.getUsername()).isEqualTo("uid=root");
		assertThat(connectionDetails.getPassword()).isEqualTo("boot");
	}

	@Test
	void shouldUseServerSslBundleWhenClientBundleIsNotSet() {
		SslBundle serverBundle = mock(SslBundle.class);
		SslBundles sslBundles = sslBundles("server", serverBundle);
		this.embeddedProperties.getSsl().setBundle("server");
		assertThat(createConnectionDetails(sslBundles).getSslBundle()).isSameAs(serverBundle);
	}

	@Test
	void shouldPreferServerSslBundleOverClientSslBundle() {
		SslBundle serverBundle = mock(SslBundle.class);
		DefaultSslBundleRegistry sslBundles = sslBundles("server", serverBundle);
		sslBundles.registerBundle("client", mock(SslBundle.class));
		this.embeddedProperties.getSsl().setBundle("server");
		this.properties.getSsl().setBundle("client");
		assertThat(createConnectionDetails(sslBundles).getSslBundle()).isSameAs(serverBundle);
	}

	@Test
	void shouldIgnoreClientSslBundleThatDoesNotExist() {
		SslBundle serverBundle = mock(SslBundle.class);
		SslBundles sslBundles = sslBundles("server", serverBundle);
		this.embeddedProperties.getSsl().setBundle("server");
		this.properties.getSsl().setBundle("does-not-exist");
		assertThat(createConnectionDetails(sslBundles).getSslBundle()).isSameAs(serverBundle);
	}

	@Test
	void shouldUseServerSslBundleWhenClientSslIsDisabled() {
		SslBundle serverBundle = mock(SslBundle.class);
		SslBundles sslBundles = sslBundles("server", serverBundle);
		this.embeddedProperties.getSsl().setBundle("server");
		this.properties.getSsl().setEnabled(false);
		assertThat(createConnectionDetails(sslBundles).getSslBundle()).isSameAs(serverBundle);
	}

	@Test
	void shouldNotUseSslBundleWhenEmbeddedSslIsDisabled() {
		SslBundles sslBundles = sslBundles("server", mock(SslBundle.class));
		this.embeddedProperties.getSsl().setBundle("server");
		this.embeddedProperties.getSsl().setEnabled(false);
		assertThat(createConnectionDetails(sslBundles).getSslBundle()).isNull();
	}

	@Test
	void shouldIgnoreClientSslBundleWhenEmbeddedSslIsDisabled() {
		SslBundles sslBundles = sslBundles("client", mock(SslBundle.class));
		this.properties.getSsl().setBundle("client");
		assertThat(createConnectionDetails(sslBundles).getSslBundle()).isNull();
	}

	@Test
	void shouldNotTrackReloadsOfTheBundleTheEmbeddedListenerStartedWith() {
		DefaultSslBundleRegistry sslBundles = sslBundles("server", mock(SslBundle.class, "original"));
		this.embeddedProperties.getSsl().setBundle("server");
		createConnectionDetails(sslBundles);
		sslBundles.updateBundle("server", mock(SslBundle.class, "reloaded"));
		assertThat(ReflectionTestUtils.getField(LdapSslSocketFactory.class, "sslBundle")).isNull();
	}

	@Test
	void shouldUseServerSslBundleWhenUrlsAreConfigured() {
		SslBundle serverBundle = mock(SslBundle.class);
		SslBundles sslBundles = sslBundles("server", serverBundle);
		this.embeddedProperties.getSsl().setBundle("server");
		this.properties.setUrls(new String[] { "ldap://ldap.example.com" });
		assertThat(createConnectionDetails(sslBundles).getSslBundle()).isSameAs(serverBundle);
	}

	@Test
	void shouldNotUseSslBundleWhenClientSslIsEnabledWithoutABundle() {
		this.properties.getSsl().setEnabled(true);
		assertThat(createConnectionDetails(null).getSslBundle()).isNull();
	}

	private DefaultSslBundleRegistry sslBundles(String name, SslBundle bundle) {
		return new DefaultSslBundleRegistry(name, bundle);
	}

	private EmbeddedLdapConnectionDetails createConnectionDetails(@Nullable SslBundles sslBundles) {
		return new EmbeddedLdapConnectionDetails(this.environment, this.properties, this.embeddedProperties,
				sslBundles);
	}

}
