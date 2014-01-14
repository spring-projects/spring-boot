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

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
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

/**
 * Auto-configuration for a RepositorySystemSession that uses Maven's settings.xml to
 * determine the configuration settings
 * 
 * @author Andy Wilkinson
 */
public class SettingsXmlRepositorySystemSessionAutoConfiguration implements
		RepositorySystemSessionAutoConfiguration {

	private static final String HOME_DIR = System.getProperty("user.home");

	@Override
	public void apply(DefaultRepositorySystemSession session,
			RepositorySystem repositorySystem) {

		Settings settings = loadSettings();

		session.setOffline(settings.isOffline());
		session.setMirrorSelector(createMirrorSelector(settings));
		session.setAuthenticationSelector(createAuthenticationSelector(settings));
		session.setProxySelector(createProxySelector(settings));

		String localRepository = settings.getLocalRepository();
		if (localRepository != null) {
			session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(
					session, new LocalRepository(localRepository)));
		}
	}

	private Settings loadSettings() {
		File settingsFile = new File(HOME_DIR, ".m2/settings.xml");
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

	private MirrorSelector createMirrorSelector(Settings settings) {
		DefaultMirrorSelector selector = new DefaultMirrorSelector();
		for (Mirror mirror : settings.getMirrors()) {
			selector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false,
					mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
		}
		return selector;
	}

	private AuthenticationSelector createAuthenticationSelector(Settings settings) {
		DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
		for (Server server : settings.getServers()) {
			AuthenticationBuilder auth = new AuthenticationBuilder();
			auth.addUsername(server.getUsername()).addPassword(server.getPassword());
			auth.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
			selector.add(server.getId(), auth.build());
		}
		return new ConservativeAuthenticationSelector(selector);
	}

	private ProxySelector createProxySelector(Settings settings) {
		DefaultProxySelector selector = new DefaultProxySelector();
		for (Proxy proxy : settings.getProxies()) {
			Authentication authentication = new AuthenticationBuilder()
					.addUsername(proxy.getUsername()).addPassword(proxy.getPassword())
					.build();
			selector.add(new org.eclipse.aether.repository.Proxy(proxy.getProtocol(),
					proxy.getHost(), proxy.getPort(), authentication), proxy
					.getNonProxyHosts());
		}
		return selector;
	}
}
