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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.lang.GroovyClassLoader;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Test;

import org.springframework.boot.cli.compiler.dependencies.SpringBootDependenciesDependencyManagement;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AetherGrapeEngine}.
 *
 * @author Andy Wilkinson
 */
public class AetherGrapeEngineTests {

	private final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

	private final RepositoryConfiguration springMilestones = new RepositoryConfiguration(
			"spring-milestones", URI.create("https://repo.spring.io/milestone"), false);

	private AetherGrapeEngine createGrapeEngine(
			RepositoryConfiguration... additionalRepositories) {
		List<RepositoryConfiguration> repositoryConfigurations = new ArrayList<>();
		repositoryConfigurations.add(new RepositoryConfiguration("central",
				URI.create("http://repo1.maven.org/maven2"), false));
		repositoryConfigurations.addAll(Arrays.asList(additionalRepositories));
		DependencyResolutionContext dependencyResolutionContext = new DependencyResolutionContext();
		dependencyResolutionContext.addDependencyManagement(
				new SpringBootDependenciesDependencyManagement());
		return AetherGrapeEngineFactory.create(this.groovyClassLoader,
				repositoryConfigurations, dependencyResolutionContext, false);
	}

	@Test
	public void dependencyResolution() {
		Map<String, Object> args = new HashMap<>();
		createGrapeEngine(this.springMilestones).grab(args,
				createDependency("org.springframework", "spring-jdbc", null));
		assertThat(this.groovyClassLoader.getURLs()).hasSize(5);
	}

	@Test
	public void proxySelector() {
		doWithCustomUserHome(() -> {
			AetherGrapeEngine grapeEngine = createGrapeEngine();
			DefaultRepositorySystemSession session = (DefaultRepositorySystemSession) ReflectionTestUtils
					.getField(grapeEngine, "session");

			assertThat(session.getProxySelector() instanceof CompositeProxySelector)
					.isTrue();
		});
	}

	@Test
	public void repositoryMirrors() {
		doWithCustomUserHome(() -> {
			List<RemoteRepository> repositories = getRepositories();
			assertThat(repositories).hasSize(1);
			assertThat(repositories.get(0).getId()).isEqualTo("central-mirror");
		});
	}

	@Test
	public void repositoryAuthentication() {
		doWithCustomUserHome(() -> {
			List<RemoteRepository> repositories = getRepositories();
			assertThat(repositories).hasSize(1);
			Authentication authentication = repositories.get(0).getAuthentication();
			assertThat(authentication).isNotNull();
		});
	}

	@Test
	public void dependencyResolutionWithExclusions() {
		Map<String, Object> args = new HashMap<>();
		args.put("excludes",
				Arrays.asList(createExclusion("org.springframework", "spring-core")));

		createGrapeEngine(this.springMilestones).grab(args,
				createDependency("org.springframework", "spring-jdbc", "3.2.4.RELEASE"),
				createDependency("org.springframework", "spring-beans", "3.2.4.RELEASE"));

		assertThat(this.groovyClassLoader.getURLs().length).isEqualTo(3);
	}

	@Test
	public void nonTransitiveDependencyResolution() {
		Map<String, Object> args = new HashMap<>();

		createGrapeEngine().grab(args, createDependency("org.springframework",
				"spring-jdbc", "3.2.4.RELEASE", false));

		assertThat(this.groovyClassLoader.getURLs().length).isEqualTo(1);
	}

	@Test
	public void dependencyResolutionWithCustomClassLoader() {
		Map<String, Object> args = new HashMap<>();
		GroovyClassLoader customClassLoader = new GroovyClassLoader();
		args.put("classLoader", customClassLoader);

		createGrapeEngine(this.springMilestones).grab(args,
				createDependency("org.springframework", "spring-jdbc", null));

		assertThat(this.groovyClassLoader.getURLs().length).isEqualTo(0);
		assertThat(customClassLoader.getURLs().length).isEqualTo(5);
	}

