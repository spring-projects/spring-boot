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

package org.springframework.boot.cli.compiler.grape;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;

import org.springframework.boot.cli.compiler.maven.MavenSettings;
import org.springframework.boot.cli.compiler.maven.MavenSettingsReader;

/**
 * Auto-configuration for a RepositorySystemSession that uses Maven's settings.xml to
 * determine the configuration settings.
 *
 * @author Andy Wilkinson
 */
public class SettingsXmlRepositorySystemSessionAutoConfiguration
		implements RepositorySystemSessionAutoConfiguration {

	@Override
	public void apply(DefaultRepositorySystemSession session,
			RepositorySystem repositorySystem) {
		MavenSettings settings = getSettings(session);
		String localRepository = settings.getLocalRepository();
		if (localRepository != null) {
			session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(
					session, new LocalRepository(localRepository)));
		}
	}

	private MavenSettings getSettings(DefaultRepositorySystemSession session) {
		MavenSettings settings = new MavenSettingsReader().readSettings();
		session.setOffline(settings.getOffline());
		session.setMirrorSelector(settings.getMirrorSelector());
		session.setAuthenticationSelector(settings.getAuthenticationSelector());
		session.setProxySelector(settings.getProxySelector());
		return settings;
	}

}
