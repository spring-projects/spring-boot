/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.cli.compiler.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ConservativeAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

/**
 * An encapsulation of settings read from a user's Maven settings.xml.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 * @see MavenSettingsReader
 */
public class MavenSettings {

	private final boolean offline;

	private final MirrorSelector mirrorSelector;

	private final AuthenticationSelector authenticationSelector;

	private final ProxySelector proxySelector;

	private final String localRepository;

	private final List<Profile> activeProfiles;

	/**
	 * Create a new {@link MavenSettings} instance.
	 * @param settings the source settings
	 * @param decryptedSettings the decrypted settings
	 */
	public MavenSettings(Settings settings, SettingsDecryptionResult decryptedSettings) {
		this.offline = settings.isOffline();
		this.mirrorSelector = createMirrorSelector(settings);
		this.authenticationSelector = createAuthenticationSelector(decryptedSettings);
		this.proxySelector = createProxySelector(decryptedSettings);
		this.localRepository = settings.getLocalRepository();
		this.activeProfiles = determineActiveProfiles(settings);
	}

	private MirrorSelector createMirrorSelector(Settings settings) {
		DefaultMirrorSelector selector = new DefaultMirrorSelector();
		for (Mirror mirror : settings.getMirrors()) {
			selector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false,
					mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
		}
		return selector;
	}

	private AuthenticationSelector createAuthenticationSelector(
			SettingsDecryptionResult decryptedSettings) {
		DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
		for (Server server : decryptedSettings.getServers()) {
			AuthenticationBuilder auth = new AuthenticationBuilder();
			auth.addUsername(server.getUsername()).addPassword(server.getPassword());
			auth.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
			selector.add(server.getId(), auth.build());
		}
		return new ConservativeAuthenticationSelector(selector);
	}

	private ProxySelector createProxySelector(
			SettingsDecryptionResult decryptedSettings) {
		DefaultProxySelector selector = new DefaultProxySelector();
		for (Proxy proxy : decryptedSettings.getProxies()) {
			Authentication authentication = new AuthenticationBuilder()
					.addUsername(proxy.getUsername()).addPassword(proxy.getPassword())
					.build();
			selector.add(
					new org.eclipse.aether.repository.Proxy(proxy.getProtocol(),
							proxy.getHost(), proxy.getPort(), authentication),
					proxy.getNonProxyHosts());
		}
		return selector;
	}

	private List<Profile> determineActiveProfiles(Settings settings) {
		SpringBootCliModelProblemCollector problemCollector = new SpringBootCliModelProblemCollector();
		List<org.apache.maven.model.Profile> activeModelProfiles = createProfileSelector()
				.getActiveProfiles(createModelProfiles(settings.getProfiles()),
						new SpringBootCliProfileActivationContext(
								settings.getActiveProfiles()),
						problemCollector);
		if (!problemCollector.getProblems().isEmpty()) {
			throw new IllegalStateException(createFailureMessage(problemCollector));
		}
		List<Profile> activeProfiles = new ArrayList<>();
		Map<String, Profile> profiles = settings.getProfilesAsMap();
		for (org.apache.maven.model.Profile modelProfile : activeModelProfiles) {
			activeProfiles.add(profiles.get(modelProfile.getId()));
		}
		return activeProfiles;
	}

	private String createFailureMessage(
			SpringBootCliModelProblemCollector problemCollector) {
		StringWriter message = new StringWriter();
		PrintWriter printer = new PrintWriter(message);
		printer.println("Failed to determine active profiles:");
		for (ModelProblemCollectorRequest problem : problemCollector.getProblems()) {
			printer.println("    " + problem.getMessage() + (problem.getLocation() != null
					? " at " + problem.getLocation() : ""));
			if (problem.getException() != null) {
				printer.println(indentStackTrace(problem.getException(), "        "));
			}
		}
		return message.toString();
	}

	private String indentStackTrace(Exception ex, String indent) {
		return indentLines(printStackTrace(ex), indent);
	}

	private String printStackTrace(Exception ex) {
		StringWriter stackTrace = new StringWriter();
		PrintWriter printer = new PrintWriter(stackTrace);
		ex.printStackTrace(printer);
		return stackTrace.toString();
	}