	@Test
	public void resolutionWithCustomResolver() {
		Map<String, Object> args = new HashMap<>();
		AetherGrapeEngine grapeEngine = this.createGrapeEngine();
		grapeEngine
				.addResolver(createResolver("restlet.org", "http://maven.restlet.org"));
		grapeEngine.grab(args, createDependency("org.restlet", "org.restlet", "1.1.6"));
		assertThat(this.groovyClassLoader.getURLs().length).isEqualTo(1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void differingTypeAndExt() {
		Map<String, Object> dependency = createDependency("org.grails",
				"grails-dependencies", "2.4.0");
		dependency.put("type", "foo");
		dependency.put("ext", "bar");
		createGrapeEngine().grab(Collections.emptyMap(), dependency);
	}

	@Test
	public void pomDependencyResolutionViaType() {
		Map<String, Object> args = new HashMap<>();
		Map<String, Object> dependency = createDependency("org.springframework",
				"spring-framework-bom", "4.0.5.RELEASE");
		dependency.put("type", "pom");
		createGrapeEngine().grab(args, dependency);
		URL[] urls = this.groovyClassLoader.getURLs();
		assertThat(urls.length).isEqualTo(1);
		assertThat(urls[0].toExternalForm().endsWith(".pom")).isTrue();
	}

	@Test
	public void pomDependencyResolutionViaExt() {
		Map<String, Object> args = new HashMap<>();
		Map<String, Object> dependency = createDependency("org.springframework",
				"spring-framework-bom", "4.0.5.RELEASE");
		dependency.put("ext", "pom");
		createGrapeEngine().grab(args, dependency);
		URL[] urls = this.groovyClassLoader.getURLs();
		assertThat(urls.length).isEqualTo(1);
		assertThat(urls[0].toExternalForm().endsWith(".pom")).isTrue();
	}

	@Test
	public void resolutionWithClassifier() {
		Map<String, Object> args = new HashMap<>();

		Map<String, Object> dependency = createDependency("org.springframework",
				"spring-jdbc", "3.2.4.RELEASE", false);
		dependency.put("classifier", "sources");
		createGrapeEngine().grab(args, dependency);

		URL[] urls = this.groovyClassLoader.getURLs();
		assertThat(urls.length).isEqualTo(1);
		assertThat(urls[0].toExternalForm().endsWith("-sources.jar")).isTrue();
	}

	@SuppressWarnings("unchecked")
	private List<RemoteRepository> getRepositories() {
		AetherGrapeEngine grapeEngine = createGrapeEngine();
		return (List<RemoteRepository>) ReflectionTestUtils.getField(grapeEngine,
				"repositories");
	}

	private Map<String, Object> createDependency(String group, String module,
			String version) {
		Map<String, Object> dependency = new HashMap<>();
		dependency.put("group", group);
		dependency.put("module", module);
		dependency.put("version", version);
		return dependency;
	}

	private Map<String, Object> createDependency(String group, String module,
			String version, boolean transitive) {
		Map<String, Object> dependency = createDependency(group, module, version);
		dependency.put("transitive", transitive);
		return dependency;
	}

	private Map<String, Object> createResolver(String name, String url) {
		Map<String, Object> resolver = new HashMap<>();
		resolver.put("name", name);
		resolver.put("root", url);
		return resolver;
	}

	private Map<String, Object> createExclusion(String group, String module) {
		Map<String, Object> exclusion = new HashMap<>();
		exclusion.put("group", group);
		exclusion.put("module", module);
		return exclusion;
	}

	private void doWithCustomUserHome(Runnable action) {
		doWithSystemProperty("user.home",
				new File("src/test/resources").getAbsolutePath(), action);
	}

	private void doWithSystemProperty(String key, String value, Runnable action) {
		String previousValue = setOrClearSystemProperty(key, value);
		try {
			action.run();
		}
		finally {
			setOrClearSystemProperty(key, previousValue);
		}
	}

	private String setOrClearSystemProperty(String key, String value) {
		if (value != null) {
			return System.setProperty(key, value);
		}
		return System.clearProperty(key);
	}

}
