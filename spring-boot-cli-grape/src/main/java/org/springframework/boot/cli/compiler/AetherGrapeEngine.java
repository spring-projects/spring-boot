/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * A {@link GrapeEngine} implementation that uses <a
 * href="http://eclipse.org/aether">Aether</a>, the dependency resolution system used by
 * Maven.
 * 
 * @author Andy Wilkinson
 */
@SuppressWarnings("rawtypes")
public class AetherGrapeEngine implements GrapeEngine {

	private static final String DEPENDENCY_MODULE = "module";

	private static final String DEPENDENCY_GROUP = "group";

	private static final String DEPENDENCY_VERSION = "version";

	private static final Collection<Exclusion> WILDCARD_EXCLUSION = Arrays
			.asList(new Exclusion("*", "*", "*", "*"));

	private final Artifact parentArtifact;

	private final ProgressReporter progressReporter = new ProgressReporter();

	private final ArtifactDescriptorReader artifactDescriptorReader;

	private final GroovyClassLoader defaultClassLoader;

	private final RepositorySystemSession repositorySystemSession;

	private final RepositorySystem repositorySystem;

	private final List<RemoteRepository> repositories;

	public AetherGrapeEngine(GroovyClassLoader classLoader, String parentGroupId,
			String parentArtifactId, String parentVersion) {
		this.defaultClassLoader = classLoader;
		this.parentArtifact = new DefaultArtifact(parentGroupId, parentArtifactId, "pom",
				parentVersion);

		DefaultServiceLocator mavenServiceLocator = MavenRepositorySystemUtils
				.newServiceLocator();
		mavenServiceLocator.addService(RepositorySystem.class,
				DefaultRepositorySystem.class);

		mavenServiceLocator.addService(RepositoryConnectorFactory.class,
				BasicRepositoryConnectorFactory.class);

		mavenServiceLocator.addService(TransporterFactory.class,
				HttpTransporterFactory.class);
		mavenServiceLocator.addService(TransporterFactory.class,
				FileTransporterFactory.class);

		this.repositorySystem = mavenServiceLocator.getService(RepositorySystem.class);

		DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils
				.newSession();
		repositorySystemSession.setTransferListener(new AbstractTransferListener() {

			@Override
			public void transferStarted(TransferEvent event)
					throws TransferCancelledException {
				AetherGrapeEngine.this.progressReporter.reportProgress();
			}

			@Override
			public void transferProgressed(TransferEvent event)
					throws TransferCancelledException {
				AetherGrapeEngine.this.progressReporter.reportProgress();
			}
		});

		repositorySystemSession.setRepositoryListener(new AbstractRepositoryListener() {
			@Override
			public void artifactResolved(RepositoryEvent event) {
				AetherGrapeEngine.this.progressReporter.reportProgress();
			}
		});

		LocalRepository localRepo = new LocalRepository(new File(
				System.getProperty("user.home"), ".m2/repository"));
		repositorySystemSession.setLocalRepositoryManager(this.repositorySystem
				.newLocalRepositoryManager(repositorySystemSession, localRepo));

		this.repositorySystemSession = repositorySystemSession;

		this.repositories = Arrays.asList(new RemoteRepository.Builder("central",
				"default", "http://repo1.maven.org/maven2/").build(),
				new RemoteRepository.Builder("spring-snapshot", "default",
						"http://repo.spring.io/snapshot").build(),
				new RemoteRepository.Builder("spring-milestone", "default",
						"http://repo.spring.io/milestone").build());

		this.artifactDescriptorReader = mavenServiceLocator
				.getService(ArtifactDescriptorReader.class);
	}

	@Override
	public Object grab(Map args) {
		return grab(args, args);
	}

	@Override
	public Object grab(Map args, Map... dependencyMaps) {
		List<Dependency> dependencies = createDependencies(dependencyMaps);

		try {
			List<File> files = resolve(dependencies);
			GroovyClassLoader classLoader = (GroovyClassLoader) args.get("classLoader");
			if (classLoader == null) {
				classLoader = this.defaultClassLoader;
			}
			for (File file : files) {
				classLoader.addURL(file.toURI().toURL());
			}
		}
		catch (ArtifactResolutionException ex) {
			throw new DependencyResolutionFailedException(ex);
		}
		catch (MalformedURLException ex) {
			throw new DependencyResolutionFailedException(ex);
		}
		return null;
	}

	private List<Dependency> createDependencies(Map<?, ?>... dependencyMaps) {
		List<Dependency> dependencies = new ArrayList<Dependency>(dependencyMaps.length);
		for (Map<?, ?> dependencyMap : dependencyMaps) {
			dependencies.add(createDependency(dependencyMap));
		}
		return dependencies;
	}

