/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;
import org.springframework.boot.cli.compiler.maven.MavenSettings;
import org.springframework.boot.cli.compiler.maven.MavenSettingsReader;
import org.springframework.util.StringUtils;

/**
 * Factory used to create {@link RepositoryConfiguration}s.
 *
 * @author Andy Wilkinson
 * @author Dave Syer
 */
public final class RepositoryConfigurationFactory {

	private static final RepositoryConfiguration MAVEN_CENTRAL = new RepositoryConfiguration(
			"central", URI.create("https://repo.maven.apache.org/maven2/"), false);

	private static final RepositoryConfiguration SPRING_MILESTONE = new RepositoryConfiguration(
			"spring-milestone", URI.create("https://repo.spring.io/milestone"), false);

	private static final RepositoryConfiguration SPRING_SNAPSHOT = new RepositoryConfiguration(
			"spring-snapshot", URI.create("https://repo.spring.io/snapshot"), true);

	private RepositoryConfigurationFactory() {
	}

	/**
	 * Create a new default repository configuration.
	 * @return the newly-created default repository configuration
	 */
	public static List<RepositoryConfiguration> createDefaultRepositoryConfiguration() {
		MavenSettings mavenSettings = new MavenSettingsReader().readSettings();
		List<RepositoryConfiguration> repositoryConfiguration = new ArrayList<RepositoryConfiguration>();
		repositoryConfiguration.add(MAVEN_CENTRAL);
		if (!Boolean.getBoolean("disableSpringSnapshotRepos")) {
			repositoryConfiguration.add(SPRING_MILESTONE);
			repositoryConfiguration.add(SPRING_SNAPSHOT);
		}
		addDefaultCacheAsRepository(mavenSettings.getLocalRepository(),
				repositoryConfiguration);
		addActiveProfileRepositories(mavenSettings.getActiveProfiles(),
				repositoryConfiguration);
		return repositoryConfiguration;
	}

	private static void addDefaultCacheAsRepository(String localRepository,
			List<RepositoryConfiguration> repositoryConfiguration) {
		RepositoryConfiguration repository = new RepositoryConfiguration("local",
				getLocalRepositoryDirectory(localRepository).toURI(), true);
		if (!repositoryConfiguration.contains(repository)) {
			repositoryConfiguration.add(0, repository);
		}
	}

	private static void addActiveProfileRepositories(List<Profile> activeProfiles,
			List<RepositoryConfiguration> configurations) {
		for (Profile activeProfile : activeProfiles) {
			Interpolator interpolator = new RegexBasedInterpolator();
			interpolator.addValueSource(
					new PropertiesBasedValueSource(activeProfile.getProperties()));
			for (Repository repository : activeProfile.getRepositories()) {
				configurations.add(getRepositoryConfiguration(interpolator, repository));
			}
		}
	}

	private static RepositoryConfiguration getRepositoryConfiguration(
			Interpolator interpolator, Repository repository) {
		String name = interpolate(interpolator, repository.getId());
		String url = interpolate(interpolator, repository.getUrl());
		boolean snapshotsEnabled = false;
		if (repository.getSnapshots() != null) {
			snapshotsEnabled = repository.getSnapshots().isEnabled();
		}
		return new RepositoryConfiguration(name, URI.create(url), snapshotsEnabled);
	}

	private static String interpolate(Interpolator interpolator, String value) {
		try {
			return interpolator.interpolate(value);
		}
		catch (InterpolationException ex) {
			return value;
		}
	}

	private static File getLocalRepositoryDirectory(String localRepository) {
		if (StringUtils.hasText(localRepository)) {
			return new File(localRepository);
		}
		return new File(getM2HomeDirectory(), "repository");
	}

	private static File getM2HomeDirectory() {
		String mavenRoot = System.getProperty("maven.home");
		if (StringUtils.hasLength(mavenRoot)) {
			return new File(mavenRoot);
		}
		return new File(System.getProperty("user.home"), ".m2");
	}

}
