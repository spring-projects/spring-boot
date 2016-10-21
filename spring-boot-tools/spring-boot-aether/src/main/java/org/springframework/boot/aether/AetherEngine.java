/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.aether;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import org.springframework.util.StringUtils;

/**
 * A utility wrapper for <a href="http://eclipse.org/aether">Aether</a>, the dependency
 * resolution system used by Maven.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Dave Syer
 */
public class AetherEngine {

	private static ServiceLocator serviceLocator;

	private final DependencyManagementContext dependencyManagement;

	private final ProgressReporter progressReporter;

	private final DefaultRepositorySystemSession session;

	private final RepositorySystem repositorySystem;

	private final List<RemoteRepository> repositories;

	public static AetherEngine create(
			List<RepositoryConfiguration> repositoryConfigurations,
			DependencyManagementContext dependencyManagement) {

		RepositorySystem repositorySystem = getServiceLocator()
				.getService(RepositorySystem.class);

		DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils
				.newSession();

		ServiceLoader<RepositorySystemSessionConfiguration> autoConfigurations = ServiceLoader
				.load(RepositorySystemSessionConfiguration.class);

		for (RepositorySystemSessionConfiguration autoConfiguration : autoConfigurations) {
			autoConfiguration.apply(repositorySystemSession, repositorySystem);
		}

		new DefaultRepositorySystemSessionAutoConfiguration()
				.apply(repositorySystemSession, repositorySystem);

		return new AetherEngine(repositorySystem, repositorySystemSession,
				createRepositories(repositoryConfigurations),
				dependencyManagement);
	}

	AetherEngine(RepositorySystem repositorySystem,
			DefaultRepositorySystemSession repositorySystemSession,
			List<RemoteRepository> remoteRepositories,
			DependencyManagementContext dependencyManagement) {
		this.repositorySystem = repositorySystem;
		this.session = repositorySystemSession;
		this.dependencyManagement = dependencyManagement;
		this.repositories = new ArrayList<RemoteRepository>();
		List<RemoteRepository> remotes = new ArrayList<RemoteRepository>(
				remoteRepositories);
		Collections.reverse(remotes); // priority is reversed in addRepository
		for (RemoteRepository repository : remotes) {
			addRepository(repository);
		}
		this.progressReporter = getProgressReporter(this.session);
	}

	private ProgressReporter getProgressReporter(DefaultRepositorySystemSession session) {
		if (Boolean.getBoolean("groovy.grape.report.downloads")) {
			return new DetailedProgressReporter(session, System.out);
		}
		return new SummaryProgressReporter(session, System.out);
	}