	private boolean isTransitive(Map<?, ?> dependencyMap) {
		Boolean transitive = (Boolean) dependencyMap.get("transitive");
		if (transitive == null) {
			transitive = true;
		}
		return transitive;
	}

	private Dependency createDependency(Map<?, ?> dependencyMap) {
		Artifact artifact = createArtifact(dependencyMap);

		Dependency dependency;

		if (!isTransitive(dependencyMap)) {
			dependency = new Dependency(artifact, JavaScopes.COMPILE, null,
					WILDCARD_EXCLUSION);
		}
		else {
			dependency = new Dependency(artifact, JavaScopes.COMPILE);
		}

		return dependency;
	}

	private Artifact createArtifact(Map<?, ?> dependencyMap) {
		String group = (String) dependencyMap.get(DEPENDENCY_GROUP);
		String module = (String) dependencyMap.get(DEPENDENCY_MODULE);
		String version = (String) dependencyMap.get(DEPENDENCY_VERSION);

		return new DefaultArtifact(group, module, "jar", version);
	}

	private List<File> resolve(List<Dependency> dependencies)
			throws ArtifactResolutionException {

		CollectRequest collectRequest = new CollectRequest((Dependency) null,
				dependencies, this.repositories);
		collectRequest.setManagedDependencies(getManagedDependencies());

		try {
			DependencyResult dependencyResult = this.repositorySystem
					.resolveDependencies(
							this.repositorySystemSession,
							new DependencyRequest(collectRequest, DependencyFilterUtils
									.classpathFilter(JavaScopes.COMPILE)));
			List<File> files = new ArrayList<File>();
			for (ArtifactResult result : dependencyResult.getArtifactResults()) {
				files.add(result.getArtifact().getFile());
			}

			return files;
		}
		catch (Exception ex) {
			throw new DependencyResolutionFailedException(ex);
		}
		finally {
			this.progressReporter.finished();
		}
	}

	private List<Dependency> getManagedDependencies() {
		ArtifactDescriptorRequest parentRequest = new ArtifactDescriptorRequest();
		parentRequest.setArtifact(this.parentArtifact);

		try {
			ArtifactDescriptorResult artifactDescriptorResult = this.artifactDescriptorReader
					.readArtifactDescriptor(this.repositorySystemSession, parentRequest);
			return artifactDescriptorResult.getManagedDependencies();
		}
		catch (ArtifactDescriptorException ex) {
			throw new DependencyResolutionFailedException(ex);
		}
	}

	@Override
	public Map<String, Map<String, List<String>>> enumerateGrapes() {
		throw new UnsupportedOperationException("Grape enumeration is not supported");
	}

	@Override
	public URI[] resolve(Map args, Map... dependencies) {
		throw new UnsupportedOperationException("Resolving to URIs is not supported");
	}

	@Override
	public URI[] resolve(Map args, List depsInfo, Map... dependencies) {
		throw new UnsupportedOperationException("Resolving to URIs is not supported");
	}

	@Override
	public Map[] listDependencies(ClassLoader classLoader) {
		throw new UnsupportedOperationException("Listing dependencies is not supported");
	}

	@Override
	public void addResolver(Map<String, Object> args) {
		throw new UnsupportedOperationException("Adding a resolver is not supported");
	}

	@Override
	public Object grab(String endorsedModule) {
		throw new UnsupportedOperationException(
				"Grabbing an endorsed module is not supported");
	}

	private static final class ProgressReporter {

		private static final long INITIAL_DELAY = TimeUnit.SECONDS.toMillis(3);

		private static final long PROGRESS_DELAY = TimeUnit.SECONDS.toMillis(1);

		private long startTime = System.currentTimeMillis();

		private long lastProgressTime = System.currentTimeMillis();

		private boolean started;

		private boolean finished;

		void reportProgress() {
			if (!this.finished
					&& System.currentTimeMillis() - this.startTime > INITIAL_DELAY) {
				if (!this.started) {
					this.started = true;
					System.out.print("Resolving dependencies..");
					this.lastProgressTime = System.currentTimeMillis();
				}
				else if (System.currentTimeMillis() - this.lastProgressTime > PROGRESS_DELAY) {
					System.out.print(".");
					this.lastProgressTime = System.currentTimeMillis();
				}
			}
		}

		void finished() {
			if (this.started && !this.finished) {
				this.finished = true;
				System.out.println("");
			}
		}
	}
}
