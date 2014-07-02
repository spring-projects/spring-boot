/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.compiler.grape;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link SettingsXmlRepositorySystemSessionAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@RunWith(MockitoJUnitRunner.class)
public class SettingsXmlRepositorySystemSessionAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private RepositorySystem repositorySystem;

	@Mock
	LocalRepositoryManager localRepositoryManager;

	@Test
	public void basicSessionCustomization() throws SettingsBuildingException {
		assertSessionCustomization("src/test/resources/maven-settings/basic");
	}

	@Test
	public void encryptedSettingsSessionCustomization() throws SettingsBuildingException {
		assertSessionCustomization("src/test/resources/maven-settings/encrypted");
	}

	private void assertSessionCustomization(String userHome) {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

		new SettingsXmlRepositorySystemSessionAutoConfiguration(userHome).apply(session,
				this.repositorySystem);

		RemoteRepository repository = new RemoteRepository.Builder("my-server",
				"default", "http://maven.example.com").build();

		assertMirrorSelectorConfiguration(session, repository);
		assertProxySelectorConfiguration(session, repository);
		assertAuthenticationSelectorConfiguration(session, repository);
	}

	private void assertProxySelectorConfiguration(DefaultRepositorySystemSession session,
			RemoteRepository repository) {
		Proxy proxy = session.getProxySelector().getProxy(repository);
		repository = new RemoteRepository.Builder(repository).setProxy(proxy).build();
		AuthenticationContext authenticationContext = AuthenticationContext.forProxy(
				session, repository);
		assertEquals("proxy.example.com", proxy.getHost());
		assertEquals("proxyuser",
				authenticationContext.get(AuthenticationContext.USERNAME));
		assertEquals("somepassword",
				authenticationContext.get(AuthenticationContext.PASSWORD));
	}

	private void assertMirrorSelectorConfiguration(
			DefaultRepositorySystemSession session, RemoteRepository repository) {
		RemoteRepository mirror = session.getMirrorSelector().getMirror(repository);
		assertNotNull("No mirror configured for repository " + repository.getId(), mirror);
		assertEquals("maven.example.com", mirror.getHost());
	}

	private void assertAuthenticationSelectorConfiguration(
			DefaultRepositorySystemSession session, RemoteRepository repository) {
		Authentication authentication = session.getAuthenticationSelector()
				.getAuthentication(repository);

		repository = new RemoteRepository.Builder(repository).setAuthentication(
				authentication).build();

		AuthenticationContext authenticationContext = AuthenticationContext
				.forRepository(session, repository);

		assertEquals("tester", authenticationContext.get(AuthenticationContext.USERNAME));
		assertEquals("secret", authenticationContext.get(AuthenticationContext.PASSWORD));
	}
}
