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

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ConservativeAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.springframework.boot.cli.util.Log;

/**
 * Auto-configuration for a RepositorySystemSession that uses Maven's settings.xml to
 * determine the configuration settings
 *
 * @author Andy Wilkinson
 */
public class SettingsXmlRepositorySystemSessionAutoConfiguration implements
		RepositorySystemSessionAutoConfiguration {

	private final String homeDir;

	public SettingsXmlRepositorySystemSessionAutoConfiguration() {
		this(System.getProperty("user.home"));
	}

	SettingsXmlRepositorySystemSessionAutoConfiguration(String homeDir) {
		this.homeDir = homeDir;
	}

	@Override
	public void apply(DefaultRepositorySystemSession session,
			RepositorySystem repositorySystem) {

		Settings settings = loadSettings();
		SettingsDecryptionResult decryptionResult = decryptSettings(settings);
		if (!decryptionResult.getProblems().isEmpty()) {
			Log.error("Maven settings decryption failed. Some Maven repositories may be inaccessible");
			// Continue - the encrypted credentials may not be used
		}

		session.setOffline(settings.isOffline());
		session.setMirrorSelector(createMirrorSelector(settings));
		session.setAuthenticationSelector(createAuthenticationSelector(decryptionResult
				.getServers()));
		session.setProxySelector(createProxySelector(decryptionResult.getProxies()));

		String localRepository = settings.getLocalRepository();
		if (localRepository != null) {
			session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(
					session, new LocalRepository(localRepository)));
		}
	}

	private Settings loadSettings() {
		File settingsFile = new File(this.homeDir, ".m2/settings.xml");
		SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		request.setUserSettingsFile(settingsFile);
		try {
			return new DefaultSettingsBuilderFactory().newInstance().build(request)
					.getEffectiveSettings();
		}
		catch (SettingsBuildingException ex) {
			throw new IllegalStateException("Failed to build settings from "
					+ settingsFile, ex);
		}
	}

	private SettingsDecryptionResult decryptSettings(Settings settings) {
		DefaultSettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(
				settings);

		return createSettingsDecrypter().decrypt(request);
	}

	private SettingsDecrypter createSettingsDecrypter() {
		SettingsDecrypter settingsDecrypter = new DefaultSettingsDecrypter();
		setField(DefaultSettingsDecrypter.class, "securityDispatcher", settingsDecrypter,
				new SpringBootSecDispatcher());
		return settingsDecrypter;
	}

	private void setField(Class<?> clazz, String fieldName, Object target, Object value) {
		try {
			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to set field '" + fieldName
					+ "' on '" + target + "'", e);
		}
	}

	private MirrorSelector createMirrorSelector(Settings settings) {
		DefaultMirrorSelector selector = new DefaultMirrorSelector();
		for (Mirror mirror : settings.getMirrors()) {
			selector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false,
					mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
		}
		return selector;
	}

	private AuthenticationSelector createAuthenticationSelector(List<Server> servers) {
		DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
		for (Server server : servers) {
			AuthenticationBuilder auth = new AuthenticationBuilder();
			auth.addUsername(server.getUsername()).addPassword(server.getPassword());
			auth.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
			selector.add(server.getId(), auth.build());
		}
		return new ConservativeAuthenticationSelector(selector);
	}

	private ProxySelector createProxySelector(List<Proxy> proxies) {
		DefaultProxySelector selector = new DefaultProxySelector();
		for (Proxy proxy : proxies) {
			Authentication authentication = new AuthenticationBuilder()
					.addUsername(proxy.getUsername()).addPassword(proxy.getPassword())
					.build();
			selector.add(new org.eclipse.aether.repository.Proxy(proxy.getProtocol(),
					proxy.getHost(), proxy.getPort(), authentication), proxy
					.getNonProxyHosts());
		}
		return selector;
	}

	private class SpringBootSecDispatcher extends DefaultSecDispatcher {

		public SpringBootSecDispatcher() {
			this._configurationFile = new File(
					SettingsXmlRepositorySystemSessionAutoConfiguration.this.homeDir,
					".m2/settings-security.xml").getAbsolutePath();
			try {
				this._cipher = new DefaultPlexusCipher();
			}
			catch (PlexusCipherException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
