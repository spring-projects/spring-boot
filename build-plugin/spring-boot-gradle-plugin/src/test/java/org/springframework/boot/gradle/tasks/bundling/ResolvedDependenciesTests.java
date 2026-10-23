/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ResolvedDependencies}.
 *
 * @author Greg Taube
 */
class ResolvedDependenciesTests {

	private final Project project = ProjectBuilder.builder().build();

	@Test
	void constructingDependenciesDoesNotAccessOtherProjects() {
		Project owner = mock(Project.class);
		given(owner.getObjects()).willReturn(this.project.getObjects());
		ResolvedDependencies dependencies = new ResolvedDependencies(owner);
		assertThat(dependencies.getArtifacts().get()).isEmpty();
		then(owner).should(never()).getRootProject();
	}

	@Test
	void graphCoordinatesDistinguishTransitiveComponentsWithTheSameProjectPath() {
		ResolvedComponentResult root = component(mock(ComponentIdentifier.class));
		ResolvedComponentResult main = projectComponent("actual", "library", "1.0");
		ResolvedComponentResult included = projectComponent("other", "library", "2.0");
		ResolvedComponentResult bridge = projectComponent("included", "bridge", "3.0");
		willReturn(Set.of(dependency(bridge), mock(UnresolvedDependencyResult.class))).given(root).getDependencies();
		willReturn(Set.of(dependency(main), dependency(included))).given(bridge).getDependencies();
		willReturn(Set.of(dependency(bridge))).given(included).getDependencies();
		ResolvedArtifactResult mainJar = artifact("main.jar", main.getId());
		ResolvedArtifactResult includedJar = artifact("included.jar", included.getId());
		ResolvedDependencies dependencies = new ResolvedDependencies(this.project);
		dependencies.resolvedArtifacts(this.project.provider(() -> Set.of(mainJar, includedJar)),
				this.project.provider(() -> root));
		assertCoordinates(dependencies, mainJar, "actual", "library", "1.0", true);
		assertCoordinates(dependencies, includedJar, "other", "library", "2.0", true);
		then(root).should(never()).getModuleVersion();
	}

	@Test
	void graphIsReadLazilyAndCoordinatesAreSnapshottedOnce() {
		AtomicInteger queries = new AtomicInteger();
		ResolvedComponentResult root = projectComponent("example", "library", "1.0");
		ResolvedArtifactResult jar = artifact("library.jar", root.getId());
		ResolvedDependencies dependencies = new ResolvedDependencies(this.project);
		dependencies.resolvedArtifacts(this.project.provider(() -> Set.of(jar)), this.project.provider(() -> {
			queries.incrementAndGet();
			return root;
		}));
		assertThat(queries).hasValue(0);
		assertCoordinates(dependencies, jar, "example", "library", "1.0", true);
		assertCoordinates(dependencies, jar, "example", "library", "1.0", true);
		assertThat(queries).hasValue(1);
	}

