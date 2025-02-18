/*
 * Copyright 2024-2024 the original author or authors.
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

package org.springframework.boot.build.groovyscripts;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import org.gradle.api.Action;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@code SpringRepositorySupport.groovy}.
 *
 * @author Phillip Webb
 */
class SpringRepositoriesExtensionTests {

	private static GroovyClassLoader groovyClassLoader;

	private static Class<?> supportClass;

	@BeforeAll
	static void loadGroovyClass() throws Exception {
		groovyClassLoader = new GroovyClassLoader(SpringRepositoriesExtensionTests.class.getClassLoader());
		supportClass = groovyClassLoader.parseClass(new File("SpringRepositorySupport.groovy"));
	}

	@AfterAll
	static void cleanup() throws Exception {
		groovyClassLoader.close();
	}

	private final List<MavenArtifactRepository> repositories = new ArrayList<>();

	private final List<RepositoryContentDescriptor> contents = new ArrayList<>();

	private final List<PasswordCredentials> credentials = new ArrayList<>();

	private final List<MavenRepositoryContentDescriptor> mavenContent = new ArrayList<>();

	@Test
	void mavenRepositoriesWhenNotCommercialSnapshot() {
		SpringRepositoriesExtension extension = createExtension("0.0.0-SNAPSHOT", "oss");
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(2);
		verify(this.repositories.get(0)).setName("spring-oss-milestone");
		verify(this.repositories.get(0)).setUrl("https://repo.spring.io/milestone");
		verify(this.mavenContent.get(0)).releasesOnly();
		verify(this.repositories.get(1)).setName("spring-oss-snapshot");
		verify(this.repositories.get(1)).setUrl("https://repo.spring.io/snapshot");
		verify(this.mavenContent.get(1)).snapshotsOnly();
	}

	@Test
	void mavenRepositoriesWhenCommercialSnapshot() {
		SpringRepositoriesExtension extension = createExtension("0.0.0-SNAPSHOT", "commercial");
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(4);
		verify(this.repositories.get(0)).setName("spring-commercial-release");
		verify(this.repositories.get(0))
			.setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-prod-local");
		verify(this.mavenContent.get(0)).releasesOnly();
		verify(this.repositories.get(1)).setName("spring-oss-milestone");
		verify(this.repositories.get(1)).setUrl("https://repo.spring.io/milestone");
		verify(this.mavenContent.get(1)).releasesOnly();
		verify(this.repositories.get(2)).setName("spring-commercial-snapshot");
		verify(this.repositories.get(2)).setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-dev-local");
		verify(this.mavenContent.get(2)).snapshotsOnly();
		verify(this.repositories.get(3)).setName("spring-oss-snapshot");
		verify(this.repositories.get(3)).setUrl("https://repo.spring.io/snapshot");
		verify(this.mavenContent.get(3)).snapshotsOnly();
	}

	@Test
	void mavenRepositoriesWhenNotCommercialMilestone() {
		SpringRepositoriesExtension extension = createExtension("0.0.0-M1", "oss");
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(1);
		verify(this.repositories.get(0)).setName("spring-oss-milestone");
		verify(this.repositories.get(0)).setUrl("https://repo.spring.io/milestone");
		verify(this.mavenContent.get(0)).releasesOnly();
	}

	@Test
	void mavenRepositoriesWhenCommercialMilestone() {
		SpringRepositoriesExtension extension = createExtension("0.0.0-M1", "commercial");
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(2);
		verify(this.repositories.get(0)).setName("spring-commercial-release");
		verify(this.repositories.get(0))
			.setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-prod-local");
		verify(this.mavenContent.get(0)).releasesOnly();
		verify(this.repositories.get(1)).setName("spring-oss-milestone");
		verify(this.repositories.get(1)).setUrl("https://repo.spring.io/milestone");
		verify(this.mavenContent.get(1)).releasesOnly();
	}

	@Test
	void mavenRepositoriesWhenNotCommercialRelease() {
		SpringRepositoriesExtension extension = createExtension("0.0.1", "oss");
		extension.mavenRepositories();
		assertThat(this.repositories).isEmpty();
	}

	@Test
	void mavenRepositoriesWhenCommercialRelease() {
		SpringRepositoriesExtension extension = createExtension("0.0.1", "commercial");
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(1);
		verify(this.repositories.get(0)).setName("spring-commercial-release");
		verify(this.repositories.get(0))
			.setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-prod-local");
		verify(this.mavenContent.get(0)).releasesOnly();
	}

	@Test
	void mavenRepositoriesWhenConditionMatches() {
		SpringRepositoriesExtension extension = createExtension("0.0.0-SNAPSHOT", "oss");
		extension.mavenRepositories(true);
		assertThat(this.repositories).hasSize(2);
	}

	@Test
	void mavenRepositoriesWhenConditionDoesNotMatch() {
		SpringRepositoriesExtension extension = createExtension("0.0.0-SNAPSHOT", "oss");
		extension.mavenRepositories(false);
		assertThat(this.repositories).isEmpty();
	}

	@Test
	void mavenRepositoriesExcludingBootGroup() {
		SpringRepositoriesExtension extension = createExtension("0.0.0-SNAPSHOT", "oss");
		extension.mavenRepositoriesExcludingBootGroup();
		assertThat(this.contents).hasSize(2);
		verify(this.contents.get(0)).excludeGroup("org.springframework.boot");
		verify(this.contents.get(1)).excludeGroup("org.springframework.boot");
	}