	private List<Dependency> getDependencies(DependencyResult dependencyResult) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
			dependencies.add(
					new Dependency(artifactResult.getArtifact(), JavaScopes.COMPILE));
		}
		return dependencies;
	}

	private List<File> getFiles(DependencyResult dependencyResult) {
		List<File> files = new ArrayList<File>();
		for (ArtifactResult result : dependencyResult.getArtifactResults()) {
			files.add(result.getArtifact().getFile());
		}
		return files;
	}

	public void addRepository(RemoteRepository repository) {
		if (this.repositories.contains(repository)) {
			return;
		}
		repository = getPossibleMirror(repository);
		repository = applyProxy(repository);
		repository = applyAuthentication(repository);
		this.repositories.add(0, repository);
	}

	private RemoteRepository getPossibleMirror(RemoteRepository remoteRepository) {
		RemoteRepository mirror = this.session.getMirrorSelector()
				.getMirror(remoteRepository);
		if (mirror != null) {
			return mirror;
		}
		return remoteRepository;
	}

	private RemoteRepository applyProxy(RemoteRepository repository) {
		if (repository.getProxy() == null) {
			RemoteRepository.Builder builder = new RemoteRepository.Builder(repository);
			builder.setProxy(this.session.getProxySelector().getProxy(repository));
			repository = builder.build();
		}
		return repository;
	}

	private RemoteRepository applyAuthentication(RemoteRepository repository) {
		if (repository.getAuthentication() == null) {
			RemoteRepository.Builder builder = new RemoteRepository.Builder(repository);
			builder.setAuthentication(this.session.getAuthenticationSelector()
					.getAuthentication(repository));
			repository = builder.build();
		}
		return repository;
	}

	public List<File> resolve(List<Dependency> dependencies)
			throws DependencyResolutionFailedException {
		try {
			CollectRequest collectRequest = getCollectRequest(dependencies);
			DependencyRequest dependencyRequest = getDependencyRequest(collectRequest);
			DependencyResult result = this.repositorySystem
					.resolveDependencies(this.session, dependencyRequest);
			addManagedDependencies(result);
			return getFiles(result);
		}
		catch (Exception ex) {
			throw new DependencyResolutionFailedException(ex);
		}
		finally {
			this.progressReporter.finished();
		}
	}

	public void addDependencyManagementBoms(List<Dependency> boms) {
		for (Dependency bom : boms) {
			try {
				ArtifactDescriptorReader resolver = AetherEngine.getServiceLocator()
						.getService(ArtifactDescriptorReader.class);
				ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(
						bom.getArtifact(), this.repositories, null);
				ArtifactDescriptorResult descriptor = resolver
						.readArtifactDescriptor(this.session, request);
				List<Dependency> managedDependencies = descriptor
						.getManagedDependencies();
				this.dependencyManagement.addManagedDependencies(managedDependencies);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to build model for '" + bom
						+ "'. Is it a valid Maven bom?", ex);
			}
		}
	}

	private CollectRequest getCollectRequest(List<Dependency> dependencies) {
		List<Dependency> resolve = new ArrayList<Dependency>();
		for (Dependency dependency : dependencies) {
			String version = dependency.getArtifact().getVersion();
			String group = dependency.getArtifact().getGroupId();
			String module = dependency.getArtifact().getArtifactId();
			if (!StringUtils.hasText(version)) {
				version = this.dependencyManagement.getManagedVersion(group, module);
				if (version == null) {
					throw new IllegalStateException(
							"Cannot resolve version for " + dependency);
				}
				resolve.add(dependency
						.setArtifact(dependency.getArtifact().setVersion(version)));
			}
			else {
				resolve.add(dependency);
			}
		}
		CollectRequest collectRequest = new CollectRequest((Dependency) null, resolve,
				new ArrayList<RemoteRepository>(this.repositories));
		collectRequest
				.setManagedDependencies(this.dependencyManagement.getManagedDependencies());
		return collectRequest;
	}

	private DependencyRequest getDependencyRequest(CollectRequest collectRequest) {
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest,
				DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE,
						JavaScopes.RUNTIME));
		return dependencyRequest;
	}

	private void addManagedDependencies(DependencyResult result) {
		this.dependencyManagement.addManagedDependencies(getDependencies(result));
	}

	public static ServiceLocator getServiceLocator() {
		if (AetherEngine.serviceLocator == null) {
			DefaultServiceLocator locator = MavenRepositorySystemUtils
					.newServiceLocator();
			locator.addService(RepositorySystem.class, DefaultRepositorySystem.class);
			locator.addService(RepositoryConnectorFactory.class,
					BasicRepositoryConnectorFactory.class);
			locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
			locator.addService(TransporterFactory.class, FileTransporterFactory.class);
			AetherEngine.serviceLocator = locator;
		}
		return AetherEngine.serviceLocator;
	}

	private static List<RemoteRepository> createRepositories(
			List<RepositoryConfiguration> repositoryConfigurations) {
		List<RemoteRepository> repositories = new ArrayList<RemoteRepository>(
				repositoryConfigurations.size());
		for (RepositoryConfiguration repositoryConfiguration : repositoryConfigurations) {
			RemoteRepository.Builder builder = new RemoteRepository.Builder(
					repositoryConfiguration.getName(), "default",
					repositoryConfiguration.getUri().toASCIIString());

			if (!repositoryConfiguration.getSnapshotsEnabled()) {
				builder.setSnapshotPolicy(
						new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_NEVER,
								RepositoryPolicy.CHECKSUM_POLICY_IGNORE));
			}
			repositories.add(builder.build());
		}
		return repositories;
	}

}
