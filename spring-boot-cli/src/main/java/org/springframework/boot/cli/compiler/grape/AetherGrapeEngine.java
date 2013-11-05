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

package org.springframework.boot.cli.compiler.grape;

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

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
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
 * A {@link GrapeEngine} implementation that uses <a
 * href="http://eclipse.org/aether">Aether</a>, the dependency resolution system used by
 * Maven.
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SuppressWarnings("rawtypes")
public class AetherGrapeEngine implements GrapeEngine {

	private static final Collection<Exclusion> WILDCARD_EXCLUSION = Arrays
			.asList(new Exclusion("*", "*", "*", "*"));

	private final ProgressReporter progressReporter;

	private final GroovyClassLoader classLoader;

	private final RepositorySystemSession session;

	private final RepositorySystem repositorySystem;

	private final List<RemoteRepository> repositories;

	public AetherGrapeEngine(GroovyClassLoader classLoader) {
		this.classLoader = classLoader;
		this.repositorySystem = createServiceLocator().getService(RepositorySystem.class);
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		LocalRepository localRepository = new LocalRepository(getM2RepoDirectory());
		LocalRepositoryManager localRepositoryManager = this.repositorySystem
				.newLocalRepositoryManager(session, localRepository);
		session.setLocalRepositoryManager(localRepositoryManager);
		this.session = session;
		this.repositories = getRemoteRepositories();
		this.progressReporter = new ProgressReporter(session);
	}

	private ServiceLocator createServiceLocator() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositorySystem.class, DefaultRepositorySystem.class);
		locator.addService(RepositoryConnectorFactory.class,
				BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		return locator;
	}

	private File getM2RepoDirectory() {
		return new File(getM2HomeDirectory(), "repository");
	}

	private File getM2HomeDirectory() {
		String grapeRoot = System.getProperty("grape.root");
		if (StringUtils.hasLength(grapeRoot)) {
			return new File(grapeRoot);
		}
		return new File(System.getProperty("user.home"), ".m2");
	}

	private List<RemoteRepository> getRemoteRepositories() {
		List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
		addRemoteRepository(repositories, "central", "http://repo1.maven.org/maven2/");
		if (!Boolean.getBoolean("disableSpringSnapshotRepos")) {
			addRemoteRepository(repositories, "spring-snapshot",
					"http://repo.spring.io/snapshot");
			addRemoteRepository(repositories, "spring-milestone",
					"http://repo.spring.io/milestone");
		}
		return repositories;
	}

	private void addRemoteRepository(List<RemoteRepository> repositories, String id,
			String url) {
		repositories.add(new RemoteRepository.Builder(id, "default", url).build());
	}

	@Override
	public Object grab(Map args) {
		return grab(args, args);
	}

	@Override
	public Object grab(Map args, Map... dependencyMaps) {
		try {
			List<Dependency> dependencies = createDependencies(dependencyMaps);
			List<File> files = resolve(dependencies);
			GroovyClassLoader classLoader = getClassLoader(args);
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

	private GroovyClassLoader getClassLoader(Map args) {
		GroovyClassLoader classLoader = (GroovyClassLoader) args.get("classLoader");
		return (classLoader == null ? this.classLoader : classLoader);
	}

	private List<Dependency> createDependencies(Map<?, ?>... dependencyMaps) {
		List<Dependency> dependencies = new ArrayList<Dependency>(dependencyMaps.length);
		for (Map<?, ?> dependencyMap : dependencyMaps) {
			dependencies.add(createDependency(dependencyMap));
		}
		return dependencies;
	}

	private Dependency createDependency(Map<?, ?> dependencyMap) {
		Artifact artifact = createArtifact(dependencyMap);
		if (isTransitive(dependencyMap)) {
			return new Dependency(artifact, JavaScopes.COMPILE);
		}
		return new Dependency(artifact, JavaScopes.COMPILE, null, WILDCARD_EXCLUSION);
	}

	private Artifact createArtifact(Map<?, ?> dependencyMap) {
		String group = (String) dependencyMap.get("group");
		String module = (String) dependencyMap.get("module");
		String version = (String) dependencyMap.get("version");
		return new DefaultArtifact(group, module, "jar", version);
	}

	private boolean isTransitive(Map<?, ?> dependencyMap) {
		Boolean transitive = (Boolean) dependencyMap.get("transitive");
		return (transitive == null ? true : transitive);
	}

	private List<File> resolve(List<Dependency> dependencies)
			throws ArtifactResolutionException {
		try {
			CollectRequest collectRequest = new CollectRequest((Dependency) null,
					dependencies, this.repositories);
			DependencyRequest dependencyRequest = new DependencyRequest(collectRequest,
					DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE));
			DependencyResult dependencyResult = this.repositorySystem
					.resolveDependencies(this.session, dependencyRequest);
			return getFiles(dependencyResult);
		}
		catch (Exception ex) {
			throw new DependencyResolutionFailedException(ex);
		}
		finally {
			this.progressReporter.finished();
		}
	}

	private List<File> getFiles(DependencyResult dependencyResult) {
		List<File> files = new ArrayList<File>();
		for (ArtifactResult result : dependencyResult.getArtifactResults()) {
			files.add(result.getArtifact().getFile());
		}
		return files;
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
}