	private String indentLines(String input, String indent) {
		StringWriter indented = new StringWriter();
		PrintWriter writer = new PrintWriter(indented);
		String line;
		BufferedReader reader = new BufferedReader(new StringReader(input));
		try {
			while ((line = reader.readLine()) != null) {
				writer.println(indent + line);
			}
		}
		catch (IOException ex) {
			return input;
		}
		return indented.toString();
	}

	private DefaultProfileSelector createProfileSelector() {
		DefaultProfileSelector selector = new DefaultProfileSelector();

		selector.addProfileActivator(new FileProfileActivator()
				.setPathTranslator(new DefaultPathTranslator()));
		selector.addProfileActivator(new JdkVersionProfileActivator());
		selector.addProfileActivator(new PropertyProfileActivator());
		selector.addProfileActivator(new OperatingSystemProfileActivator());
		return selector;
	}

	private List<org.apache.maven.model.Profile> createModelProfiles(
			List<Profile> profiles) {
		List<org.apache.maven.model.Profile> modelProfiles = new ArrayList<>();
		for (Profile profile : profiles) {
			org.apache.maven.model.Profile modelProfile = new org.apache.maven.model.Profile();
			modelProfile.setId(profile.getId());
			if (profile.getActivation() != null) {
				modelProfile
						.setActivation(createModelActivation(profile.getActivation()));
			}
			modelProfiles.add(modelProfile);
		}
		return modelProfiles;
	}

	private org.apache.maven.model.Activation createModelActivation(
			Activation activation) {
		org.apache.maven.model.Activation modelActivation = new org.apache.maven.model.Activation();
		modelActivation.setActiveByDefault(activation.isActiveByDefault());
		if (activation.getFile() != null) {
			ActivationFile activationFile = new ActivationFile();
			activationFile.setExists(activation.getFile().getExists());
			activationFile.setMissing(activation.getFile().getMissing());
			modelActivation.setFile(activationFile);
		}
		modelActivation.setJdk(activation.getJdk());
		if (activation.getOs() != null) {
			ActivationOS os = new ActivationOS();
			os.setArch(activation.getOs().getArch());
			os.setFamily(activation.getOs().getFamily());
			os.setName(activation.getOs().getName());
			os.setVersion(activation.getOs().getVersion());
			modelActivation.setOs(os);
		}
		if (activation.getProperty() != null) {
			ActivationProperty property = new ActivationProperty();
			property.setName(activation.getProperty().getName());
			property.setValue(activation.getProperty().getValue());
			modelActivation.setProperty(property);
		}
		return modelActivation;
	}

	public boolean getOffline() {
		return this.offline;
	}

	public MirrorSelector getMirrorSelector() {
		return this.mirrorSelector;
	}

	public AuthenticationSelector getAuthenticationSelector() {
		return this.authenticationSelector;
	}

	public ProxySelector getProxySelector() {
		return this.proxySelector;
	}

	public String getLocalRepository() {
		return this.localRepository;
	}

	public List<Profile> getActiveProfiles() {
		return this.activeProfiles;
	}

	private static final class SpringBootCliProfileActivationContext
			implements ProfileActivationContext {

		private final List<String> activeProfiles;

		SpringBootCliProfileActivationContext(List<String> activeProfiles) {
			this.activeProfiles = activeProfiles;
		}

		@Override
		public List<String> getActiveProfileIds() {
			return this.activeProfiles;
		}

		@Override
		public List<String> getInactiveProfileIds() {
			return Collections.emptyList();
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Map<String, String> getSystemProperties() {
			return (Map) System.getProperties();
		}

		@Override
		public Map<String, String> getUserProperties() {
			return Collections.emptyMap();
		}

		@Override
		public File getProjectDirectory() {
			return new File(".");
		}

		@Override
		public Map<String, String> getProjectProperties() {
			return Collections.emptyMap();
		}

	}

	private static final class SpringBootCliModelProblemCollector
			implements ModelProblemCollector {

		private final List<ModelProblemCollectorRequest> problems = new ArrayList<>();

		@Override
		public void add(ModelProblemCollectorRequest req) {
			this.problems.add(req);
		}

		List<ModelProblemCollectorRequest> getProblems() {
			return this.problems;
		}

	}

}