	@Test
	void mismatchedGraphFailsRatherThanSilentlyDroppingProjectCoordinates() {
		ResolvedComponentResult root = component(mock(ComponentIdentifier.class));
		ResolvedArtifactResult jar = artifact("missing.jar", mock(ProjectComponentIdentifier.class));
		ResolvedDependencies dependencies = new ResolvedDependencies(this.project);
		dependencies.resolvedArtifacts(this.project.provider(() -> Set.of(jar)), this.project.provider(() -> root));
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> dependencies.getArtifacts().get())
			.withStackTraceContaining("No project coordinates found for")
			.withStackTraceContaining("must come from the same configuration");
	}

	@Test
	void projectWithMissingModuleVersionIsNotAssignedGuessedCoordinates() {
		ResolvedComponentResult root = component(mock(ProjectComponentIdentifier.class));
		ResolvedArtifactResult jar = artifact("missing.jar", root.getId());
		ResolvedDependencies dependencies = new ResolvedDependencies(this.project);
		dependencies.resolvedArtifacts(this.project.provider(() -> Set.of(jar)), this.project.provider(() -> root));
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> dependencies.getArtifacts().get())
			.withStackTraceContaining("No project coordinates found for");
	}

	@Test
	void moduleCoordinatesComeFromThePublicComponentIdentifier() {
		ModuleComponentIdentifier module = mock(ModuleComponentIdentifier.class);
		given(module.getGroup()).willReturn("example");
		given(module.getModule()).willReturn("module");
		given(module.getVersion()).willReturn("1.0");
		ResolvedArtifactResult jar = artifact("renamed.jar", module);
		ResolvedComponentResult root = component(mock(ComponentIdentifier.class));
		ResolvedDependencies dependencies = new ResolvedDependencies(this.project);
		dependencies.resolvedArtifacts(this.project.provider(() -> Set.of(jar)), this.project.provider(() -> root));
		assertCoordinates(dependencies, jar, "example", "module", "1.0", false);
	}

	@Test
	void unknownArtifactsAndFilesRemainUnmapped() {
		ResolvedArtifactResult jar = artifact("unknown.jar", mock(ComponentIdentifier.class));
		ResolvedComponentResult root = component(mock(ComponentIdentifier.class));
		ResolvedDependencies dependencies = new ResolvedDependencies(this.project);
		dependencies.resolvedArtifacts(this.project.provider(() -> Set.of(jar)), this.project.provider(() -> root));
		assertThat(dependencies.find(jar.getFile())).isNull();
		assertThat(dependencies.find(new File("not-registered.jar"))).isNull();
	}

	@Test
	void artifactOnlyRegistrationRetainsProjectCoordinateLookup() {
		Project library = ProjectBuilder.builder().withName("library").withParent(this.project).build();
		library.setGroup("legacy");
		library.setVersion("1.0");
		ProjectComponentIdentifier id = mock(ProjectComponentIdentifier.class);
		given(id.getProjectPath()).willReturn(library.getPath());
		ResolvedArtifactResult jar = artifact("renamed.jar", id);
		ResolvedDependencies dependencies = new ResolvedDependencies(this.project);
		dependencies.resolvedArtifacts(this.project.provider(() -> Set.of(jar)), this.project);
		library.setVersion("2.0");
		assertCoordinates(dependencies, jar, "legacy", "library", "1.0", true);
	}

	@Test
	void graphOverloadDefaultsToExistingMethodForOtherBootArchiveImplementations() {
		BootArchive archive = mock(BootArchive.class, CALLS_REAL_METHODS);
		var artifacts = this.project.provider(() -> Collections.<ResolvedArtifactResult>emptySet());
		var root = this.project.provider(() -> component(mock(ComponentIdentifier.class)));
		archive.resolvedArtifacts(artifacts, root);
		then(archive).should().resolvedArtifacts(artifacts);
	}

	@Test
	void registrationsAreAdditiveAndFirstRegistrationForAFileWins() {
		ResolvedComponentResult first = projectComponent("first", "library", "1.0");
		ResolvedComponentResult second = projectComponent("second", "library", "2.0");
		ResolvedArtifactResult firstJar = artifact("duplicate.jar", first.getId());
		ResolvedArtifactResult secondJar = artifact("duplicate.jar", second.getId());
		ResolvedArtifactResult otherJar = artifact("other.jar", second.getId());
		ResolvedDependencies dependencies = new ResolvedDependencies(this.project);
		dependencies.resolvedArtifacts(this.project.provider(() -> Set.of(firstJar)),
				this.project.provider(() -> first));
		dependencies.resolvedArtifacts(this.project.provider(() -> Set.of(secondJar, otherJar)),
				this.project.provider(() -> second));
		assertCoordinates(dependencies, firstJar, "first", "library", "1.0", true);
		assertCoordinates(dependencies, otherJar, "second", "library", "2.0", true);
		assertThat(dependencies.getArtifacts().get()).hasSize(3);
	}

	private void assertCoordinates(ResolvedDependencies dependencies, ResolvedArtifactResult artifact, String group,
			String name, String version, boolean projectDependency) {
		ResolvedDependencies.DependencyDescriptor descriptor = dependencies.find(artifact.getFile());
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getCoordinates().getGroupId()).isEqualTo(group);
		assertThat(descriptor.getCoordinates().getArtifactId()).isEqualTo(name);
		assertThat(descriptor.getCoordinates().getVersion()).isEqualTo(version);
		assertThat(descriptor.isProjectDependency()).isEqualTo(projectDependency);
	}

	private ResolvedComponentResult projectComponent(String group, String name, String version) {
		ProjectComponentIdentifier id = mock(ProjectComponentIdentifier.class);
		given(id.getProjectPath()).willReturn(":" + name);
		ResolvedComponentResult component = component(id);
		ModuleVersionIdentifier moduleVersion = mock(ModuleVersionIdentifier.class);
		given(moduleVersion.getGroup()).willReturn(group);
		given(moduleVersion.getName()).willReturn(name);
		given(moduleVersion.getVersion()).willReturn(version);
		given(component.getModuleVersion()).willReturn(moduleVersion);
		return component;
	}

	private ResolvedComponentResult component(ComponentIdentifier id) {
		ResolvedComponentResult component = mock(ResolvedComponentResult.class);
		given(component.getId()).willReturn(id);
		given(component.getDependencies()).willReturn(Collections.emptySet());
		return component;
	}

	private ResolvedDependencyResult dependency(ResolvedComponentResult component) {
		ResolvedDependencyResult dependency = mock(ResolvedDependencyResult.class);
		given(dependency.getSelected()).willReturn(component);
		return dependency;
	}

	private ResolvedArtifactResult artifact(String name, ComponentIdentifier component) {
		ComponentArtifactIdentifier id = mock(ComponentArtifactIdentifier.class);
		given(id.getComponentIdentifier()).willReturn(component);
		ResolvedArtifactResult artifact = mock(ResolvedArtifactResult.class);
		given(artifact.getId()).willReturn(id);
		given(artifact.getFile()).willReturn(new File(name));
		return artifact;
	}

}