	@Test
	void mavenRepositoriesWithRepositorySpecificEnvironmentVariables() {
		Map<String, String> environment = new HashMap<>();
		environment.put("COMMERCIAL_RELEASE_REPO_URL", "curl");
		environment.put("COMMERCIAL_RELEASE_REPO_USERNAME", "cuser");
		environment.put("COMMERCIAL_RELEASE_REPO_PASSWORD", "cpass");
		environment.put("COMMERCIAL_SNAPSHOT_REPO_URL", "surl");
		environment.put("COMMERCIAL_SNAPSHOT_REPO_USERNAME", "suser");
		environment.put("COMMERCIAL_SNAPSHOT_REPO_PASSWORD", "spass");
		SpringRepositoriesExtension extension = createExtension("0.0.0-SNAPSHOT", "commercial", environment::get);
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(4);
		verify(this.repositories.get(0)).setUrl("curl");
		verify(this.repositories.get(2)).setUrl("surl");
		assertThat(this.credentials).hasSize(2);
		verify(this.credentials.get(0)).setUsername("cuser");
		verify(this.credentials.get(0)).setPassword("cpass");
		verify(this.credentials.get(1)).setUsername("suser");
		verify(this.credentials.get(1)).setPassword("spass");
	}

	@Test
	void mavenRepositoriesWhenRepositoryEnvironmentVariables() {
		Map<String, String> environment = new HashMap<>();
		environment.put("COMMERCIAL_REPO_URL", "url");
		environment.put("COMMERCIAL_REPO_USERNAME", "user");
		environment.put("COMMERCIAL_REPO_PASSWORD", "pass");
		SpringRepositoriesExtension extension = createExtension("0.0.0-SNAPSHOT", "commercial", environment::get);
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(4);
		verify(this.repositories.get(0)).setUrl("url");
		verify(this.repositories.get(2)).setUrl("url");
		assertThat(this.credentials).hasSize(2);
		verify(this.credentials.get(0)).setUsername("user");
		verify(this.credentials.get(0)).setPassword("pass");
		verify(this.credentials.get(1)).setUsername("user");
		verify(this.credentials.get(1)).setPassword("pass");
	}

	private SpringRepositoriesExtension createExtension(String version, String buildType) {
		return createExtension(version, buildType, (name) -> null);
	}

	@SuppressWarnings({ "unchecked", "unchecked" })
	private SpringRepositoriesExtension createExtension(String version, String buildType,
			UnaryOperator<String> environment) {
		RepositoryHandler repositoryHandler = mock(RepositoryHandler.class);
		given(repositoryHandler.maven(any(Closure.class))).willAnswer(this::mavenClosure);
		return SpringRepositoriesExtension.get(repositoryHandler, version, buildType, environment);
	}

	@SuppressWarnings({ "unchecked", "unchecked" })
	private Object mavenClosure(InvocationOnMock invocation) {
		MavenArtifactRepository repository = mock(MavenArtifactRepository.class);
		willAnswer(this::contentAction).given(repository).content(any(Action.class));
		willAnswer(this::credentialsAction).given(repository).credentials(any(Action.class));
		willAnswer(this::mavenContentAction).given(repository).mavenContent(any(Action.class));
		Closure<MavenArtifactRepository> closure = invocation.getArgument(0);
		closure.call(repository);
		this.repositories.add(repository);
		return null;
	}

	private Object contentAction(InvocationOnMock invocation) {
		RepositoryContentDescriptor content = mock(RepositoryContentDescriptor.class);
		Action<RepositoryContentDescriptor> action = invocation.getArgument(0);
		action.execute(content);
		this.contents.add(content);
		return null;
	}

	private Object credentialsAction(InvocationOnMock invocation) {
		PasswordCredentials credentials = mock(PasswordCredentials.class);
		Action<PasswordCredentials> action = invocation.getArgument(0);
		action.execute(credentials);
		this.credentials.add(credentials);
		return null;
	}

	private Object mavenContentAction(InvocationOnMock invocation) {
		MavenRepositoryContentDescriptor mavenContent = mock(MavenRepositoryContentDescriptor.class);
		Action<MavenRepositoryContentDescriptor> action = invocation.getArgument(0);
		action.execute(mavenContent);
		this.mavenContent.add(mavenContent);
		return null;
	}

	interface SpringRepositoriesExtension {

		void mavenRepositories();

		void mavenRepositories(boolean condition);

		void mavenRepositoriesExcludingBootGroup();

		static SpringRepositoriesExtension get(RepositoryHandler repositoryHandler, String version, String buildType,
				UnaryOperator<String> environment) {
			try {
				Class<?> extensionClass = supportClass.getClassLoader().loadClass("SpringRepositoriesExtension");
				Object extension = extensionClass
					.getDeclaredConstructor(Object.class, Object.class, Object.class, Object.class)
					.newInstance(repositoryHandler, version, buildType, environment);
				return (SpringRepositoriesExtension) Proxy.newProxyInstance(
						SpringRepositoriesExtensionTests.class.getClassLoader(),
						new Class<?>[] { SpringRepositoriesExtension.class }, (instance, method, args) -> {
							Class<?>[] params = new Class<?>[(args != null) ? args.length : 0];
							Arrays.fill(params, Object.class);
							Method groovyMethod = extension.getClass().getDeclaredMethod(method.getName(), params);
							return groovyMethod.invoke(extension, args);
						});
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

	}

}
