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

package org.springframework.boot.cli.compiler.grape;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.JavaScopes;

import org.springframework.boot.aether.AetherEngine;
import org.springframework.boot.aether.DependencyResolutionFailedException;

/**
 * A {@link GrapeEngine} implementation that uses
 * <a href="http://eclipse.org/aether">Aether</a>, the dependency resolution system used
 * by Maven.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SuppressWarnings("rawtypes")
public class AetherGrapeEngine implements GrapeEngine {

	private static final Collection<Exclusion> WILDCARD_EXCLUSION;

	static {
		List<Exclusion> exclusions = new ArrayList<Exclusion>();
		exclusions.add(new Exclusion("*", "*", "*", "*"));
		WILDCARD_EXCLUSION = Collections.unmodifiableList(exclusions);
	}

	private final DependencyResolutionContext resolutionContext;

	private final AetherEngine engine;

	private final GroovyClassLoader classLoader;

	public AetherGrapeEngine(GroovyClassLoader classLoader, AetherEngine engine,
			DependencyResolutionContext resolutionContext) {
		this.classLoader = classLoader;
		this.engine = engine;
		this.resolutionContext = resolutionContext;
	}

	@Override
	public Object grab(Map args) {
		return grab(args, args);
	}

	@Override
	public Object grab(Map args, Map... dependencyMaps) {
		List<Exclusion> exclusions = createExclusions(args);
		List<Dependency> dependencies = createDependencies(dependencyMaps, exclusions);
		try {
			List<File> files = resolve(dependencies);
			GroovyClassLoader classLoader = getClassLoader(args);
			for (File file : files) {
				classLoader.addURL(file.toURI().toURL());
			}
		}
		catch (MalformedURLException ex) {
			throw new DependencyResolutionFailedException(ex);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<Exclusion> createExclusions(Map<?, ?> args) {
		List<Exclusion> exclusions = new ArrayList<Exclusion>();
		if (args != null) {
			List<Map<String, Object>> exclusionMaps = (List<Map<String, Object>>) args
					.get("excludes");
			if (exclusionMaps != null) {
				for (Map<String, Object> exclusionMap : exclusionMaps) {
					exclusions.add(createExclusion(exclusionMap));
				}
			}
		}
		return exclusions;
	}

	private Exclusion createExclusion(Map<String, Object> exclusionMap) {
		String group = (String) exclusionMap.get("group");
		String module = (String) exclusionMap.get("module");
		return new Exclusion(group, module, "*", "*");
	}

	private List<Dependency> createDependencies(Map<?, ?>[] dependencyMaps,
			List<Exclusion> exclusions) {
		List<Dependency> dependencies = new ArrayList<Dependency>(dependencyMaps.length);
		for (Map<?, ?> dependencyMap : dependencyMaps) {
			dependencies.add(createDependency(dependencyMap, exclusions));
		}
		return dependencies;
	}

	private Dependency createDependency(Map<?, ?> dependencyMap,
			List<Exclusion> exclusions) {
		Artifact artifact = createArtifact(dependencyMap);
		if (isTransitive(dependencyMap)) {
			return new Dependency(artifact, JavaScopes.COMPILE, false, exclusions);
		}
		return new Dependency(artifact, JavaScopes.COMPILE, null, WILDCARD_EXCLUSION);
	}

	private Artifact createArtifact(Map<?, ?> dependencyMap) {
		String group = (String) dependencyMap.get("group");
		String module = (String) dependencyMap.get("module");
		String version = (String) dependencyMap.get("version");
		if (version == null) {
			version = this.resolutionContext.getManagedVersion(group, module);
		}
		String classifier = (String) dependencyMap.get("classifier");
		String type = determineType(dependencyMap);
		return new DefaultArtifact(group, module, classifier, type, version);
	}

	private String determineType(Map<?, ?> dependencyMap) {
		String type = (String) dependencyMap.get("type");
		String ext = (String) dependencyMap.get("ext");
		if (type == null) {
			type = ext;
			if (type == null) {
				type = "jar";
			}
		}
		else if (ext != null && !type.equals(ext)) {
			throw new IllegalArgumentException(
					"If both type and ext are specified they must have the same value");
		}
		return type;
	}

	private boolean isTransitive(Map<?, ?> dependencyMap) {
		Boolean transitive = (Boolean) dependencyMap.get("transitive");
		return (transitive == null ? true : transitive);
	}

	private GroovyClassLoader getClassLoader(Map args) {
		GroovyClassLoader classLoader = (GroovyClassLoader) args.get("classLoader");
		return (classLoader == null ? this.classLoader : classLoader);
	}

	@Override
	public void addResolver(Map<String, Object> args) {
		String name = (String) args.get("name");
		String root = (String) args.get("root");
		RemoteRepository.Builder builder = new RemoteRepository.Builder(name, "default",
				root);
		RemoteRepository repository = builder.build();
		addRepository(repository);
	}

	protected void addRepository(RemoteRepository repository) {
		this.engine.addRepository(repository);
	}

	@Override
	public Map<String, Map<String, List<String>>> enumerateGrapes() {
		throw new UnsupportedOperationException("Grape enumeration is not supported");
	}

	@Override
	public URI[] resolve(Map args, Map... dependencyMaps) {
		return this.resolve(args, null, dependencyMaps);
	}

	@Override
	public URI[] resolve(Map args, List depsInfo, Map... dependencyMaps) {
		List<Exclusion> exclusions = createExclusions(args);
		List<Dependency> dependencies = createDependencies(dependencyMaps, exclusions);
		List<File> files = resolve(dependencies);
		List<URI> uris = new ArrayList<URI>(files.size());
		for (File file : files) {
			uris.add(file.toURI());
		}
		return uris.toArray(new URI[uris.size()]);
	}

	private List<File> resolve(List<Dependency> dependencies)
			throws DependencyResolutionFailedException {
		return this.engine.resolve(dependencies);
	}

	@Override
	public Map[] listDependencies(ClassLoader classLoader) {
		throw new UnsupportedOperationException("Listing dependencies is not supported");
	}

	@Override
	public Object grab(String endorsedModule) {
		throw new UnsupportedOperationException(
				"Grabbing an endorsed module is not supported");
	}
}
