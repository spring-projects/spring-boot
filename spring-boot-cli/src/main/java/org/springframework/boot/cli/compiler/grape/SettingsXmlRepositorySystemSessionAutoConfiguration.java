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
import org.apache.maven.settings.building.SettingsBuildingResult;
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
		SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();

		File userSettingsFile = new File(System.getProperty("user.home"),
				".m2/settings.xml");

		request.setUserSettingsFile(userSettingsFile);

		SettingsBuildingResult result;
		try {
			result = new DefaultSettingsBuilderFactory().newInstance().build(request);
		}
		catch (SettingsBuildingException e) {
			throw new IllegalStateException("Failed to build settings from "
					+ userSettingsFile, e);
		}

		return result.getEffectiveSettings();
	}

	private MirrorSelector createMirrorSelector(Settings settings) {
		DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
		for (Mirror mirror : settings.getMirrors()) {
			mirrorSelector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(),
					false, mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
		}
		return mirrorSelector;
	}

	private AuthenticationSelector createAuthenticationSelector(Settings settings) {
		DefaultAuthenticationSelector authenticationSelector = new DefaultAuthenticationSelector();

		for (Server server : settings.getServers()) {
			AuthenticationBuilder auth = new AuthenticationBuilder();
			auth.addUsername(server.getUsername()).addPassword(server.getPassword());
			auth.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
			authenticationSelector.add(server.getId(), auth.build());
		}

		return new ConservativeAuthenticationSelector(authenticationSelector);
	}

	private ProxySelector createProxySelector(Settings settings) {
		DefaultProxySelector proxySelector = new DefaultProxySelector();

		for (Proxy proxy : settings.getProxies()) {
			Authentication authentication = new AuthenticationBuilder()
					.addUsername(proxy.getUsername()).addPassword(proxy.getPassword())
					.build();

			proxySelector.add(
					new org.eclipse.aether.repository.Proxy(proxy.getProtocol(), proxy
							.getHost(), proxy.getPort(), authentication), proxy
							.getNonProxyHosts());
		}

		return proxySelector;
	}
}
