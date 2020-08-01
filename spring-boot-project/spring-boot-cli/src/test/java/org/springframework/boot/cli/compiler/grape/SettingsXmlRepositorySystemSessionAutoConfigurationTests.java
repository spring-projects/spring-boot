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

package org.springframework.boot.cli.compiler.grape;

import java.io.File;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link SettingsXmlRepositorySystemSessionAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class SettingsXmlRepositorySystemSessionAutoConfigurationTests {

	@Mock
	private RepositorySystem repositorySystem;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void basicSessionCustomization() {
		assertSessionCustomization("src/test/resources/maven-settings/basic");
	}

	@Test
	void encryptedSettingsSessionCustomization() {
		assertSessionCustomization("src/test/resources/maven-settings/encrypted");
	}

	@Test
	void propertyInterpolation() {
		final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		given(this.repositorySystem.newLocalRepositoryManager(eq(session), any(LocalRepository.class)))
				.willAnswer((invocation) -> {
					LocalRepository localRepository = invocation.getArgument(1);
					return new SimpleLocalRepositoryManagerFactory().newInstance(session, localRepository);
				});
		TestPropertyValues.of("user.home:src/test/resources/maven-settings/property-interpolation", "foo:bar")
				.applyToSystemProperties(() -> {
					new SettingsXmlRepositorySystemSessionAutoConfiguration().apply(session,
							SettingsXmlRepositorySystemSessionAutoConfigurationTests.this.repositorySystem);
					return null;
				});
		assertThat(session.getLocalRepository().getBasedir().getAbsolutePath())
				.endsWith(File.separatorChar + "bar" + File.separatorChar + "repository");
	}

	private void assertSessionCustomization(String userHome) {
		final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		TestPropertyValues.of("user.home:" + userHome).applyToSystemProperties(() -> {
			new SettingsXmlRepositorySystemSessionAutoConfiguration().apply(session,
					SettingsXmlRepositorySystemSessionAutoConfigurationTests.this.repositorySystem);
			return null;
		});
		RemoteRepository repository = new RemoteRepository.Builder("my-server", "default", "https://maven.example.com")
				.build();
		assertMirrorSelectorConfiguration(session, repository);
		assertProxySelectorConfiguration(session, repository);
		assertAuthenticationSelectorConfiguration(session, repository);
	}

	private void assertProxySelectorConfiguration(DefaultRepositorySystemSession session, RemoteRepository repository) {
		Proxy proxy = session.getProxySelector().getProxy(repository);
		repository = new RemoteRepository.Builder(repository).setProxy(proxy).build();
		AuthenticationContext authenticationContext = AuthenticationContext.forProxy(session, repository);
		assertThat(proxy.getHost()).isEqualTo("proxy.example.com");
		assertThat(authenticationContext.get(AuthenticationContext.USERNAME)).isEqualTo("proxyuser");
		assertThat(authenticationContext.get(AuthenticationContext.PASSWORD)).isEqualTo("somepassword");
	}

	private void assertMirrorSelectorConfiguration(DefaultRepositorySystemSession session,
			RemoteRepository repository) {
		RemoteRepository mirror = session.getMirrorSelector().getMirror(repository);
		assertThat(mirror).as("Mirror configured for repository " + repository.getId()).isNotNull();
		assertThat(mirror.getHost()).isEqualTo("maven.example.com");
	}

	private void assertAuthenticationSelectorConfiguration(DefaultRepositorySystemSession session,
			RemoteRepository repository) {
		Authentication authentication = session.getAuthenticationSelector().getAuthentication(repository);
		repository = new RemoteRepository.Builder(repository).setAuthentication(authentication).build();
		AuthenticationContext authenticationContext = AuthenticationContext.forRepository(session, repository);
		assertThat(authenticationContext.get(AuthenticationContext.USERNAME)).isEqualTo("tester");
		assertThat(authenticationContext.get(AuthenticationContext.PASSWORD)).isEqualTo("secret");
	}

}
